#!/usr/bin/env python3
"""Root-only legitimate non-admin test account, using User.login's MD5 contract."""
import hashlib
import os
import re
import secrets
import string
import subprocess
import sys
from pathlib import Path

if os.geteuid() != 0:
    sys.exit('Run with sudo on homelab')
name = sys.argv[1] if len(sys.argv) == 2 else 'avatartest'
if not re.fullmatch(r'[a-z][a-z0-9]{3,19}', name):
    sys.exit('Use 4-20 lowercase letters/digits, beginning with a letter')
path = Path('/etc/avatar2d/test-account-' + name + '.txt')
if path.exists():
    sys.exit('Credential file exists; left unchanged')
password = ''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(20))
digest = hashlib.md5(password.encode()).hexdigest()
# Refuse an existing account; never reset credentials or add privileged inventory.
sql = f"""INSERT INTO users
(username,password,role,active,gmail,vnd,tongnap,login_lock)
SELECT '{name}','{digest}',-1,1,'',0,0,0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='{name}');
SELECT ROW_COUNT();
"""
result = subprocess.run(['mariadb', '-N', 'avatar_2x'], input=sql, text=True, capture_output=True, check=True)
if result.stdout.strip() != '1':
    sys.exit('Account already exists; left unchanged')
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as output:
    output.write('username=' + name + '\npassword=' + password + '\n')
print('Non-admin account created; credentials saved at ' + str(path))
