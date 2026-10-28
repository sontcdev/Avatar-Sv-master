#!/usr/bin/env python3
"""Index dispatcher cases; unresolved trace fields stay explicitly unknown."""
import csv
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
FIELDS = ['map_npc', 'menu_leaf', 'client_command', 'payload', 'dispatcher', 'player_state', 'database_inventory', 'response_ui', 'donor', 'original_evidence', 'implementation_status', 'runtime_acceptance']

def rows():
    for path in sorted((ROOT / 'src/main/java/avatar/message').rglob('*.java')):
        for number, line in enumerate(path.read_text().splitlines(), 1):
            match = re.search(r'case\s+(Cmd\.\w+|[-\d]+)\s*:', line)
            if match:
                row = dict.fromkeys(FIELDS, 'REFERENCE_REQUIRED')
                row.update(client_command=match[1], dispatcher=f'{path.relative_to(ROOT)}:{number}', runtime_acceptance='NOT_RUN')
                yield row

if __name__ == '__main__':
    entries = list(rows())
    assert entries and len({r['dispatcher'] for r in entries}) == len(entries)
    with (ROOT / 'GAMEPLAY_ACTION_INVENTORY.csv').open('w', newline='') as output:
        writer = csv.DictWriter(output, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(entries)
    print(f'{len(entries)} dispatcher candidates indexed; not an exhaustive action inventory')
