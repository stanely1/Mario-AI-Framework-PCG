import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import engine.core.MarioAgent;
import engine.core.MarioGame;
import engine.core.MarioLevelModel;
import engine.core.MarioResult;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

public class GenerateLevel {
    public static int TIMER = 45;
    public static int LEVEL_WIDTH = 150;
    public static int LEVEL_HEIGHT = 16;

    public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
        }
        return content;
    }

    public static void writeLevel(String dirname, int num, String level) {
        try {
            String dirPath = "levels/" + dirname;
            Files.createDirectories(Paths.get(dirPath));
            String filePath = dirPath + "/lvl-" + num + ".txt";
            Files.write(Paths.get(filePath), level.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to write level to file: " + e.getMessage());
        }
    }

    public static void evolution(final String task, final int id)
    {
        var generator = new levelGenerators.assignment03.LevelGenerator(task, id);

        MarioGame game = new MarioGame();
        MarioAgent[] agents = switch (task) {
            case "robin"     -> new MarioAgent[]{new agents.robinBaumgarten.Agent()};
            case "killer"    -> new MarioAgent[]{new agents.killer.Agent()};
            case "collector" -> new MarioAgent[]{new agents.collector.Agent(), new agents.killer.Agent()};
            default          -> new MarioAgent[]{new agents.robinBaumgarten.Agent()};
        };
        String level;
        boolean passed;

        do {
            level = generator.getGeneratedLevel(new MarioLevelModel(LEVEL_WIDTH, LEVEL_HEIGHT), new MarioTimer(5 * 60 * 60 * 1000));
            passed = true;
            for (var agent : agents) {
                // need to pass 10 games to be accepted
                for (int i = 0; i < 10; i++) {
                    MarioResult result = game.runGame(agent, level, TIMER, 0, false);
                    if (result.getGameStatus() != GameStatus.WIN) {
                        passed = false;
                        break;
                    }
                }
                if (!passed) break;
            }
        } while (!passed);

        String dirname = generator.getGeneratorName() + "/" + task;
        writeLevel(dirname, id, level);
        try {
            var values = generator.getValueHistory();
            var valuesString = String.join(" ", values.stream().map(String::valueOf).toList());
            Files.write(Paths.get("levels/" + dirname + "/lvl-" + id + "-values.txt"), valuesString.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to write level values to file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Missing arguments: <task> {robin|killer|collector}, [optional] gen");
            System.exit(1);
        }

        final String task = args[0];
        final int numLevels = 10;

        // var generator = new levelGenerators.assignment03.LevelGenerator(task, 0);

        /****************** generate levels ******************/
        if (args.length >= 2 && args[1].equals("gen"))
        {
            ExecutorService executor = Executors.newFixedThreadPool(numLevels);
            CompletableFuture<?>[] futures = new CompletableFuture<?>[numLevels];

            for (int i = 0; i < numLevels; i++) {
                final int id = i;
                futures[i] = CompletableFuture.runAsync(() -> evolution(task, id), executor);
            }

            CompletableFuture.allOf(futures).join();
            executor.shutdown();
        }

        /****************** run agents **********************/
        // MarioAgent agent = new agents.robinBaumgarten.Agent();
        // MarioAgent agent = new agents.collector.Agent();
        // MarioAgent agent = new agents.killer.Agent();
        MarioAgent[] agents = switch (task) {
            case "robin"     -> new MarioAgent[]{new agents.robinBaumgarten.Agent()};
            case "killer"    -> new MarioAgent[]{new agents.killer.Agent()};
            case "collector" -> new MarioAgent[]{new agents.collector.Agent(), new agents.killer.Agent()};
            default          -> new MarioAgent[]{new agents.robinBaumgarten.Agent()};
        };

        for (var agent : agents) {
            System.out.println(String.format("Task: %s, agent: %s", task, agent.getAgentName()));

            MarioGame game = new MarioGame();
            int passed = 0, coins = 0, kills = 0;
            for (int i = 0; i < numLevels; i++) {
                String level = getLevel(String.format("levels/Assignment03LevelGenerator/%s/lvl-%d.txt", task, i));
                MarioResult result = game.runGame(agent, level, TIMER, 0, true);

                if (result.getGameStatus() == GameStatus.WIN) passed++;
                coins += result.getCurrentCoins();
                kills += result.getKillsTotal();

                System.out.println(String.format(
                    "[level %d] passed: %b, coins: %d, kills: %d",
                    i,
                    result.getGameStatus() == GameStatus.WIN,
                    result.getCurrentCoins(),
                    result.getKillsTotal()));
            }

            System.out.println("");
            System.out.println("Passed %:     " + 100.0*passed/numLevels);
            System.out.println("Avg. coins:   " + (double)coins/numLevels);
            System.out.println("Avg. enemies: " + (double)kills/numLevels);
        }

        // Results:
        // Task: killer, agent: Killer
        // [level 0] passed: true, coins: 11, kills: 59
        // [level 1] passed: true, coins: 8, kills: 43
        // [level 2] passed: true, coins: 11, kills: 50
        // [level 3] passed: true, coins: 8, kills: 47
        // [level 4] passed: true, coins: 14, kills: 47
        // [level 5] passed: true, coins: 6, kills: 44
        // [level 6] passed: true, coins: 16, kills: 61
        // [level 7] passed: true, coins: 3, kills: 43
        // [level 8] passed: true, coins: 19, kills: 50
        // [level 9] passed: true, coins: 10, kills: 44

        // Passed %:     100.0
        // Avg. coins:   10.6
        // Avg. enemies: 48.8


        // Task: robin, agent: RobinBaumgartenAgent
        // [level 0] passed: true, coins: 20, kills: 11
        // [level 1] passed: true, coins: 13, kills: 13
        // [level 2] passed: true, coins: 9, kills: 15
        // [level 3] passed: true, coins: 14, kills: 15
        // [level 4] passed: true, coins: 11, kills: 16
        // [level 5] passed: true, coins: 14, kills: 11
        // [level 6] passed: true, coins: 19, kills: 10
        // [level 7] passed: true, coins: 22, kills: 14
        // [level 8] passed: true, coins: 10, kills: 21
        // [level 9] passed: true, coins: 15, kills: 17

        // Passed %:     100.0
        // Avg. coins:   14.7
        // Avg. enemies: 14.3


        // Task: collector, agent: Collector
        // [level 0] passed: true, coins: 65, kills: 17
        // [level 1] passed: true, coins: 59, kills: 19
        // [level 2] passed: true, coins: 78, kills: 18
        // [level 3] passed: true, coins: 70, kills: 27
        // [level 4] passed: true, coins: 63, kills: 16
        // [level 5] passed: true, coins: 56, kills: 31
        // [level 6] passed: true, coins: 53, kills: 20
        // [level 7] passed: true, coins: 75, kills: 20
        // [level 8] passed: true, coins: 52, kills: 16
        // [level 9] passed: true, coins: 76, kills: 12

        // Passed %:     100.0
        // Avg. coins:   64.7
        // Avg. enemies: 19.6

        // Task: collector, agent: Killer
        // [level 0] passed: true, coins: 23, kills: 56
        // [level 1] passed: true, coins: 13, kills: 39
        // [level 2] passed: true, coins: 41, kills: 55
        // [level 3] passed: true, coins: 14, kills: 48
        // [level 4] passed: true, coins: 32, kills: 61
        // [level 5] passed: true, coins: 12, kills: 50
        // [level 6] passed: true, coins: 12, kills: 48
        // [level 7] passed: true, coins: 16, kills: 40
        // [level 8] passed: true, coins: 16, kills: 28
        // [level 9] passed: true, coins: 17, kills: 44

        // Passed %:     100.0
        // Avg. coins:   19.6
        // Avg. enemies: 46.9
    }
}
