import subprocess,time,json,sys,pathlib
port=sys.argv[1];duration=float(sys.argv[2]);level=int(sys.argv[3]);root=pathlib.Path('/tmp/avatar2d-m25')
base=['/opt/homebrew/bin/adb','-s','emulator-'+port]
def act(args,pause=2):
 subprocess.run(base+['shell','input']+args,check=True,stdout=subprocess.DEVNULL);time.sleep(pause)
def key(k,pause=2):act(['keyevent',str(k)],pause)
def tap(x,y,pause=2):act(['tap',str(x),str(y)],pause)
start=time.time();log=open(root/f'active-{port}.jsonl','a',buffering=1);n=0
log.write(json.dumps({'epoch':start,'level':level,'port':port,'event':'start'})+'\n')
while time.time()-start<duration:
 n+=1
 key(21);key(22);tap(660,1140);tap(110,490)
 if level>1 and n==1 and len(sys.argv)<5:
  key(66);act(['text',f'M25_LOAD_{level}_{port}_{n}']);key(66)
  key(1);key(20);key(66,2);tap(660,1140);tap(110,490)
 key(1);key(20);key(20);key(66,4)
 subprocess.run(base+['shell','screencap','-p',f'/sdcard/m25-load.png'],check=True,stdout=subprocess.DEVNULL)
 subprocess.run(base+['pull','/sdcard/m25-load.png',str(root/f'load-{level}-{port}-farm.png')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 key(21);key(22);key(66);key(20);key(20);key(20);key(66,2);key(66,5)
 log.write(json.dumps({'epoch':time.time(),'level':level,'cycle':n,'port':port,'event':'native UI movement, friend-farm visit/bootstrap, exit, park'})+'\n')
print('finished',port,n,'cycles',time.time()-start)
