import pathlib,os,subprocess,json,time
r=pathlib.Path('/home/sontc/avatar2d-gameplay-stage');os.chdir(r)
assert r.resolve()==pathlib.Path.cwd()
with open('m27-db-queries.jsonl','a',buffering=1) as out:
 while not pathlib.Path('m27-load-stop.request').exists():
  text=subprocess.check_output(['mariadb','--socket=mysql.sock','-uroot','-BN','-e',"SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_select','Com_insert','Com_update','Com_delete','Questions','Queries','Threads_running','Threads_connected','Innodb_row_lock_waits','Innodb_row_lock_time'); SHOW GLOBAL VARIABLES LIKE 'performance_schema';"],text=True,timeout=5)
  out.write(json.dumps({'epoch':time.time(),'status':dict(x.split('\t') for x in text.splitlines())})+'\n');time.sleep(5)
