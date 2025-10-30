package levelGenerators.assignment02;

import java.util.Random;

import engine.core.MarioLevel;
import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator
{
    /******************************** CONSTANTS ********************************/
    private static final double PIT_PROBABILITY = 0.02;
    private static final double PIPE_PROBABILITY = 0.16;
    private static final double PIPE_FLOWER_PROBABILITY = 0.02;
    private static final double ENEMY_PROBABILITY = 0.36;
    private static final double WINGED_ENEMY_PROBABILITY = 0;
    private static final double COIN_PROBABILITY = 0.4;
    private static final double BRICK_BLOCK_PROBABILITY = 0.05;

    private static final int MIN_PIPE_HEIGHT = 2;
    private static final int MAX_PIPE_HEIGHT = 4;
    private static final int MIN_PIT_WIDTH = 0;
    private static final int MAX_PIT_WIDTH = 2;
    private static final int MIN_COIN_HEIGHT = 4;
    private static final int MAX_COIN_HEIGHT = 4;
    private static final int MIN_BRICK_BLOCK_HEIGHT = 3;
    private static final int MAX_BRICK_BLOCK_HEIGHT = 4;

    // private static final char[] ENEMY_TYPES = {MarioLevelModel.GOOMBA, MarioLevelModel.GREEN_KOOPA, MarioLevelModel.RED_KOOPA};
    private static final char[] ENEMY_TYPES = {MarioLevelModel.GOOMBA, MarioLevelModel.RED_KOOPA};
    // private static final char[] ENEMY_TYPES = {MarioLevelModel.GOOMBA};

    /******************************** VARIABLES ********************************/
    private Random random;

    /***************************** PRIVATE METHODS *****************************/
    private void placePipe(MarioLevelModel model, int x, int y, int height, char type)
    {
        model.setRectangle(x, y - height, 2, height, type);
    }

    /***************************** PUBLIC METHODS ******************************/
    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer)
    {
        this.random = new Random();
        model.clearMap();

        for (int x = 0; x < model.getWidth(); x++)
        {
            int y = model.getHeight() - 3;

            // place pit
            if (x > 1 &&
                x < model.getWidth() - MAX_PIT_WIDTH - 2 &&
                model.getBlock(x - 1, y + 1) == MarioLevelModel.GROUND &&
                random.nextDouble() < PIT_PROBABILITY)
            {
                x += random.nextInt(MIN_PIT_WIDTH, MAX_PIT_WIDTH);
                continue;
            }


            // place ground
            model.setBlock(x, y + 1, MarioLevelModel.GROUND);
            model.setBlock(x, y + 2, MarioLevelModel.GROUND);

            // place pipe
            if (x > 2 && x < model.getWidth() - 3 &&
                model.getBlock(x - 1, y + 1) == MarioLevelModel.GROUND &&
                model.getBlock(x - 2, y + 1) != MarioLevelModel.EMPTY &&
                model.getBlock(x - 1, y) != MarioLevelModel.PIPE &&
                model.getBlock(x - 1, y) != MarioLevelModel.PIPE_FLOWER &&
                random.nextDouble() < PIPE_PROBABILITY)
            {
                final var pipeType = random.nextDouble() < PIPE_FLOWER_PROBABILITY ? MarioLevelModel.PIPE_FLOWER : MarioLevelModel.PIPE;
                final int pipeHeight = random.nextInt(MIN_PIPE_HEIGHT, MAX_PIPE_HEIGHT);

                for (int i = pipeHeight; i >= 0; i--)
                {
                    if (model.getBlock(x - 2, y - i) == MarioLevelModel.SPECIAL_BRICK)
                    {
                        model.setBlock(x - 2, y - i, MarioLevelModel.EMPTY);
                    }
                }

                for (int i = 0; i <= y; i++)
                {
                    final var block = model.getBlock(x - 1, i);
                    if (block == MarioLevelModel.SPECIAL_BRICK || block == MarioLevelModel.COIN)
                    {
                        model.setBlock(x - 1, i - pipeHeight, block);
                        model.setBlock(x - 1, i, MarioLevelModel.EMPTY);
                    }
                }

                placePipe(model, x - 1, y + 1, pipeHeight, pipeType);
                y -= pipeHeight;
            }

            // place enemy
            if (x > 2 && x < model.getWidth() - 2 && random.nextDouble() < ENEMY_PROBABILITY)
            {
                // final var enemyChars = MarioLevelModel.getEnemyCharacters(false);
                // var enemyType = enemyChars[random.nextInt(enemyChars.length)];
                var enemyType = ENEMY_TYPES[random.nextInt(ENEMY_TYPES.length)];
                if (model.getBlock(x, y + 1) != MarioLevelModel.GROUND)
                {
                    // enemyType = MarioLevelModel.RED_KOOPA;
                    enemyType = MarioLevelModel.EMPTY;
                }
                if (random.nextDouble() < WINGED_ENEMY_PROBABILITY)
                {
                    enemyType = MarioLevelModel.getWingedEnemyVersion(enemyType, true);
                }
                model.setBlock(x, y, enemyType);
            }

            // place mushroom brick block
            if (x < model.getWidth() - 3 && random.nextDouble() < BRICK_BLOCK_PROBABILITY)
            {
                model.setBlock(x, y - random.nextInt(MIN_BRICK_BLOCK_HEIGHT, MAX_BRICK_BLOCK_HEIGHT), MarioLevelModel.SPECIAL_BRICK);
            }

            if (x < model.getWidth() - 2)
            {
                // place coins
                for (int i = MIN_COIN_HEIGHT; i <= MAX_COIN_HEIGHT; i++)
                {
                    if (model.getBlock(x, y - i) == MarioLevelModel.EMPTY && random.nextDouble() < COIN_PROBABILITY)
                    {
                        model.setBlock(x, y - i, MarioLevelModel.COIN);
                    }
                }
            }
        }

        // place Mario start and finish points
        model.setBlock(1, model.getHeight() - 3, MarioLevelModel.MARIO_START);
        model.setBlock(model.getWidth() - 2, model.getHeight() - 3, MarioLevelModel.PYRAMID_BLOCK);
        model.setBlock(model.getWidth() - 2, model.getHeight() - 4, MarioLevelModel.MARIO_EXIT);

        return model.getMap();
    }

    @Override
    public String getGeneratorName()
    {
        return "Assignment02LevelGenerator";
    }
}
