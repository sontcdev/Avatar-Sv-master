#!/usr/bin/env python3
"""Transport checks against a local echo upstream, without production data."""
import asyncio
import importlib.util
from pathlib import Path
from websockets.asyncio.client import connect
from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed

spec = importlib.util.spec_from_file_location('bridge', Path(__file__).with_name('avatar-ws-bridge.py'))
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)

async def echo(reader, writer):
    try:
        while data := await reader.read(65536):
            writer.write(data)
            await writer.drain()
    finally:
        writer.close()
        await writer.wait_closed()

async def main():
    b.UPSTREAM = '127.0.0.1'
    async with await asyncio.start_server(echo, '127.0.0.1', 19128), serve(b.bridge, '127.0.0.1', 0, process_request=b.request) as server:
        url = f'ws://127.0.0.1:{server.sockets[0].getsockname()[1]}/game'
        async with connect(url) as one, connect(url) as two:
            payload = bytes(range(256)) * 200
            await one.send(payload[:31])
            await one.send(payload[31:])
            received = bytearray()
            while len(received) < len(payload):
                received.extend(await asyncio.wait_for(one.recv(), 3))
            assert received == payload
            async with connect(url) as third:
                try:
                    await third.recv()
                    assert False, 'Third peer connection accepted'
                except ConnectionClosed as e:
                    assert e.rcvd.code == 1013
            await two.send('text is forbidden')
            try:
                await two.recv()
                assert False, 'Text accepted'
            except ConnectionClosed as e:
                assert e.rcvd.code == 1003
        for _ in range(100):
            if b.active == 0:
                break
            await asyncio.sleep(.01)
        assert b.active == 0 and b.peers == {}
    print('PASS: byte ordering, binary-only, per-IP limit, connection cleanup')

if __name__ == '__main__':
    asyncio.run(main())
