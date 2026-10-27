#!/usr/bin/env python3
"""Make a private endpoint-only copy; preserve the bundled original MIDlet."""
import argparse
import ipaddress
import struct
import zipfile
from pathlib import Path


def patch_class(data, host):
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    result = bytearray(data[:10])
    position, index, changes = 10, 1, 0
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4}
    while index < count:
        tag = data[position]
        result.append(tag)
        position += 1
        if tag == 1:
            size = struct.unpack_from('>H', data, position)[0]
            position += 2
            value = data[position:position + size]
            position += size
            if value in (b'127.0.0.1', b'112.78.1.25'):
                value = host.encode('ascii')
                changes += 1
            # Disable remote server-list fetching; it could replace private endpoints.
            elif b'/srvips/' in value and value.startswith(b'http://'):
                value = b'http://127.0.0.1:9/disabled'
                changes += 1
            result.extend(struct.pack('>H', len(value)))
            result.extend(value)
        else:
            size = sizes[tag]
            result.extend(data[position:position + size])
            position += size
            if tag in (5, 6):
                index += 1
        index += 1
    assert changes == 6, changes
    result.extend(data[position:])
    return bytes(result)


def prepare(source, target, host):
    address = ipaddress.ip_address(host)
    assert address.version == 4 and address in ipaddress.ip_network('100.64.0.0/10'), 'Use Tailscale IPv4'
    assert source.resolve() != target.resolve(), 'Never overwrite original'
    target.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(target, 'x') as output:
        assert original.testzip() is None
        for entry in original.infolist():
            assert not entry.filename.upper().endswith(('.SF', '.RSA', '.DSA')), 'Signed artifact requires separate review'
            payload = original.read(entry)
            if entry.filename == 'main/GameMidlet.class':
                payload = patch_class(payload, host)
            output.writestr(entry, payload)
    with zipfile.ZipFile(target) as check:
        assert check.testzip() is None
        assert b'main.GameMidlet' in check.read('META-INF/MANIFEST.MF')
    print(target)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('host')
    parser.add_argument('--output', type=Path, default=Path('artifacts/Avatar258-private.jar'))
    args = parser.parse_args()
    prepare(Path('resAndProjectUnityFileGameJar/Avatar258.jar'), args.output, args.host)
