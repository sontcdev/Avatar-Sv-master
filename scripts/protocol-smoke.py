#!/usr/bin/env python3
"""Validate the actual -27 XOR-key negotiation; never report this as gameplay."""
import socket
import struct
import sys


def receive_exact(sock, size):
    result = bytearray()
    while len(result) < size:
        chunk = sock.recv(size - len(result))
        if not chunk:
            raise EOFError('Connection closed during packet')
        result.extend(chunk)
    return bytes(result)


def handshake(host, port):
    with socket.create_connection((host, port), timeout=5) as sock:
        sock.settimeout(5)
        sock.sendall(struct.pack('>bH', -27, 0))
        cmd, size = struct.unpack('>bH', receive_exact(sock, 3))
        assert cmd == -27 and 2 <= size <= 256, (cmd, size)
        payload = receive_exact(sock, size)
        assert payload[0] == size - 1
        key = bytearray([payload[1]])
        for value in payload[2:]:
            key.append(value ^ key[-1])
        assert key.endswith(b'_kitakeyos'), 'Unexpected protocol key'
    print('PASS: -27 handshake and XOR key negotiation')


if __name__ == '__main__':
    handshake(sys.argv[1], int(sys.argv[2]))
