from pathlib import Path
import json,re,csv,math,collections,shutil
r=Path('/tmp/avatar2d-m27');o=Path('/Volumes/sontc/sources/others/avatar2d-gameplay-migration/docs/evidence/milestone-2-7');o.mkdir(exist_ok=True)
def pct(v,p):return sorted(v)[max(0,math.ceil(len(v)*p)-1)]
def readjson(p):return [json.loads(x) for x in p.read_text().splitlines() if x.strip()]
metrics=[{k:int(v) for k,v in x.items()} for x in csv.DictReader((r/'m27-metrics.csv').open())]
host=readjson(r/'m27-host-metrics.jsonl')
trace=[x for x in (r/'m27-server-private.log').read_text(errors='replace').splitlines() if x.startswith(('STAGING_DISPATCH ','STAGING_DISCONNECT ')) or x.startswith('GAMEPLAY_PACKET ') and re.search(r' cmd=(54|55|61|50|-1) ',x)]
(o/'session-telemetry.txt').write_text('\n'.join(trace)+'\n')
summary=[];handlers=[];rtts=[];counts=[]
for n in (1,3,5,10):
 log=readjson(r/f'protocol-{n}.jsonl');shutil.copy(r/f'protocol-{n}.jsonl',o/f'protocol-{n}.jsonl')
 assert log[-1]['event']=='complete' and log[-1]['passed']==n and log[-1]['failed']==0
 full=[x for x in log if x['event']=='concurrency' and x['authenticated']==n and x['active']==n]
 first,last=full[0]['epoch_ms'],full[-1]['epoch_ms'];span=(last-first)/1000
 middle=[x for x in log if x['event']=='concurrency' and first<=x['epoch_ms']<=last]
 assert span>=300 and len(middle)==len(full),(n,span)
 assert max(b['epoch_ms']-a['epoch_ms'] for a,b in zip(full,full[1:]))<6500
 for x in log:
  if x['event']=='concurrency':counts.append(dict(level=n,**x))
 win=[x for x in metrics if first<=x['epoch_ms']<=last];a,b=win[0],win[-1]
 operations=[x for x in log if first<=x['epoch_ms']<=last and (x['event']=='private_chat_delivered' or x['event']=='response' and x['command'] in (54,55,61))]
 hwin=[x for x in host if first<=x['epoch']*1000<=last]
 lat=collections.defaultdict(list)
 for line in trace:
  if not line.startswith('STAGING_DISPATCH '):continue
  d=dict(re.findall(r'(\w+)=([^ ]+)',line))
  if first<=int(d['epoch_ms'])<=last and int(d['user']) in {71,*range(92,101)}:
   assert d['completed']=='true'
   lat[int(d['cmd'])].append(int(d['elapsed_ns'])/1e6)
 for cmd,v in sorted(lat.items()):handlers.append(dict(level=n,command=cmd,samples=len(v),p50_ms=pct(v,.5),p95_ms=pct(v,.95),p99_ms=pct(v,.99),max_ms=max(v)))
 rt=collections.defaultdict(list)
 for x in log:
  if x['event']=='response' and first<=x['epoch_ms']<=last:rt[x['command']].append(x['rtt_ms'])
 for cmd,v in sorted(rt.items()):rtts.append(dict(level=n,command=cmd,samples=len(v),p50_ms=pct(v,.5),p95_ms=pct(v,.95),p99_ms=pct(v,.99),max_ms=max(v)))
 def db(d):return dict(x.split('\t') for x in d['db'].splitlines())
 da,dbb=db(hwin[0]),db(hwin[-1])
 production={}
 for pid in ('313665','302726'):
  def stat(d):return d['production'][pid]['stat'].split(') ',1)[1].split()
  pa,pb=stat(hwin[0]),stat(hwin[-1]);production[pid]={'cpu_pct_one_core':100*(int(pb[11])+int(pb[12])-int(pa[11])-int(pa[12]))/100/(hwin[-1]['epoch']-hwin[0]['epoch']),'rss_max_mib':max(int(stat(x)[21])*4096/2**20 for x in hwin),'identity_present_all_samples':all(pid in x['production'] for x in hwin)}
 disks=[]
 for d in (hwin[0],hwin[-1]):
  rows=[x.split() for x in d['diskstats'].splitlines()];disks.append({x[2]:{'read_bytes':int(x[5])*512,'write_bytes':int(x[9])*512,'io_ms':int(x[12])} for x in rows if x[2]=='sda'})
 disk={k:disks[1].get('sda',{}).get(k,0)-disks[0].get('sda',{}).get(k,0) for k in ('read_bytes','write_bytes','io_ms')}
 summary.append(dict(level=n,first_active_sample=first,last_active_sample=last,active_sample_span_s=span,full_active_samples=len(full),successful_operations=len(operations),operations_per_s=len(operations)/span,failed_operations=0,unexpected_disconnects=0,session_generations=[{'user':x['user'],'session':x['session']} for x in log if x['event']=='authenticated_session_5s'],cpu_one_core_pct=100*(b['cpu_ns']-a['cpu_ns'])/(b['epoch_ms']-a['epoch_ms'])/1e6,heap_max_mib=max(x['heap_used'] for x in win)/2**20,rss_max_mib=max(x['rss_kb'] for x in win)/1024,gc_count_delta=b['gc_count']-a['gc_count'],gc_ms_delta=b['gc_ms']-a['gc_ms'],java_threads_max=max(x['java_threads'] for x in win),os_threads_max=max(x['os_threads'] for x in win),pool_active_max=max(x['pool_active'] for x in win),pool_idle_max=max(x['pool_idle'] for x in win),pool_waiting_max=max(x['pool_waiting'] for x in win),jvm_read_bytes_delta=b['read_bytes']-a['read_bytes'],jvm_write_bytes_delta=b['write_bytes']-a['write_bytes'],db_lock_wait_delta=int(dbb['Innodb_row_lock_waits'])-int(da['Innodb_row_lock_waits']),db_lock_time_ms_delta=int(dbb['Innodb_row_lock_time'])-int(da['Innodb_row_lock_time']),db_current_waits_max=max(int(db(x)['Innodb_row_lock_current_waits']) for x in hwin),slow_queries_delta=int(dbb['Slow_queries'])-int(da['Slow_queries']),host_load1_max=max(float(x['loadavg'].split()[0]) for x in hwin),host_available_mem_min_mib=min(int(next(z for z in x['memory'].splitlines() if z.startswith('MemAvailable:')).split()[1])/1024 for x in hwin),host_disk_delta=disk,production=production))
for name,rows in [('handler-latency.csv',handlers),('client-rtt.csv',rtts),('authenticated-active-counts.csv',counts)]:
 keys=list(rows[0]);
 with (o/name).open('w') as f:
  w=csv.DictWriter(f,fieldnames=keys);w.writeheader();w.writerows(rows)
(o/'protocol-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
for p in r.glob('level*-*.tsv'):shutil.copy(p,o/p.name)
for src,dst in [('m27-metrics.csv','jvm-metrics.csv'),('m27-state.csv','server-state.csv'),('m27-host-metrics.jsonl','server-host-metrics.jsonl'),('supervisor-events.jsonl','supervisor-events.jsonl'),('final-test-manifest.tsv','test-manifest.tsv'),('native-result.json','native-result.json'),('M27Metrics.java','M27Metrics.java'),('observe.py','server-observer.py'),('run-levels.py','run-levels.py'),('functional.jsonl','functional1.jsonl'),('functional3.jsonl','functional3.jsonl'),('protocol-1-guard-aborted.jsonl','protocol-1-guard-aborted.jsonl')]:shutil.copy(r/src,o/dst)
for p in r.glob('driver-*.jsonl'):shutil.copy(p,o/p.name)
for p in r.glob('*-gate-failed.png'):shutil.copy(p,o/p.name)
print(json.dumps(summary,indent=2))

queries=readjson(r/'m27-db-queries.jsonl')
for row in summary:
 q=[x for x in queries if row['first_active_sample']<=x['epoch']*1000<=row['last_active_sample']]
 row['db_query_counter_delta']={k:int(q[-1]['status'][k])-int(q[0]['status'][k]) for k in ('Com_select','Com_insert','Com_update','Com_delete','Questions','Queries')}
 row['db_threads_running_max']=max(int(x['status']['Threads_running']) for x in q)
 row['performance_schema']=q[-1]['status']['performance_schema']
(o/'protocol-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
for name in ('artifact-identity.json','artifact-identity-pre-fix.json','test-manifest.tsv','m27-db-queries.jsonl','protocol-3-backend-race.jsonl','protocol-1-pre-fix.jsonl','protocol-1-state-timeout.jsonl','snapshot-red.log','snapshot-suite.log','state-stream.py','query-observer.py','protocol-before.tsv','integrity-final.tsv'):
 shutil.copy(r/name,o/('baseline-test-manifest.tsv' if name=='test-manifest.tsv' else name))
shutil.copytree(r/'held-native',o/'held-native',dirs_exist_ok=True,ignore=shutil.ignore_patterns('ocr','latest-state.txt'))

for p in r.glob('protocol-*-parallel-login.jsonl'):shutil.copy(p,o/p.name)
shutil.copy(r/'integrity-initial.tsv',o/'integrity-initial.tsv')
for row in summary:
 log=readjson(r/f"protocol-{row['level']}.jsonl")
 deliveries=[x['delivery_ms'] for x in log if x['event']=='private_chat_delivered' and row['first_active_sample']<=x['epoch_ms']<=row['last_active_sample']]
 row['private_chat_delivery']={'samples':len(deliveries),'p50_ms':pct(deliveries,.5),'p95_ms':pct(deliveries,.95),'p99_ms':pct(deliveries,.99)} if deliveries else None
(o/'protocol-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
