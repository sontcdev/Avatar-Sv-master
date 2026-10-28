import pathlib,subprocess,time,json,os
root=pathlib.Path('/home/sontc/avatar2d-gameplay-stage');os.chdir(root)
assert pathlib.Path('/proc/313665/cmdline').read_bytes().find(b'server.jar')>=0
out=open('m25-host-metrics.jsonl','a',buffering=1)
while not pathlib.Path('m25-load-stop.request').exists():
 d={'epoch':time.time(),'loadavg':pathlib.Path('/proc/loadavg').read_text().strip(),'cpu':pathlib.Path('/proc/stat').read_text().splitlines()[0],'diskstats':pathlib.Path('/proc/diskstats').read_text(),'memory':pathlib.Path('/proc/meminfo').read_text(),'production':{}}
 for pid in (313665,302726):
  if not pathlib.Path(f'/proc/{pid}').exists():raise RuntimeError('Production PID changed; stop load')
  d['production'][pid]={'stat':pathlib.Path(f'/proc/{pid}/stat').read_text(),'io':pathlib.Path(f'/proc/{pid}/io').read_text() if os.access(f'/proc/{pid}/io',os.R_OK) else 'permission denied'}
 d['db']=subprocess.check_output(['mariadb','--socket=mysql.sock','-uroot','-BN','-e',"SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Innodb_row_lock_current_waits','Innodb_row_lock_time','Innodb_row_lock_waits','Slow_queries')"],text=True)
 d['service']=subprocess.check_output(['systemctl','show','avatar2d','-p','ActiveState','-p','NRestarts'],text=True).strip()
 out.write(json.dumps(d)+'\n');time.sleep(5)
