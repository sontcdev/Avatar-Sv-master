#!/usr/bin/env python3
"""Small local checks for protocol framing and private-client preparation."""
import importlib.util
import socket
import struct
import tempfile
import threading
import zipfile
from pathlib import Path


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    loaded = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded)
    return loaded


protocol = module('protocol', 'protocol-smoke.py')
client = module('client', 'prepare-client.py')
left, right = socket.socketpair()
with left, right:
    right.sendall(b'ab')
    right.shutdown(socket.SHUT_WR)
    assert protocol.receive_exact(left, 2) == b'ab'
    try:
        protocol.receive_exact(left, 1)
    except EOFError:
        pass
    else:
        raise AssertionError('EOF was accepted')

with socket.socket() as listener:
    listener.bind(('127.0.0.1', 0))
    listener.listen(1)
    failures = []

    def server():
        try:
            with listener.accept()[0] as peer:
                assert protocol.receive_exact(peer, 3) == struct.pack('>bH', -27, 0)
                key = b'123_kitakeyos'
                body = bytes([len(key), key[0]]) + bytes(a ^ b for a, b in zip(key[1:], key))
                peer.sendall(struct.pack('>bH', -27, len(body)) + body)
        except Exception as error:
            failures.append(error)

    thread = threading.Thread(target=server)
    thread.start()
    protocol.handshake('127.0.0.1', listener.getsockname()[1])
    thread.join(timeout=5)
    assert not thread.is_alive() and not failures, failures

original = Path('resAndProjectUnityFileGameJar/Avatar258.jar')
with tempfile.TemporaryDirectory() as directory:
    output = Path(directory) / 'client.jar'
    client.prepare(original, output, '100.112.0.62')
    with zipfile.ZipFile(original) as source, zipfile.ZipFile(output) as patched:
        assert source.namelist() == patched.namelist()
        differences = [name for name in source.namelist() if source.read(name) != patched.read(name)]
        assert differences == ['main/GameMidlet.class'], differences
        data = patched.read(differences[0])
        assert b'100.112.0.62' in data and b'/srvips/' not in data
    try:
        client.prepare(original, Path(directory) / 'public.jar', '8.8.8.8')
    except AssertionError:
        pass
    else:
        raise AssertionError('Public endpoint was accepted')
print('PASS: EOF handling, framed handshake, single-class private patch, public endpoint rejection')
