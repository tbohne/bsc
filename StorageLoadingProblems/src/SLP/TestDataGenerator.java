package SLP;

import java.util.Random;

public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    /*************************** CONFIGURATION *********************************/
    public static final int INITIAL_NUMBER_OF_ITEMS = 20;
    public static final int NUMBER_OF_SETUPS = 1;
    public static final int NUMBER_OF_ITEMS_ADDED_PER_SETUP = 1;
    public static final float ITEM_TO_STACK_MULTIPLIER = (float)(3.0 / 8.0);
    public static final float STACK_CAP_MULTIPLIER = (float)(2.0 / 3.0);
    public static final int NUMBER_OF_INSTANCES_PER_SETUP = 5;

    public static final int COSTS_INCLUSIVE_LOWER_BOUND = 0;
    public static final int COSTS_EXCLUSIVE_UPPER_BOUND = 10;

    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS_LB = 0.89F;
    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS_UB = 0.996F;
    /***************************************************************************/

    public static void main(String[] args) {

        for (int setup = 0; setup < NUMBER_OF_SETUPS; setup++) {

//            int numOfItems = INITIAL_NUMBER_OF_ITEMS + setup * NUMBER_OF_ITEMS_ADDED_PER_SETUP;
//            int numOfStacks = (int)(numOfItems * ITEM_TO_STACK_MULTIPLIER);
//            int stackCap = (numOfItems + (int)Math.ceil(numOfItems * STACK_CAP_MULTIPLIER)) / numOfStacks;

            int numOfItems = INITIAL_NUMBER_OF_ITEMS + setup * NUMBER_OF_ITEMS_ADDED_PER_SETUP;
            int numOfStacks = 7;
            int stackCap = 3;

            for (int idx = 0; idx < NUMBER_OF_INSTANCES_PER_SETUP; idx++) {

                int[][] matrix = TestDataGenerator.generateStackingConstraintMatrix(numOfItems, numOfItems, true);

                int[][] costs = new int[numOfItems][numOfStacks];
                for (int i = 0; i < numOfItems; i++) {
                    for (int j = 0; j < numOfStacks; j++) {
                        Random rand = new Random();
                        costs[i][j] = rand.nextInt(COSTS_EXCLUSIVE_UPPER_BOUND - COSTS_INCLUSIVE_LOWER_BOUND) + COSTS_INCLUSIVE_LOWER_BOUND;
                    }
                }

                String instanceName = "slp_instance_" + numOfItems + "_" + numOfStacks + "_" + stackCap + "_" + idx;

                Instance instance = new Instance(numOfItems, numOfStacks, stackCap, matrix, costs, instanceName);
                InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);
            }
        }
    }

    private static double mapRange(double numberOfItems) {
        return CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS_LB
                + ((numberOfItems - INITIAL_NUMBER_OF_ITEMS)
                * (CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS_UB - CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS_LB))
                / ((INITIAL_NUMBER_OF_ITEMS + NUMBER_OF_ITEMS_ADDED_PER_SETUP * NUMBER_OF_SETUPS) - INITIAL_NUMBER_OF_ITEMS);
    }

    public static int[][] generateStackingConstraintMatrix(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                } else {
//                    double chance = dimOne > 20 ? mapRange(dimOne) : mapRange(dimOne) - 0.1;
                    double chance = mapRange(dimOne);
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
