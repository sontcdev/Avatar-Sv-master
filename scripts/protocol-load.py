#!/usr/bin/env python3
"""Bounded real Avatar TCP workload; only private localhost staging tunnels.
Accounts: [{"id":71,"username":"...","password":"..."}, ...].
State CSV: epoch_ms,user,session,map,x,y,zone,context,direct[,farmOwner].
No packet/auth bodies are logged. State polling latency is never reported as RTT.
"""
import argparse
import concurrent.futures
import json
import socket
import select
import struct
import threading
import time
from pathlib import Path

ALLOWED = {71, *range(92, 101)}
PRINT_LOCK = threading.Lock()
STOP = threading.Event()


def emit(**fields):
    with PRINT_LOCK:
        print(json.dumps(dict(epoch_ms=int(time.time()*1000), **fields)), flush=True)


def scope(host, port, players):
    if host != '127.0.0.1' or port not in (19128, 19129) or not 1 <= players <= 10:
        raise ValueError('Only 127.0.0.1:19128/19129 and 1..10 players allowed')


def utf(value):
    # Java modified UTF-8 encodes NUL specially and supplementary codepoints as surrogate pairs.
    result = bytearray()
    raw = value.encode('utf-16-be')
    for offset in range(0, len(raw), 2):
        char = int.from_bytes(raw[offset:offset+2], 'big')
        if 0 < char < 128: result.append(char)
        elif char < 2048: result.extend((0xc0 | char >> 6, 0x80 | char & 63))
        else: result.extend((0xe0 | char >> 12, 0x80 | char >> 6 & 63, 0x80 | char & 63))
    if len(result) > 65535: raise ValueError('UTF field too long')
    return struct.pack('>H', len(result)) + result


def exact(sock, size):
    result = bytearray()
    while len(result) < size:
        chunk = sock.recv(size-len(result))
        if not chunk: raise EOFError('Server disconnected')
        result.extend(chunk)
    return bytes(result)


class Cipher:
    def __init__(self, key): self.key, self.cursor = key, 0
    def apply(self, data):
        output = bytearray()
        for value in data:
            output.append(value ^ self.key[self.cursor])
            self.cursor = (self.cursor + 1) % len(self.key)
        return bytes(output)


class Wire:
    def __init__(self, sock, key):
        self.sock, self.read, self.write = sock, Cipher(key), Cipher(key)
    def send(self, cmd, body=b''):
        packet = memoryview(self.write.apply(struct.pack('>bH', cmd, len(body)) + body))
        deadline = time.monotonic()+8
        while packet:
            remaining = deadline-time.monotonic()
            if remaining <= 0 or not select.select([], [self.sock], [], remaining)[1]:
                raise TimeoutError('Socket write timeout')
            try: sent = self.sock.send(packet, socket.MSG_DONTWAIT)
            except BlockingIOError: continue
            if not sent: raise EOFError('Server disconnected during write')
            packet = packet[sent:]
    def receive(self):
        cmd = struct.unpack('>b', self.read.apply(exact(self.sock, 1)))[0]
        size = struct.unpack('>I', exact(self.sock,4))[0] if cmd == 90 else struct.unpack('>H',self.read.apply(exact(self.sock,2)))[0]
        if size > 8*1024*1024: raise ValueError('Oversized packet')
        body = exact(self.sock,size)
        return cmd, body if cmd == 90 else self.read.apply(body)


class Player:
    def __init__(self, account, args):
        self.account, self.args = account, args
        self.uid = account['id']
        self.sock = None
        self.cv = threading.Condition()
        self.events = []
        self.error = None
        self.session = None
        self.deadline = None
        self.authenticated = False
        self.last_success = None
        self.finished = False
        self.operations = 0
        self.successes = 0
        self.rtts = []
    def pump(self):
        try:
            while True:
                cmd, body = self.wire.receive()
                with self.cv:
                    self.events.append((time.monotonic(), cmd, body))
                    self.events = self.events[-2048:]
                    self.cv.notify_all()
        except (OSError, EOFError, ValueError) as error:
            with self.cv:
                self.error = type(error).__name__
                self.cv.notify_all()
    def request(self, cmd, body, predicate, timeout=8):
        start = time.monotonic()
        if self.deadline is not None: timeout = min(timeout,max(0,self.deadline-start))
        self.wire.send(cmd, body)
        with self.cv:
            while True:
                if STOP.is_set(): raise RuntimeError('Workload stopped after player failure')
                if self.error: raise RuntimeError('Connection failed: '+self.error)
                for stamp, incoming, payload in self.events:
                    if stamp >= start and predicate(incoming,payload):
                        elapsed = (stamp-start)*1000
                        self.rtts.append(elapsed)
                        if cmd in (54,55,61):
                            self.operations += 1
                            self.last_success = stamp
                        emit(event='response',user=self.uid,command=cmd,rtt_ms=round(elapsed,3))
                        return elapsed
                remaining = start+timeout-time.monotonic()
                if remaining <= 0: raise TimeoutError('No correlated response for command '+str(cmd))
                self.cv.wait(min(remaining, .5))
    def state(self, predicate, timeout=8):
        deadline = time.monotonic()+timeout
        if self.deadline is not None: deadline = min(deadline,self.deadline)
        while time.monotonic() < deadline:
            if STOP.is_set(): raise RuntimeError('Workload stopped after player failure')
            if self.error: raise RuntimeError('Disconnected: '+self.error)
            try:
                for line in Path(self.args.state_file).read_text().splitlines():
                    row = line.split(',')
                    if len(row) not in (9,10): continue
                    if int(row[1]) != self.uid or not 0 <= time.time()*1000-int(row[0]) < 10000: continue
                    if self.session is not None and row[2] != self.session: continue
                    if predicate(row): return row
            except (OSError, ValueError): pass
            time.sleep(.1)
        raise TimeoutError('No fresh correlated server state')
    def context(self,index):
        self.request(-1,bytes([index]),lambda cmd,p: cmd == -1 and p == bytes([index]))
    def chat_matches(self, payload, text):
        return payload == struct.pack('>i',self.uid)+utf(self.account['username'])+utf(text)

    def private_chat(self, sequence):
        if not self.peers:
            return
        peer = self.peers[0]
        text = 'm27-private-%d-%d' % (self.uid,sequence)
        start = time.monotonic()
        self.wire.send(-6,struct.pack('>i',peer.uid)+utf(text))
        with peer.cv:
            while True:
                if STOP.is_set() or peer.error or self.error:
                    raise RuntimeError('Private chat connection lost')
                for stamp, cmd, body in peer.events:
                    if stamp >= start and cmd == -6 and self.chat_matches(body,text):
                        self.operations += 1
                        self.last_success = stamp
                        emit(event='private_chat_delivered',user=self.uid,receiver=peer.uid,
                             command=-6,delivery_ms=round((stamp-start)*1000,3))
                        return
                remaining = min(start+8,self.deadline)-time.monotonic()
                if remaining <= 0:
                    raise TimeoutError('Private chat not received by paired client')
                peer.cv.wait(min(remaining,.5))

    def cycle(self, sequence):
        self.deadline = min(time.monotonic()+30, (self.last_success or time.monotonic())+30)
        self.context(9)
        self.wire.send(50, struct.pack('>bbhh',9,0,348,156))
        self.state(lambda row: row[3]=='9' and row[6]=='0' and row[7]=='ParkMsgHandler')
        x,y = (270 if sequence%2==0 else 280),156
        movement = struct.pack('>ihhb',self.uid,x,y,0)
        # Zone broadcasts include the sender; match id AND unique coordinates.
        self.request(54,struct.pack('>hhb',x,y,0),lambda cmd,p:cmd==54 and p==movement)
        self.state(lambda row: row[3]=='9' and row[4]==str(x) and row[5]==str(y) and row[7]=='ParkMsgHandler')
        text = 'm27-%d-%d'%(self.uid,sequence)
        self.request(55,utf(text),lambda cmd,p:cmd==55 and p==struct.pack('>i',self.uid)+utf(text))
        self.private_chat(sequence)
        self.context(10)
        friend = 92 if self.uid == 71 else 71
        for owner in (self.uid,friend):
            self.request(61,struct.pack('>i',owner),lambda cmd,p:cmd==61 and len(p)>=4 and struct.unpack('>i',p[:4])[0]==owner)
            self.state(lambda row: row[7]=='FarmMsgHandler' and (len(row)==9 or row[9]==str(owner)))
        self.successes += 1
        self.last_success = time.monotonic()
        emit(event='cycle',user=self.uid,sequence=sequence,successes=self.successes)
    def run(self):
        try:
            self.sock = socket.create_connection((self.args.host,self.args.port),timeout=5)
            self.sock.settimeout(8)
            self.sock.sendall(struct.pack('>bH',-27,0))
            cmd,size = struct.unpack('>bH',exact(self.sock,3))
            if cmd != -27 or not 2<=size<=256: raise ValueError('Bad handshake')
            body = exact(self.sock,size)
            if body[0] != size-1: raise ValueError('Bad handshake key length')
            key = bytearray([body[1]])
            for value in body[2:]: key.append(value ^ key[-1])
            self.sock.settimeout(None)
            self.wire = Wire(self.sock,bytes(key))
            threading.Thread(target=self.pump,daemon=True).start()
            info = bytes([9])+struct.pack('>i',128*1024*1024)+utf('M2.7 TCP harness')+struct.pack('>iii',1024*1024,240,320)+bytes([0,0])+utf('2.5.0')
            self.wire.send(-17,info)
            login_epoch = int(time.time()*1000)
            self.request(-2,utf(self.account['username'])+utf(self.account['password'])+utf('2.5.0'),lambda cmd,p:cmd==-4 and len(p)>=4 and struct.unpack('>i',p[:4])[0]==self.uid)
            before = self.state(lambda row: int(row[0]) >= login_epoch)
            session = before[2]
            self.session = session
            if STOP.wait(5): raise RuntimeError('Workload stopped after player failure')
            self.state(lambda row:row[2]==session and int(row[0]) >= login_epoch+5000)
            self.authenticated = True
            emit(event='authenticated_session_5s',user=self.uid,session=session)
            self.barrier.wait(timeout=20)
            start = time.monotonic()
            sequence = 0
            while time.monotonic()-start < self.args.duration and not STOP.is_set():
                cycle_start = time.monotonic()
                self.cycle(sequence)
                if time.monotonic()-cycle_start>30: raise TimeoutError('Cycle exceeded 30 seconds')
                sequence += 1
                STOP.wait(max(0,self.args.interval-(time.monotonic()-cycle_start)))
            if STOP.is_set(): raise RuntimeError('Workload stopped after player failure')
            self.barrier.wait(timeout=35)
            emit(event='player_complete',user=self.uid,cycles=self.successes,completed_operations=self.operations,rtt_samples=len(self.rtts),rtt_max_ms=max(self.rtts,default=0))
            return True
        except Exception as error:
            STOP.set()
            self.barrier.abort()
            # Exception messages never include username/password or packet bodies.
            emit(event='player_failed',user=self.uid,error=type(error).__name__,reason=str(error) if isinstance(error,(TimeoutError,RuntimeError)) else 'Protocol or local IO failure')
            return False
        finally:
            self.finished = True
            if self.sock:
                try: self.sock.shutdown(socket.SHUT_RDWR)
                except OSError: pass
                self.sock.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host',default='127.0.0.1')
    parser.add_argument('--port',type=int,default=19128)
    parser.add_argument('--accounts',required=True)
    parser.add_argument('--state-file',required=True)
    parser.add_argument('--players',type=int,default=1)
    parser.add_argument('--duration',type=float,default=300)
    parser.add_argument('--interval',type=float,default=5)
    args = parser.parse_args()
    scope(args.host,args.port,args.players)
    if args.duration<=0 or args.interval<1: raise ValueError('Invalid duration or interval')
    accounts = json.loads(Path(args.accounts).read_text())
    selected = accounts[:args.players]
    ids = [a['id'] for a in selected]
    if len(selected)!=args.players or len(set(ids))!=len(ids) or not set(ids)<=ALLOWED: raise ValueError('Need distinct allowlisted accounts')
    if len({a['username'] for a in selected})!=len(selected): raise ValueError('Accounts must have distinct usernames')
    emit(event='start',players=args.players,duration=args.duration,port=args.port)
    players = [Player(account,args) for account in selected]
    barrier = threading.Barrier(len(players))
    for player in players:
        player.barrier = barrier
        player.peers = [peer for peer in players if peer is not player]
    def monitor():
        while not all(player.finished for player in players):
            rows = {}
            try:
                for line in Path(args.state_file).read_text().splitlines():
                    row = line.split(',')
                    if len(row) in (9,10) and 0 <= time.time()*1000-int(row[0]) < 10000:
                        rows[(int(row[1]),row[2])] = row
            except (OSError,ValueError): pass
            authenticated = [p for p in players if p.authenticated and not p.finished and not p.error and (p.uid,p.session) in rows]
            active = [p for p in authenticated if p.last_success is not None and time.monotonic()-p.last_success <= 30]
            emit(event='concurrency',requested=args.players,authenticated=len(authenticated),active=len(active),completed_operations=sum(p.operations for p in players))
            if STOP.wait(5): break
    sampler = threading.Thread(target=monitor,daemon=True)
    sampler.start()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.players) as pool:
        results = list(pool.map(lambda player:player.run(),players))
    emit(event='complete',players=args.players,passed=sum(results),failed=len(results)-sum(results))
    return 0 if all(results) else 1

if __name__=='__main__': raise SystemExit(main())
