#!/usr/bin/env python3
"""Bounded WebSocket-to-Avatar bridge; no client-selected upstream destinations."""
import asyncio
import os
from pathlib import Path
from websockets.asyncio.server import serve
from websockets.http11 import Response
from websockets.datastructures import Headers
from websockets.exceptions import ConnectionClosed

UPSTREAM = os.environ.get('AVATAR_HOST', '100.112.0.62')
APK = Path(os.environ.get('AVATAR_APK', '/home/sontc/avatar2d-public/Avatar-Homelab.apk'))
MAX_CLIENTS = 6
active = 0
peers = {}


def response(code, text):
    body = text.encode()
    return Response(code, 'OK' if code == 200 else 'Unavailable',
                    Headers({'Content-Type': 'text/plain; charset=utf-8',
                             'Content-Length': str(len(body)), 'Cache-Control': 'no-store'}), body)


async def request(connection, req):
    if req.path == '/healthz':
        try:
            reader, writer = await asyncio.wait_for(asyncio.open_connection(UPSTREAM, 19128), 3)
            writer.close()
            await writer.wait_closed()
            return response(200, 'Avatar bridge and game reachable\n')
        except (OSError, TimeoutError):
            return response(503, 'Game unavailable\n')
    if req.path in ('/', '/Avatar-Homelab.apk'):
        if not APK.is_file():
            return response(503, 'Android build is being prepared\n')
        body = APK.read_bytes()
        return Response(200, 'OK', Headers({
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': 'attachment; filename="Avatar-Homelab.apk"',
            'Content-Length': str(len(body)), 'Cache-Control': 'no-store',
            'X-Content-Type-Options': 'nosniff'}), body)
    if req.path != '/game':
        return response(404, 'Not found\n')
    if req.headers.get('Upgrade', '').lower() != 'websocket':
        return response(426, 'WebSocket required\n')
    return None


async def bridge(ws):
    global active
    # Only the local cloudflared connector can reach this loopback listener.
    ip = ws.request.headers.get('CF-Connecting-IP', 'local')
    if active >= MAX_CLIENTS or peers.get(ip, 0) >= 2:
        await ws.close(1013, 'Game is full; retry later')
        return
    active += 1
    peers[ip] = peers.get(ip, 0) + 1
    writer = None
    try:
        reader, writer = await asyncio.wait_for(asyncio.open_connection(UPSTREAM, 19128), 5)

        async def upstream():
            async for message in ws:
                if not isinstance(message, bytes):
                    await ws.close(1003, 'Binary messages required')
                    return
                writer.write(message)
                await writer.drain()

        async def downstream():
            while data := await reader.read(65536):
                await ws.send(data)

        tasks = [asyncio.create_task(upstream()), asyncio.create_task(downstream())]
        try:
            await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)
    except (OSError, TimeoutError, ConnectionClosed):
        pass
    finally:
        if writer:
            writer.close()
            try:
                await writer.wait_closed()
            except OSError:
                pass
        active -= 1
        peers[ip] -= 1
        if not peers[ip]:
            del peers[ip]
        await ws.close()


async def main():
    async with serve(bridge, '127.0.0.1', 18728, process_request=request,
                     max_size=65536, max_queue=4, write_limit=65536,
                     ping_interval=20, ping_timeout=20, compression=None):
        await asyncio.Future()


if __name__ == '__main__':
    asyncio.run(main())
