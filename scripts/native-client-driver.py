#!/usr/bin/env python3
"""Fail-closed native staging driver; requires local OCR helper and server state observer.

No passwords, login retries or force-stop recovery. Unknown UI requires calibration.
"""
import argparse
import hashlib
import json
import pathlib
import subprocess
import time
import unicodedata
from PIL import Image

ACCOUNTS = {5556: 71, 5558: 92, 5560: 93, 5562: 94, 5564: 95,
            5566: 96, 5568: 97, 5570: 98, 5572: 99, 5574: 100}


def classify(rows, authenticated):
    text = ' '.join(r['text'] for r in rows)
    normalized = ''.join(c for c in unicodedata.normalize('NFD', text.lower())
                         if unicodedata.category(c) != 'Mn')
    for state, terms in [('ERROR', ('not responding', 'isn’t responding', 'mat ket noi', 'disconnected', 'loi ket noi')),
                         ('LAUNCHER', ('j2me loader',)), ('CITY', ('bao binh', 'nhom', 'thanh pho', 'chon t. pho', 'xu so than tien')),
                         ('LOGIN', ('tiep tuc', 'dang nhap', 'mat khau', 'ngoi nha hanh phuc', 'choi moi')),
                         ('WORLD_MAP', ('giai tri', 'mua sam', 'nong trai', 'sanbay', 'sinh thai')),
                         ('FRIEND_LIST', ('danh sach ban', 'danh sach',)),
                         ('CHAT', ('chat voi', 'tin nhan',)),
                         ('FARM_LOADING', ('dang tai', 'loading',))]:
        if any(term in normalized for term in terms):
            return state
    # shortcut: pixel-font OCR alone cannot prove a park or rendered farm; stop until a
    # calibrated screenshot and independent server state agree, rather than guessing.
    return 'UNKNOWN_AUTHENTICATED' if authenticated else 'UNKNOWN'


class Driver:
    def __init__(self, root, port):
        self.root, self.port = pathlib.Path(root), port
        self.serial, self.user = 'emulator-' + str(port), ACCOUNTS[port]
        self.log = self.root / ('driver-' + str(port) + '.jsonl')

    def record(self, event, **fields):
        with self.log.open('a') as out:
            out.write(json.dumps(dict(epoch=time.time(), serial=self.serial, user=self.user,
                                      event=event, **fields)) + '\n')

    def adb(self, *args):
        started = time.time()
        p = subprocess.run(['/opt/homebrew/bin/adb', '-s', self.serial, *args],
                           capture_output=True, timeout=15)
        self.record('adb', args=list(args), returncode=p.returncode, elapsed=time.time()-started)
        if p.returncode:
            raise RuntimeError('ADB command failed')
        return p.stdout

    def server(self):
        states = []
        for line in (self.root / 'latest-state.txt').read_text().splitlines():
            fields = line.split(',')
            if len(fields) == 6 and int(fields[1]) == self.user:
                states.append(list(map(int, fields)))
        return states[-1] if states and time.time()*1000-states[-1][0] < 10000 else None

    def observe(self):
        if self.adb('get-state').strip() != b'device':
            raise RuntimeError('Wrong/offline emulator')
        state = self.server()
        image = self.adb('exec-out', 'screencap', '-p')
        path = self.root / (str(self.port) + '-current.png')
        path.write_bytes(image)
        rows = json.loads(subprocess.check_output([str(self.root / 'ocr'), str(path)], timeout=15))
        ui = classify(rows, state is not None)
        if ui == 'UNKNOWN_AUTHENTICATED' and state[3] == 9:
            with Image.open(path) as source:
                pixels = source.convert('RGB')
                # Calibrated only for the 800x1280 map9 view, with no modal menu.
                markers = [(400, 320, (93, 190, 247)), (400, 600, (60, 84, 77)),
                           (380, 820, (198, 195, 198))]
                if pixels.size == (800, 1280) and all(
                        max(abs(a-b) for a, b in zip(pixels.getpixel((x, y)), color)) < 5
                        for x, y, color in markers):
                    ui = 'PARK'
        result = dict(ui=ui, server=state,
                      image_sha256=hashlib.sha256(image).hexdigest(), ocr=rows)
        self.record('observe', **result)
        return result

    def move(self, key):
        before = self.observe()
        if before['ui'] != 'PARK' or before['server'] is None:
            self.record('rejected', expected='PARK', actual=before['ui'])
            raise RuntimeError('Park not verified')
        self.adb('shell', 'input', 'keyevent', str(key))
        deadline = time.monotonic() + 15
        while time.monotonic() < deadline:
            after = self.observe()
            if after['ui'] == 'PARK' and after['server'][2] == before['server'][2] and (
                    after['server'][4:6] != before['server'][4:6]):
                self.record('completed', action='movement', before=before['server'], after=after['server'])
                return after
            if after['ui'] == 'ERROR' or after['server'] is None:
                break
            time.sleep(1)
        self.record('failure', reason='movement_not_verified')
        raise RuntimeError('Movement not verified; stop')

    def transition(self, expected, input_args, target, require_auth=True):
        before = self.observe()
        if before['ui'] != expected or (require_auth and before['server'] is None):
            self.record('rejected', expected=expected, actual=before['ui'])
            raise RuntimeError('Expected UI/authentication not observed')
        self.adb('shell', 'input', *map(str, input_args))
        deadline = time.monotonic() + 25
        while time.monotonic() < deadline:
            after = self.observe()
            if after['ui'] == target:
                self.record('transition_completed', target=target)
                return after
            if after['ui'] == 'ERROR':
                break
            time.sleep(1)
        self.record('failure', reason='transition_timeout', expected=target)
        raise RuntimeError('Transition not observed')



def self_check():
    assert classify([{'text': 'System UI is not responding'}], False) == 'ERROR'
    assert classify([{'text': 'Bảo Bình'}], False) == 'CITY'
    assert classify([{'text': 'Tiếp tục'}], False) == 'LOGIN'
    assert classify([{'text': 'Danh sách bạn'}], True) == 'FRIEND_LIST'
    assert classify([], True) == 'UNKNOWN_AUTHENTICATED'
    assert len(set(ACCOUNTS.values())) == 10
    import tempfile
    from unittest.mock import Mock, patch
    with tempfile.TemporaryDirectory() as directory:
        driver = Driver(directory, 5556)
        driver.observe = Mock(return_value={'ui': 'PARK', 'server': None})
        driver.adb = Mock(side_effect=AssertionError('Unauthenticated input executed'))
        try:
            driver.move(22)
            raise AssertionError('Missing authentication accepted')
        except RuntimeError:
            pass
        driver.adb.assert_not_called()
        driver.observe = Mock(return_value={'ui': 'PARK', 'server': [1, 71, 2, 9, 258, 156]})
        driver.adb = Mock()
        with patch('time.monotonic', side_effect=[0, 16]):
            try:
                driver.move(22)
                raise AssertionError('Input submission counted as completion')
            except RuntimeError:
                pass
        events = [json.loads(line)['event'] for line in driver.log.read_text().splitlines()]
        assert events == ['rejected', 'failure']


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--self-check', action='store_true')
    p.add_argument('--root', default='/tmp/avatar2d-m26')
    p.add_argument('--port', type=int, choices=ACCOUNTS)
    args = p.parse_args()
    self_check()
    if not args.self_check:
        if args.port is None:
            p.error('--port required')
        print(json.dumps(Driver(args.root, args.port).observe()))
