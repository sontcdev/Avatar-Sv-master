import importlib.util,sys,time
s=importlib.util.spec_from_file_location('d','/Volumes/sontc/sources/others/avatar2d-gameplay-migration/scripts/native-client-driver.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
d=m.Driver('/tmp/avatar2d-m27/held-native',int(sys.argv[1]));a=d.observe()
if a['ui']=='LAUNCHER':a=d.transition('LAUNCHER',['tap',350,390],'LOGIN',False)
if a['ui']=='LOGIN':
 assert any(str(d.user) in r['text'] for r in a['ocr']), 'Account label missing; no blind login'
 a=d.transition('LOGIN',['tap',400,610],'CITY',False)
assert a['ui']=='CITY'
if not any(('Binh' in r['text'] and 'Pho' in r['text']) for r in a['ocr']):
 row=next(r for r in a['ocr'] if 'Than Tien' in r['text']);d.adb('shell','input','tap',str(round(row['x']*800)),str(round(row['y']*1280)))
for i in range(10):
 a=d.observe();rows=[r for r in a['ocr'] if ('Binh' in r['text'] and 'Pho' in r['text'])]
 if rows:break
 time.sleep(1)
else:raise RuntimeError('City expansion did not complete')
# Wait for scrolling to settle by requiring the observed row position twice.
last=rows[0]
for i in range(10):
 time.sleep(.4);a=d.observe();row=next(r for r in a['ocr'] if ('Binh' in r['text'] and 'Pho' in r['text']))
 if abs(last['y']-row['y'])<.003:break
 last=row
print(d.transition('CITY',['tap',round(row['x']*800),round(row['y']*1280)],'WORLD_MAP',False))
