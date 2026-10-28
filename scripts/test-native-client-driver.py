#!/usr/bin/env python3
import importlib.util
import pathlib
import tempfile
import time
from PIL import Image, ImageDraw, ImageChops
spec = importlib.util.spec_from_file_location('driver', pathlib.Path(__file__).with_name('native-client-driver.py'))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
with tempfile.TemporaryDirectory() as root:
    d = m.Driver(root, 5556)
    cache = pathlib.Path(root) / 'latest-state.txt'
    now = int(time.time()*1000)
    cache.write_text(f'{now},71,2,9,348,156,1,ParkMsgHandler,0\n')
    assert d.server() == [now,71,2,9,348,156,1,'ParkMsgHandler',0], 'nine-column authenticated state missing'
    cache.write_text(f'{now},71,2,9,348,156,1,ParkMsgHandler,0,71\n')
    assert d.server()[7:] == ['ParkMsgHandler',0], 'optional owner breaks state'
    cache.write_text(f'{now},71,2,9,348,156\n')
    assert len(d.server()) == 6, 'legacy cache broken'
    cache.write_text(f'{now-20000},71,2,9,348,156\n')
    assert d.server() is None, 'stale state authenticates'
    cache.write_text(f'{now+20000},71,2,9,348,156\n')
    assert d.server() is None, 'future state must not authenticate'
im = Image.new('RGB',(800,1280),'black'); draw = ImageDraw.Draw(im)
for y,c in [(200,(93,190,247)),(400,(60,84,77)),(600,(198,195,198))]:
    draw.rectangle((100,y,300,y+50), fill=c)
assert m.park_scene(im)
assert m.park_scene(ImageChops.offset(im,130,50)), 'camera movement rejected'
assert not m.park_scene(Image.new('RGB', (800,1280), (93,190,247)))
assert m.classify([{'text':'Disconnected'}],True) == 'DISCONNECTED'
print('native driver regression checks passed')

from unittest.mock import Mock, patch
with tempfile.TemporaryDirectory() as root:
    d = m.Driver(root, 5556)
    before = {'ui':'PARK','server':[1,71,2,9,348,156,1,'ParkMsgHandler',0]}
    moved = {'ui':'PARK','server':[2,71,2,9,258,156,1,'ParkMsgHandler',0]}
    d.observe = Mock(side_effect=[before,moved]); d.adb = Mock()
    assert d.move(21) == moved
    drift = {'ui':'CHAT','server':moved['server']}
    d.observe = Mock(side_effect=[before,drift]); d.adb = Mock()
    try:
        d.move(21)
        raise AssertionError('UI drift accepted')
    except RuntimeError:
        pass
    assert d.adb.call_count == 1, 'input retried after drift'
print('movement completion and stop-on-drift passed')
