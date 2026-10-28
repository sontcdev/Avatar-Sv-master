import pathlib,subprocess,collections,time,json
r=pathlib.Path('/tmp/avatar2d-m27');rows=collections.deque(maxlen=100)
p=subprocess.Popen(['ssh','homelab','cd /home/sontc/avatar2d-gameplay-stage && tail -n 30 -F m27-state.csv'],stdout=subprocess.PIPE,text=True,stderr=open(r/'state-stream-ssh.log','a'))
try:
 for line in p.stdout:
  if len(line.strip().split(','))!=10:continue
  rows.append(line)
  (r/'state-next.txt').write_text(''.join(rows));(r/'state-next.txt').replace(r/'latest-state.txt')
finally:p.terminate();p.wait(timeout=10)
