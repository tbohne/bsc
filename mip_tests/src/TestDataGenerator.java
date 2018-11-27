import java.util.Random;

public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    public static void main(String[] args) {

        int numOfItemsInit = 10;
//        int numOfStacksInit = 5;
//        int stackCapInit = 3;

        for (int setup = 0; setup < 20; setup++) {

            int numOfItems = numOfItemsInit + setup * 10;
            int numOfStacks = numOfItems / 2 - numOfItems / 8;
            int x = numOfItems + (int)Math.ceil((numOfItems * 2) / 3);
            int stackCap = x / numOfStacks;

            // Creates 10 instances for each problem setup.
            for (int idx = 0; idx < 10; idx++) {

                int[][] matrix = TestDataGenerator.generateStackingConstraintMatrix(numOfItems, numOfItems, true);

                int[][] costs = new int[numOfItems][numOfStacks];
                for (int i = 0; i < numOfItems; i++) {
                    for (int j = 0; j < numOfStacks; j++) {

                        Random r = new Random();
                        // inclusive
                        int low = 0;
                        // exclusive
                        int high = 10;

                        costs[i][j] = r.nextInt(high-low) + low;;
                    }
                }

                String instanceName = "slp_instance_" + numOfItems + "_" + numOfStacks + "_" + stackCap + "_" + idx;

                Instance instance = new Instance(numOfItems, numOfStacks, stackCap, matrix, costs, instanceName);
                InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);
            }
        }
    }

    private static double mapRange(double numberOfItems) {
        return 0.96 + ((numberOfItems - 10) * (0.99 - 0.96)) / (200 - 10);
    }

    public static int[][] generateStackingConstraintMatrix(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                } else {
                    double chance = dimOne > 20 ? mapRange(dimOne) : mapRange(dimOne) - 0.1;
                    System.out.println("CHANCE: " + chance);
                    if (Math.random() >= /* 0.90 */ chance) {
                        matrix[i][j] = 1;
                    } else {
                        matrix[i][j] = 0;
                    }
                }
            }
        }

        if (transitiveStackingConstraints) {
            matrix = makeMatrixTransitive(matrix);
        }
        return matrix;
    }

    public static int[][] makeMatrixTransitive(int[][] matrix) {

        // The ones that are added can then induce new ones in other rows,
        // therefore we have to repeat the process as long as there are changes.

        boolean somethingChanged = true;

        while (somethingChanged) {
            somethingChanged = false;
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    // we only have to add the ones that follow from transitivity
                    if (i != j && matrix[i][j] == 0) {
                        for (int k = 0; k < matrix[0].length; k++) {
                            if (matrix[i][k] == 1 && i != k && j != k) {
                                if (matrix[k][j] == 1) {
                                    matrix[i][j] = 1;
                                    somethingChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return matrix;
    }
}
