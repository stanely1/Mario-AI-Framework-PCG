from pathlib import Path

SUBDIRS = ['original', 'Assignment02LevelGenerator', 'BenWeberLevelGenerator']
COLUMN_WIDTH = 5
COLUMN_HEIGHT = 16

def fix_broken_pipes(col):
    single_cols = ['-' * COLUMN_HEIGHT]
    single_cols += [col[i:i+COLUMN_HEIGHT] for i in range(0, COLUMN_WIDTH*COLUMN_HEIGHT, COLUMN_HEIGHT)]
    single_cols.append('-' * COLUMN_HEIGHT)
    for i in range(1, len(single_cols) - 1):
        for j in range(COLUMN_HEIGHT):
            c = single_cols[i][j]
            if c.lower() == 't' and single_cols[i-1][j] != c and single_cols[i+1][j] != c:
                single_cols[i] = single_cols[i].replace(c, '-')

    # print(col)
    # print(''.join(single_cols[1:-1]))
    # print()

    return ''.join(single_cols[1:-1])


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

columns = set(map(lambda c: (fix_broken_pipes(c[0]), c[1]), columns))

for c, s in sorted(columns):
    print(c, s)

# fix_broken_pipes(next(iter(columns))[0])

# fix_broken_pipes('----%XXXXXXXXXXX----%||||XXXXXXX---------XXXXXXX---------XXXXXXX-----ttttXXXXXXX')
# fix_broken_pipes('----%tttXXXXXXXX---G%|||XXXXXXXX----%|||XXXXXXXX-------%XXXXXXXX------G%XXXXXXXX')
# fix_broken_pipes('----%tttXXXXXXXX---G%|ttXXXXXXXX---G%|ttXXXXXXXX----%|||XXXXXXXX----%|||XXXXXXXX')
# fix_broken_pipes('----%|XXXXXXXXXX----%|XXXXXXXXXX--ttttXXXXXXXXXX-ottttXXXXXXXXXX---G%|XXXXXXXXXX')
# fix_broken_pipes('-------------#XX------------##XX-----------###XX----------TTTTXX----------TTTTXX')
# fix_broken_pipes('-------------#XX------------##XX-----------###XX---------------------------TTTXX')
