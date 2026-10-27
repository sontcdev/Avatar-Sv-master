#!/usr/bin/env python3
"""Validate the complete Avatar key packet over public TLS WebSocket."""
import asyncio
import struct
from websockets.asyncio.client import connect

async def main():
    async with connect('wss://avatar.memora.io.vn/game', compression=None) as ws:
        await ws.send(struct.pack('>bH', -27, 0))
        data = bytearray()
        while len(data) < 3 or len(data) < 3 + int.from_bytes(data[1:3], 'big'):
            message = await asyncio.wait_for(ws.recv(), 10)
            assert isinstance(message, bytes)
            data.extend(message)
            assert len(data) <= 65536
        cmd, size = struct.unpack('>bH', data[:3])
        assert cmd == -27 and 2 <= size <= 256
        payload = data[3:3 + size]
        assert payload[0] == size - 1
        key = bytearray([payload[1]])
        for value in payload[2:]:
            key.append(value ^ key[-1])
        assert key.endswith(b'_kitakeyos')
    print('PASS: public WSS full -27 handshake and XOR key negotiation')

if __name__ == '__main__':
    asyncio.run(main())
