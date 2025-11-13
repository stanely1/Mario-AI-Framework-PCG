from pathlib import Path

# LEVELS_DIR = Path('./levels/')
LEVELS_DIR = Path('./levels/original')

COLUMN_WIDTH = 5

columns = set()

for file_path in LEVELS_DIR.rglob('*'):
    if file_path.is_file() and file_path.name.startswith('lvl-'):
        with file_path.open('r') as file:
            lines = [line.strip() for line in file.readlines()]
            single_columns = [''.join(c) for c in zip(*lines)]

            columns.update(''.join(single_columns[i:i+COLUMN_WIDTH]) for i in range(len(single_columns) - COLUMN_WIDTH + 1))
            # columns.update(''.join(c) for c in zip(*lines))

for c in sorted(columns):
    print(c)
