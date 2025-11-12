from pathlib import Path

# LEVELS_DIR = Path('./levels/')
LEVELS_DIR = Path('./levels/original')

columns = set()

for file_path in LEVELS_DIR.rglob('*'):
    if file_path.is_file() and file_path.name.startswith('lvl-'):
        with file_path.open('r') as file:
            lines = [line.strip() for line in file.readlines()]
            columns.update(''.join(c) for c in zip(*lines))

for c in sorted(columns):
    print(c)
