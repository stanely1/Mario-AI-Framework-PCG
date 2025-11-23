from pathlib import Path

# TODO: select good data
SUBDIRS = ['original', 'Assignment02LevelGenerator', 'BenWeberLevelGenerator']
COLUMN_WIDTH = 5

columns = set()

for subdir in SUBDIRS:
    # LEVELS_DIR = Path('./levels/')
    LEVELS_DIR = Path(f'./levels/{subdir}')

    for file_path in LEVELS_DIR.rglob('*'):
        if file_path.is_file() and file_path.name.startswith('lvl-'):
            with file_path.open('r') as file:
                lines = [line.strip() for line in file.readlines()]
                single_columns = [''.join(c) for c in zip(*lines)]

                columns.update((''.join(single_columns[i:i+COLUMN_WIDTH]), subdir) for i in range(len(single_columns) - COLUMN_WIDTH + 1))

for c, s in sorted(columns):
    print(c, s)
