package SLP.experiments;

import SLP.representations.Instance;
import SLP.representations.InstanceWriter;
import SLP.representations.Item;
import SLP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.Random;

public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    /*************************** CONFIGURATION *********************************/
    public static final int NUMBER_OF_INSTANCES = 200;
    public static final int NUMBER_OF_ITEMS = 500;
    public static final int STACK_CAPACITY = 3;

    // The number of stacks m is initially m = n / b,
    // this number specifies the percentage by which the initial m gets increased.
    public static final int ADDITIONAL_STACK_PERCENTAGE = 50;

    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS = 0.0055F;
    public static final float CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS = 0.7F;

    public static final int COSTS_INCLUSIVE_LOWER_BOUND = 1;
    public static final int COSTS_EXCLUSIVE_UPPER_BOUND = 10;

    // Configuration of 2nd approach to stacking constraint generation:
    public static final boolean USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE = false;
    public static final int ITEM_LENGTH_LB = 1;
    public static final int ITEM_LENGTH_UB = 30;
    public static final int ITEM_WIDTH_LB = 1;
    public static final int ITEM_WIDTH_UB = 30;
    /***************************************************************************/

    public static void main(String[] args) {

        int numOfStacks = (NUMBER_OF_ITEMS / STACK_CAPACITY);
        if (ADDITIONAL_STACK_PERCENTAGE > 0) {
            numOfStacks = (int)(Math.ceil(numOfStacks + numOfStacks * ((float)ADDITIONAL_STACK_PERCENTAGE / 100.0)));
        }

        float avgPercentage = 0;

        for (int idx = 0; idx < NUMBER_OF_INSTANCES; idx++) {
            int[][] stackingConstraintMatrix;
            if (USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE) {
                stackingConstraintMatrix = TestDataGenerator.generateStackingConstraintMatrixApproachOne(NUMBER_OF_ITEMS, NUMBER_OF_ITEMS, true);
            } else {
                stackingConstraintMatrix = generateStackingConstraintMatrixApproachTwo(NUMBER_OF_ITEMS, NUMBER_OF_ITEMS);
            }

            //////////////////////////////////////////////////
            int numOfEntries = NUMBER_OF_ITEMS * NUMBER_OF_ITEMS;
            int numOfOnes = 0;
            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < NUMBER_OF_ITEMS; j++) {
                    numOfOnes += stackingConstraintMatrix[i][j];
                }
            }
            System.out.println("ONE-PERCENTAGE: " + ((float)numOfOnes / (float)numOfEntries) * 100);
            avgPercentage += ((float)numOfOnes / (float)numOfEntries) * 100;
            //////////////////////////////////////////////////

            int[][] costs = new int[NUMBER_OF_ITEMS][numOfStacks];
            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < numOfStacks; j++) {
                    Random rand = new Random();
                    costs[i][j] = rand.nextInt(COSTS_EXCLUSIVE_UPPER_BOUND - COSTS_INCLUSIVE_LOWER_BOUND) + COSTS_INCLUSIVE_LOWER_BOUND;
                }
            }

            int[][] stackConstraintMatrix = new int[NUMBER_OF_ITEMS][numOfStacks];
            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < numOfStacks; j++) {
                    if (Math.random() < CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS) {
                        stackConstraintMatrix[i][j] = 1;
                    } else {
                        stackConstraintMatrix[i][j] = 0;
                    }
                }
            }

            String idxString = idx < 10 ? "0" + idx : String.valueOf(idx);
            String instanceName = "slp_instance_" + NUMBER_OF_ITEMS + "_" + numOfStacks + "_" + STACK_CAPACITY + "_" + idxString;

            Instance instance = new Instance(NUMBER_OF_ITEMS, numOfStacks, STACK_CAPACITY, stackingConstraintMatrix, stackConstraintMatrix, costs, instanceName);
            InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);

            InstanceWriter.writeConfig(
                    INSTANCE_PREFIX + "instance_set_config.csv",
                    NUMBER_OF_INSTANCES,
                    NUMBER_OF_ITEMS,
                    STACK_CAPACITY,
                    ADDITIONAL_STACK_PERCENTAGE,
                    CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS,
                    CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS,
                    COSTS_INCLUSIVE_LOWER_BOUND,
                    COSTS_EXCLUSIVE_UPPER_BOUND,
                    USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE,
                    ITEM_LENGTH_LB,
                    ITEM_LENGTH_UB,
                    ITEM_WIDTH_LB,
                    ITEM_WIDTH_UB
            );
        }

        System.out.println("AVG PERCENTAGE: " + avgPercentage / NUMBER_OF_INSTANCES);
    }

    // the transitivity should be induced by the generation already
    public static int[][] generateStackingConstraintMatrixApproachTwo(int dimOne, int dimTwo) {

        // generate n items with a random length and width from a range

        ArrayList<Item> items = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            int length = HeuristicUtil.getRandomValueInBetween(ITEM_LENGTH_LB, ITEM_LENGTH_UB);
            int width = HeuristicUtil.getRandomValueInBetween(ITEM_WIDTH_LB, ITEM_WIDTH_UB);
            Item item = new Item(i, length, width);
            items.add(item);
        }

        // both values <= other item's values --> 1 in matrix

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                }

                if (items.get(i).getLength() == items.get(j).getLength()
                    && items.get(i).getWidth() == items.get(j).getWidth()) {
                        matrix[i][j] = 1;
                        matrix[j][i] = 1;
                } else if (items.get(i).getLength() < items.get(j).getLength()
                    && items.get(i).getWidth() < items.get(j).getWidth()) {
                        matrix[i][j] = 1;
                } else {
                    matrix[i][j] = 0;
                }
            }
        }
//        if (transitiveStackingConstraints) {
//            matrix = makeMatrixTransitive(matrix);
//        }
        return matrix;
    }

    /**
     * Generates the stacking constraint matrix with "random" 0/1 entries based on a given chance.
     *
     * @param dimOne - the first dimension of the matrix
     * @param dimTwo - the second dimension of the matrix
     * @param transitiveStackingConstraints - specifies whether or not the stacking constraints should be transitive
     * @return the generated matrix
     */
    public static int[][] generateStackingConstraintMatrixApproachOne(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

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

    /**
     * Adds the one-entries in the stacking constraint matrix that follow from transitivity.
     *
     * @param matrix - the matrix to be extended
     * @return the extended matrix
     */
    public static int[][] makeMatrixTransitive(int[][] matrix) {

        // The ones that are added can then induce new ones in other rows,
        // therefore the process has to be repeated as long as there are changes.

        boolean oneEntryAdded = true;

        while (oneEntryAdded) {
            oneEntryAdded = false;
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (i != j && matrix[i][j] == 0) {
                        for (int k = 0; k < matrix[0].length; k++) {
                            if (matrix[i][k] == 1 && i != k && j != k) {
                                if (matrix[k][j] == 1) {
                                    matrix[i][j] = 1;
                                    oneEntryAdded = true;
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
