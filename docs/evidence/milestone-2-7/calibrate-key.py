#!/usr/bin/env python3
"""Single held-key calibration; no automatic retry and no driver source modification."""
import argparse
import importlib.util
import json


def held_args(args):
    if args[:3] == ('shell', 'input', 'keyevent'):
        return (*args[:3], '--longpress', *args[3:])
    return args


assert held_args(('shell', 'input', 'keyevent', '22')) == ('shell', 'input', 'keyevent', '--longpress', '22')
assert held_args(('get-state',)) == ('get-state',)
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--self-check', action='store_true')
p.add_argument('--port', type=int, default=5556)
p.add_argument('--key', type=int, choices=(21,22), default=22)
p.add_argument('--root', default='/tmp/avatar2d-m27')
a = p.parse_args()
if not a.self_check:
    spec = importlib.util.spec_from_file_location('native_driver', '/Volumes/sontc/sources/others/avatar2d-gameplay-migration/scripts/native-client-driver.py')
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    d = m.Driver(a.root, a.port)
    original_adb = d.adb
    d.adb = lambda *args: original_adb(*held_args(args))
    d.record('calibration_start', key=a.key, duration='android_longpress', retry=False)
    result = d.move(a.key)
    expected = 0 if a.key == 22 else 2
    if len(result['server']) < 9 or result['server'][8] != expected:
        d.record('calibration_failed', reason='facing_not_verified', expected=expected)
        raise RuntimeError('World moved but facing not verified; stop')
    d.record('calibration_passed', key=a.key, facing=expected, server=result['server'])
    print(json.dumps(result))
