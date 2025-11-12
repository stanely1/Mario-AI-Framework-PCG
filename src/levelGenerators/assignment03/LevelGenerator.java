package levelGenerators.assignment03;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator
{
    private static final int LEVEL_HEIGHT = 16;
    private static final int LEVEL_WIDTH  = 150;

    private final Random random = new Random();
    private List<String> columns = new ArrayList<>();
    private List<String> startColumns = new ArrayList<>();;
    private List<String> finishColumns = new ArrayList<>();;

    // TODO:
    // - separate lists for columns with Mario starting and finish position
    // - mutation
    // - simulated annealing
    // - objective functions for each task
    // - some different search/evolution?

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

            this.columns = List.of("--------------XX");
            this.startColumns = List.of("-------------MXX");
            this.finishColumns = List.of("-------------FXX");
        }

        // columns.forEach(System.out::println);
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer)
    {
        var level = getRandomLevel();
        return decodeLevel(level);
    }

    @Override
    public String getGeneratorName()
    {
        return "Assignment03LevelGenerator";
    }

    private int[] getRandomLevel()
    {
        int[] v = new int[LEVEL_WIDTH];
        v[0] = random.nextInt(startColumns.size());
        v[LEVEL_WIDTH - 1] = random.nextInt(finishColumns.size());
        for (int i = 1; i < LEVEL_WIDTH - 1; i++) v[i] = random.nextInt(columns.size());
        return v;
    }

    private String decodeLevel(final int[] vec)
    {
        String[] result = new String[LEVEL_HEIGHT];
        Arrays.fill(result, "");

        for (int i = 0; i < LEVEL_WIDTH; i++)
        {
            final String col = switch (i) {
                case 0 -> startColumns.get(vec[i]);
                case LEVEL_WIDTH - 1 -> finishColumns.get(vec[i]);
                default -> columns.get(vec[i]);
            };

            for (int j = 0; j < LEVEL_HEIGHT; j++)
            {
                result[j] += col.charAt(j);
            }
        }

        return String.join("\n", result);
    }
}
