from pathlib import Path

# TODO: select good data
SUBDIRS = ['original']
COLUMN_WIDTH = 5

columns = set()

for subdir in SUBDIRS:
    # LEVELS_DIR = Path('./levels/')
    LEVELS_DIR = Path(f'./levels/{subdir}')

    # i = 0
    for file_path in LEVELS_DIR.rglob('*'):
        if file_path.is_file() and file_path.name.startswith('lvl-'):
            # i += 1
            with file_path.open('r') as file:
                lines = [line.strip() for line in file.readlines()]
                single_columns = [''.join(c) for c in zip(*lines)]

                columns.update(''.join(single_columns[i:i+COLUMN_WIDTH]) for i in range(len(single_columns) - COLUMN_WIDTH + 1))
                # columns.update(''.join(c) for c in zip(*lines))
        # if i == 10: break

for c in sorted(columns):
    print(c)
