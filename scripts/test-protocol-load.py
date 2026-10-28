#!/usr/bin/env python3
import importlib.util
import socket
import struct
import unittest
import tempfile
import time
import types
from pathlib import Path

spec = importlib.util.spec_from_file_location('load', Path(__file__).with_name('protocol-load.py'))
load = importlib.util.module_from_spec(spec)
spec.loader.exec_module(load)

class ProtocolTest(unittest.TestCase):
    def test_mutf8(self):
        self.assertEqual(load.utf('a\0😀'), bytes.fromhex('000961c080eda0bdedb880'))

    def test_xor_cursors_and_raw_command90(self):
        a, b = socket.socketpair()
        with a, b:
            peer = load.Wire(a, b'key')
            encoder = load.Cipher(b'key')
            packet = encoder.apply(bytes([90])) + struct.pack('>I', 4) + b'raw!' 
            packet += encoder.apply(struct.pack('>bH', -4, 4) + struct.pack('>i', 71))
            b.sendall(packet)
            self.assertEqual(peer.receive(), (90, b'raw!'))
            self.assertEqual(peer.receive(), (-4, struct.pack('>i', 71)))
            peer.send(54, b'abc')
            self.assertEqual(load.Cipher(b'key').apply(b.recv(6)), struct.pack('>bH', 54, 3) + b'abc')

    def test_eof(self):
        a,b = socket.socketpair()
        b.close()
        with a, self.assertRaises(EOFError): load.exact(a,1)

    def test_state_rejects_stale_and_wrong_user(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'state'
            path.write_text(f'{int(time.time()*1000)-20000},71,1,9,1,2,0,ParkMsgHandler,0,71\n')
            player = load.Player({'id':71},types.SimpleNamespace(state_file=str(path)))
            with self.assertRaises(TimeoutError): player.state(lambda row:True, timeout=.01)
            path.write_text(f'{int(time.time()*1000)},92,1,9,1,2,0,ParkMsgHandler,0,92\n')
            with self.assertRaises(TimeoutError): player.state(lambda row:True, timeout=.01)
            path.write_text(f'{int(time.time()*1000)},71,1,9,1,2,0,ParkMsgHandler,0,71\n')
            self.assertEqual(player.state(lambda row:row[9]=='71')[1],'71')

    def test_response_requires_post_request_match(self):
        class FakeWire:
            def send(self,cmd,body): pass
        player = load.Player({'id':71},None)
        player.wire = FakeWire()
        player.events = [(time.monotonic()-1,54,b'expected')]
        with self.assertRaises(TimeoutError):
            player.request(54,b'',lambda cmd,body:cmd==54 and body==b'expected',timeout=.01)
        self.assertEqual(player.rtts,[])

    def test_shared_stop_cancels_wait(self):
        class FakeWire:
            def send(self,cmd,body): pass
        player = load.Player({'id':71},None)
        player.wire = FakeWire()
        load.STOP.set()
        try:
            with self.assertRaises(RuntimeError): player.request(54,b'',lambda c,p:False)
        finally: load.STOP.clear()

    def test_private_chat_matches_sender_name_and_content(self):
        player = load.Player({'id':71,'username':'stage71'},None)
        expected = struct.pack('>i',71)+load.utf('stage71')+load.utf('marker')
        self.assertTrue(player.chat_matches(expected,'marker'))
        self.assertFalse(player.chat_matches(expected,'other'))

    def test_scope(self):
        for host, port, count in [('example.com',19128,1),('127.0.0.1',19130,1),('127.0.0.1',19128,11)]:
            with self.assertRaises(ValueError): load.scope(host,port,count)
        load.scope('127.0.0.1',19128,10)

if __name__ == '__main__': unittest.main()
