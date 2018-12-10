package SLP;

import java.util.Random;

public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    /*************************** CONFIGURATION *********************************/
    public static final int NUMBER_OF_INSTANCES = 100;
    public static final int NUMBER_OF_ITEMS = 20;
    public static final int STACK_CAPACITY = 3;

    // The number of stacks m is initially m = n / b,
    // this number specifies the percentage by which the initial m gets increased.
    public static final int ADDITIONAL_STACK_PERCENTAGE = 20;

    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS = 0.0825F;

    public static final int COSTS_INCLUSIVE_LOWER_BOUND = 0;
    public static final int COSTS_EXCLUSIVE_UPPER_BOUND = 10;
    /***************************************************************************/

    public static void main(String[] args) {

        int numOfStacks = (NUMBER_OF_ITEMS / STACK_CAPACITY);

        if (ADDITIONAL_STACK_PERCENTAGE > 0) {
            numOfStacks = (int)(Math.ceil(numOfStacks + numOfStacks * 0.2));
        }

        for (int idx = 0; idx < NUMBER_OF_INSTANCES; idx++) {

            int[][] matrix = TestDataGenerator.generateStackingConstraintMatrix(NUMBER_OF_ITEMS, NUMBER_OF_ITEMS, true);

            int[][] costs = new int[NUMBER_OF_ITEMS][numOfStacks];
            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < numOfStacks; j++) {
                    Random rand = new Random();
                    costs[i][j] = rand.nextInt(COSTS_EXCLUSIVE_UPPER_BOUND - COSTS_INCLUSIVE_LOWER_BOUND) + COSTS_INCLUSIVE_LOWER_BOUND;
                }
            }

            String instanceName = "slp_instance_" + NUMBER_OF_ITEMS + "_" + numOfStacks + "_" + STACK_CAPACITY + "_" + idx;

            Instance instance = new Instance(NUMBER_OF_ITEMS, numOfStacks, STACK_CAPACITY, matrix, costs, instanceName);
            InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);
            InstanceWriter.writeConfig(
                    INSTANCE_PREFIX + "config.txt",
                    NUMBER_OF_INSTANCES,
                    NUMBER_OF_ITEMS,
                    STACK_CAPACITY,
                    ADDITIONAL_STACK_PERCENTAGE,
                    CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS,
                    COSTS_INCLUSIVE_LOWER_BOUND,
                    COSTS_EXCLUSIVE_UPPER_BOUND
            );
        }
    }

    public static int[][] generateStackingConstraintMatrix(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                } else {
                    if (Math.random() >= 1.0 - CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS) {
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
