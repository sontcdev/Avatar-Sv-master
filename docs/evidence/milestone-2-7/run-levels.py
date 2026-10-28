import subprocess,pathlib,time,json,signal,sys
r=pathlib.Path('/tmp/avatar2d-m27');repo=pathlib.Path('/Volumes/sontc/sources/others/avatar2d-gameplay-migration')
out=open(r/'supervisor-events.jsonl','a',buffering=1)
def event(**d):out.write(json.dumps(dict(epoch=time.time(),**d))+'\n')
def remote(cmd):return subprocess.check_output(['ssh','homelab','cd /home/sontc/avatar2d-gameplay-stage && '+cmd],text=True,timeout=12)
def snapshot(label):
 text=remote('mariadb --socket=mysql.sock -uroot -BN avatar2d_gameplay_stage <m27-integrity.sql');(r/(label+'.tsv')).write_text(text)
 rows=[x.split('\t') for x in text.splitlines()]
 assert len(rows)==20
 return [x[:-1] for x in rows]
baseline=snapshot('protocol-before')
for n in (1,3,5,10):
 assert snapshot(f'level{n}-before')==baseline,'Gameplay changed before level'
 f=open(r/f'protocol-{n}.jsonl','w')
 p=subprocess.Popen(['python3',str(repo/'scripts/protocol-load.py'),'--accounts',str(r/'accounts-private.json'),'--state-file',str(r/'latest-state.txt'),'--players',str(n),'--duration','320'],stdout=f,stderr=f,stdin=subprocess.DEVNULL)
 event(event='level_start',players=n,pid=p.pid)
 try:
  deadline=time.monotonic()+430
  while p.poll() is None:
   if (r/'stop-load').exists():raise RuntimeError('Explicit stop marker')
   if time.monotonic()>deadline:raise TimeoutError('Bounded level deadline')
   host=json.loads(remote('tail -n 1 m27-host-metrics.jsonl'))
   assert time.time()-host['epoch']<15,'Host observer stale'
   assert dict(line.split('=',1) for line in host['service'].splitlines())=={'ActiveState':'active','NRestarts':'0'},'Production service changed'
   assert set(host['production'])=={'313665','302726'},'Production identity changed'
   mem=dict((line.split(':')[0],int(line.split()[1])) for line in host['memory'].splitlines() if line.startswith('MemAvailable:'))
   assert mem['MemAvailable:'.rstrip(':')]>512*1024,'Host low memory'
   assert float(host['loadavg'].split()[0])<4,'Host load pressure'
   time.sleep(5)
  assert p.returncode==0,f'Protocol level {n} failed'
  time.sleep(5)
  assert snapshot(f'level{n}-after')==baseline,'Gameplay integrity mismatch'
  event(event='level_pass',players=n,returncode=p.returncode)
 except BaseException as e:
  p.terminate()
  try:p.wait(10)
  except subprocess.TimeoutExpired:p.kill();p.wait()
  event(event='stopped',players=n,error=type(e).__name__,reason=str(e));sys.exit(1)
event(event='all_levels_complete')
