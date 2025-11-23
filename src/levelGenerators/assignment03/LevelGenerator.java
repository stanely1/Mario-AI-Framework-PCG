package levelGenerators.assignment03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator
{
    private static final int LEVEL_HEIGHT = 16;
    private static final int LEVEL_WIDTH  = 150;
    private static final int COLUMN_WIDTH = 5;
    private static final int LEVEL_ENCODING_LENGTH = LEVEL_WIDTH / COLUMN_WIDTH;
    private static final int MUTATION_CHANGED_POS_COUNT = 1;
    private static final double WIN_SCORE = 1000.0;

    private final Random random = new Random();

    private record Column(String value, String origin) {};
    private List<Column> columns = new ArrayList<>();
    private List<Column> startColumns = new ArrayList<>();
    private List<Column> finishColumns = new ArrayList<>();

    private List<Double> valueHistory = new ArrayList<>();

    private final String task;
    private final int id;

    private static final Set<Integer> enemyChars = charArrayToSet(MarioLevelModel.getEnemyCharacters());
    private static final Set<Integer> nonBlockingChars = charArrayToSet(MarioLevelModel.getNonBlockingTiles());

    private Integer notPassableColumnIdx = null;

    public LevelGenerator(final String task, final int id)
    {
        this.task = task;
        this.id = id;

        Path columnFilePath = Paths.get("data/columns.txt");
        try
        {
            for (String colString : Files.readAllLines(columnFilePath))
            {
                final var colSplit = colString.split(" ");
                final String colValue = colSplit[0];
                final String colOrigin = colSplit[1];
                final Column col = new Column(colValue, colOrigin);

                if (colValue.indexOf('M') != -1) this.startColumns.add(col);
                else if (colValue.indexOf('F') != -1) this.finishColumns.add(col);
                else this.columns.add(col);
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to read a file: " + columnFilePath);

            this.startColumns  = List.of(new Column("-------------MXX" + "--------------XX".repeat(COLUMN_WIDTH - 1), "Default"));
            this.finishColumns = List.of(new Column("--------------XX".repeat(COLUMN_WIDTH - 1) + "-------------FXX", "Default"));
            this.columns       = List.of(new Column("--------------XX".repeat(COLUMN_WIDTH), "Default"));
        }

        // columns.forEach(System.out::println);
    }

    public List<Double> getValueHistory()
    {
        return valueHistory;
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer)
    {
        valueHistory.clear();

        var bestLevel = getRandomLevel();
        double bestScore = evaluateLevel(bestLevel);

        var currentLevel = bestLevel;
        double currentScore = bestScore;
        int noScoreChange = 0;

        // hill climbing - TODO: SA
        int N = 30000;
        for (int i = 0; i < N; i++)
        {
            System.err.println(String.format("[%d] iteration %d -> best score: %f", this.id, i, bestScore));
            valueHistory.add(bestScore);

            var newLevel = mutateLevel(currentLevel);
            double newScore = evaluateLevel(newLevel);
            if (newScore > currentScore) {
                currentLevel = newLevel;
                currentScore = newScore;
            } else {
                // if no change for long time, perform random restart
                noScoreChange++;
                if (noScoreChange >= 400) {
                    currentLevel = getRandomLevel();
                    currentScore = evaluateLevel(currentLevel);
                    noScoreChange = 0;
                }
            }
            if (newScore > bestScore) {
                bestLevel = newLevel;
                bestScore = newScore;
            }
        }
        System.err.println(String.format("[%d] iteration %d -> best score: %f", this.id, N, bestScore));
        valueHistory.add(bestScore);

        return decodeLevel(bestLevel);
    }

    @Override
    public String getGeneratorName()
    {
        return "Assignment03LevelGenerator";
    }

    private int[] getRandomLevel()
    {
        int[] v = new int[LEVEL_ENCODING_LENGTH];
        v[0] = random.nextInt(startColumns.size());
        v[LEVEL_ENCODING_LENGTH - 1] = random.nextInt(finishColumns.size());
        for (int i = 1; i < LEVEL_ENCODING_LENGTH - 1; i++) v[i] = random.nextInt(columns.size());
        return v;
    }

    private int[] mutateLevel(final int[] level)
    {
        int[] newLevel = level.clone();

        // if level not passable, mutate the column that is not passable
        if (notPassableColumnIdx != null)
        {
            final int maxVal = switch (notPassableColumnIdx) {
                case 0 -> startColumns.size();
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.size();
                default -> columns.size();
            };
            newLevel[notPassableColumnIdx] = random.nextInt(maxVal);
        }

        // mutate K columns
        for (int i = 0; i < MUTATION_CHANGED_POS_COUNT; i++)
        {
            final int idx = random.nextInt(LEVEL_ENCODING_LENGTH);
            final int maxVal = switch (idx) {
                case 0 -> startColumns.size();
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.size();
                default -> columns.size();
            };
            newLevel[idx] = random.nextInt(maxVal);
        }
        return newLevel;
    }

    private double evaluateLevel(final int[] level)
    {
        if (this.task.equals("robin")) return evaluateLevelForRobinTask(level);
        if (this.task.equals("killer")) return evaluateLevelForKillerTask(level);

        return random.nextDouble();
    }

    private double evaluateLevelForRobinTask(final int[] level)
    {
        // TODO: check some other elements?
        return commonEval(level)
             + 3.0 * countEnemies(level)
             + countCoins(level)
             + 0.4 * countPlantPipes(level);
    }

    private double evaluateLevelForKillerTask(final int[] level)
    {
        // final MarioGame game = new MarioGame();
        // final MarioAgent agent = new agents.killer.Agent();
        // final String levelString = decodeLevel(level);
        // // TODO: pick right value for timer
        // final int timer = 30;

        // MarioResult runResult = game.runGame(agent, levelString, timer, 0, false);
        // return (runResult.getGameStatus() == GameStatus.WIN ? 200.0 : 0.0) + runResult.getKillsTotal();

        return commonEval(level) + 3.0 * countEnemies(level);
    }

    private double commonEval(final int[] level)
    {
        // Bonus for passability and diversity (columns from different origins)
        return (isPassable(level) ? WIN_SCORE : 0.0)
             + 100.0 * countColumnOrigins(level)
             + 7.0 * leastCommonColumnOriginCount(level);
    }

    private boolean isPassable(final int[] level)
    {
        final String[] lines = decodeLevel(level).split("\n");

        int[][] dist = new int[lines.length][lines[0].length()];
        for (var row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        PriorityQueue<int[]> Q = new PriorityQueue<>((a, b) -> Integer.compare(a[2], b[2]));

        for (int i = 0; i < lines.length; i++)
        {
            final int marioPos = lines[i].indexOf('M');
            if (marioPos != -1) {
                var entry = new int[]{i, marioPos, aStarHeuristic(i, marioPos)};
                dist[entry[0]][entry[1]] = 0;
                Q.add(entry);
            }
        }

        int maxJ = 0;
        while (!Q.isEmpty())
        {
            var entry = Q.poll();
            int i = entry[0], j = entry[1];
            maxJ = Integer.max(maxJ, j);

            // generate list of possible moves
            List<int[]> moves = new ArrayList<>();

            // on ground - can jump
            if (isInBounds(i + 1, j) && !nonBlockingChars.contains((int)lines[i + 1].charAt(j))) {
                if (isInBounds(i, j + 1) && nonBlockingChars.contains((int)lines[i].charAt(j + 1))) {
                    moves.add(new int[]{i, j + 1});
                }
                for (int di = 1; di <= 3; di++) {
                    int addedMoves = 0;
                    for (int dj = 0; dj <= di; dj++) {
                        int ni = i - di, nj = j + dj;
                        if (isInBounds(ni, nj) && nonBlockingChars.contains((int)lines[ni].charAt(nj))) {
                            moves.add(new int[]{ni, nj});
                            addedMoves++;
                        } else break;
                    }
                    if (addedMoves == 0) break;
                }
            } else { // in air - can only fall down
                if (isInBounds(i + 1, j)) {
                    moves.add(new int[]{i + 1, j});
                }
                if (isInBounds(i + 1, j + 1) && nonBlockingChars.contains((int)lines[i + 1].charAt(j + 1))) {
                    moves.add(new int[]{i + 1, j + 1});
                }
            }

            // iterate over possible moves, add them to queue, if 'F' found -> return
            for (var move : moves) {
                int mi = move[0], mj = move[1];

                if (lines[mi].charAt(mj) == MarioLevelModel.MARIO_EXIT)
                {
                    notPassableColumnIdx = null;
                    return true;
                }

                int distChange = Math.abs(mi - i) + Math.abs(mj - j);
                int newDist = dist[i][j] + distChange;
                if (newDist < dist[mi][mj]) {
                    var newEntry = new int[]{mi, mj, newDist + aStarHeuristic(mi, mj)};
                    dist[mi][mj] = newDist;
                    Q.add(newEntry);
                }
            }
        }

        notPassableColumnIdx = maxJ / COLUMN_WIDTH;
        return false;
    }

    private int aStarHeuristic(final int i, final int j)
    {
        return LEVEL_WIDTH - j;
    }

    private boolean isInBounds(final int i, final int j)
    {
        return 0 <= i && i < LEVEL_HEIGHT && 0 <= j && j < LEVEL_WIDTH;
    }

    private long countEnemies(final int[] level)
    {
        final String levelString = decodeLevel(level);
        return levelString.chars().filter(c -> enemyChars.contains(c)).count();
    }

    private long countCoins(final int[] level)
    {
        final String levelString = decodeLevel(level);
        return levelString.chars().filter(c -> c == MarioLevelModel.COIN).count();
    }

    private long countPlantPipes(final int[] level)
    {
        final String levelString = decodeLevel(level);
        return levelString.chars().filter(c -> c == MarioLevelModel.PIPE_FLOWER).count();
    }

    private long countColumnOrigins(final int[] level)
    {
        return Arrays.stream(level)
            .mapToObj(i -> switch (i) {
                case 0 -> startColumns.get(i).origin();
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.get(i).origin();
                default -> columns.get(i).origin();
            })
            .distinct()
            .count();
    }

    private long leastCommonColumnOriginCount(final int[] level)
    {
        final var counts = Arrays.stream(level)
            .mapToObj(i -> switch (i) {
                case 0 -> startColumns.get(i).origin();
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.get(i).origin();
                default -> columns.get(i).origin();
            })
            .collect(Collectors.groupingBy(origin -> origin, Collectors.counting()));

        return counts.values().stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);
    }

    private String decodeLevel(final int[] level)
    {
        String[] result = new String[LEVEL_HEIGHT];
        Arrays.fill(result, "");

        for (int i = 0; i < LEVEL_ENCODING_LENGTH; i++)
        {
            final String col = switch (i) {
                case 0 -> startColumns.get(level[i]).value();
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.get(level[i]).value();
                default -> columns.get(level[i]).value();
            };

            for (int j = 0; j < LEVEL_HEIGHT; j++)
            {
                for (int k = 0; k < COLUMN_WIDTH; k++)
                    result[j] += col.charAt(j + k * LEVEL_HEIGHT);
            }
        }

        return String.join("\n", result);
    }

    private static Set<Integer> charArrayToSet(final char[] chars)
    {
        return IntStream.range(0, chars.length).map(i -> chars[i]).boxed().collect(Collectors.toSet());
    }
}
