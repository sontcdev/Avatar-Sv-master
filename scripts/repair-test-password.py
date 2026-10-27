#!/usr/bin/env python3
"""Repair only the dedicated test account's incompatible J2ME password."""
import hashlib
import os
from pathlib import Path
import subprocess
import re
import secrets
import string

assert os.geteuid() == 0, 'Run with sudo'
path = Path('/etc/avatar2d/test-account-avatartest.txt')
values = dict(line.split('=', 1) for line in path.read_text().splitlines())
assert values['username'] == 'avatartest'
old = values['password']
new = ''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(20))
assert 18 <= len(new) <= 20 and re.fullmatch('[A-Za-z0-9]+', new)
if old != new:
    old_hash = hashlib.md5(old.encode()).hexdigest()
    new_hash = hashlib.md5(new.encode()).hexdigest()
    sql = f"UPDATE users SET password='{new_hash}' WHERE username='avatartest' AND role=-1 AND active=1 AND password='{old_hash}'; SELECT ROW_COUNT();"
    result = subprocess.run(['mariadb', '-N', 'avatar_2x'], input=sql, text=True, capture_output=True, check=True)
    assert result.stdout.strip() == '1', 'Account mismatch; credentials left unchanged'
    path.write_text('username=avatartest\npassword=' + new + '\n')
subprocess.run(['install', '-m', '600', '-o', 'sontc', '-g', 'sontc', str(path), '/home/sontc/avatar2d-staging/test-account.txt'], check=True)
print('Dedicated non-admin test password made J2ME-compatible; private credential files updated.')
