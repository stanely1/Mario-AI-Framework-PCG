package levelGenerators.assignment03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import engine.core.MarioAgent;
import engine.core.MarioGame;
import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

public class LevelGenerator implements MarioLevelGenerator
{
    private static final int LEVEL_HEIGHT = 16;
    private static final int LEVEL_WIDTH  = 150;
    private static final int COLUMN_WIDTH = 5;
    private static final int LEVEL_ENCODING_LENGTH = LEVEL_WIDTH / COLUMN_WIDTH;
    private static final int MUTATION_CHANGED_POS_COUNT = 1;

    private final Random random = new Random();
    private List<String> columns = new ArrayList<>();
    private List<String> startColumns = new ArrayList<>();
    private List<String> finishColumns = new ArrayList<>();

    // TODO:
    // - simulated annealing
    // - objective functions for each task
    // - parameter tuning: COLUMN_WIDTH
    // - some different search/evolution?
    // - different approach to level encoding?

    public LevelGenerator()
    {
        Path columnFilePath = Paths.get("data/columns.txt");
        try
        {
            for (String col : Files.readAllLines(columnFilePath))
            {
                if (col.indexOf('M') != -1) this.startColumns.add(col);
                else if (col.indexOf('F') != -1) this.finishColumns.add(col);
                else this.columns.add(col);
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to read a file: " + columnFilePath);

            this.startColumns  = List.of("-------------MXX" + "--------------XX".repeat(COLUMN_WIDTH - 1));
            this.finishColumns = List.of("--------------XX".repeat(COLUMN_WIDTH - 1) + "-------------FXX");
            this.columns       = List.of("--------------XX".repeat(COLUMN_WIDTH));
        }

        // columns.forEach(System.out::println);
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer)
    {
        var bestLevel = getRandomLevel();
        double bestScore = evaluateLevel(bestLevel);

        // hill climbing - TODO: SA
        int N = 10;
        for (int i = 0; i < N; i++)
        {
            System.err.println(String.format("[iteration %d] best score: %f", i, bestScore));
            var newLevel = mutateLevel(bestLevel);
            double newScore = evaluateLevel(newLevel);
            if (newScore > bestScore)
            {
                bestLevel = newLevel;
                bestScore = newScore;
            }
        }
        System.err.println(String.format("[iteration %d] best score: %f", N, bestScore));

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
        final MarioGame game = new MarioGame();
        // final MarioAgent agent = new agents.robinBaumgarten.Agent();
        final MarioAgent agent = new agents.killer.Agent();
        final String levelString = decodeLevel(level);
        // TODO: pick right value for timer
        final int timer = 40;

        MarioResult runResult = game.runGame(agent, levelString, timer);
        return (runResult.getGameStatus() == GameStatus.WIN ? 150.0 : 0.0) + runResult.getKillsTotal(); // + runResult.getCurrentCoins();
    }

    private String decodeLevel(final int[] level)
    {
        String[] result = new String[LEVEL_HEIGHT];
        Arrays.fill(result, "");

        for (int i = 0; i < LEVEL_ENCODING_LENGTH; i++)
        {
            final String col = switch (i) {
                case 0 -> startColumns.get(level[i]);
                case LEVEL_ENCODING_LENGTH - 1 -> finishColumns.get(level[i]);
                default -> columns.get(level[i]);
            };

            for (int j = 0; j < LEVEL_HEIGHT; j++)
            {
                for (int k = 0; k < COLUMN_WIDTH; k++)
                    result[j] += col.charAt(j + k * LEVEL_HEIGHT);
            }
        }

        return String.join("\n", result);
    }
}
