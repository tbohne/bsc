package SLP.experiments;

import SLP.representations.Coordinates;
import SLP.representations.Instance;
import SLP.representations.InstanceWriter;
import SLP.representations.Item;
import SLP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.Random;

public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    /*************************** CONFIGURATION *********************************/
    public static final int NUMBER_OF_INSTANCES = 20;
    public static final int NUMBER_OF_ITEMS = 500;
    public static final int STACK_CAPACITY = 2;

    // The number of stacks m is initially m = n / b,
    // this number specifies the percentage by which the initial m gets increased.
    public static final int ADDITIONAL_STACK_PERCENTAGE = 20;

    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS = 0.0055F;
    public static final float CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS = 0.7F;

    // Configuration of 2nd approach to stacking constraint generation:
    public static final boolean USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE = false;
    public static final int ITEM_LENGTH_LB = 1;
    public static final int ITEM_LENGTH_UB = 30;
    public static final int ITEM_WIDTH_LB = 1;
    public static final int ITEM_WIDTH_UB = 30;

    public static final int STORAGE_AREA_SLOT_LENGTH = 1;
    public static final int STORAGE_AREA_SLOT_WIDTH = 1;
    public static final int STORAGE_AREA_TRUCK_DISTANCE_FACTOR = 5;
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

            avgPercentage += getPercentageOfOneEntries(stackingConstraintMatrix);

            ArrayList<Coordinates> itemPositions = generateItemPositions();
            ArrayList<Coordinates> stackPositions = generateStackPositions(numOfStacks);

            int[][] costs = new int[NUMBER_OF_ITEMS][numOfStacks];
            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < numOfStacks; j++) {
                    costs[i][j] = computeManhattanDist(i, j, itemPositions, stackPositions);
                }
            }

            for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
                for (int j = 0; j < numOfStacks; j++) {
                    if (Math.random() >= CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS) {
                        costs[i][j] = Integer.MAX_VALUE / NUMBER_OF_ITEMS;
                    }
                }
            }

            String idxString = idx < 10 ? "0" + idx : String.valueOf(idx);
            String instanceName = "slp_instance_" + NUMBER_OF_ITEMS + "_" + numOfStacks + "_" + STACK_CAPACITY + "_" + idxString;
            Instance instance = new Instance(
                NUMBER_OF_ITEMS,
                numOfStacks,
                itemPositions,
                stackPositions,
                STACK_CAPACITY,
                stackingConstraintMatrix,
                costs,
                instanceName
            );
            InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);

            InstanceWriter.writeConfig(
                INSTANCE_PREFIX + "instance_set_config.csv",
                NUMBER_OF_INSTANCES,
                NUMBER_OF_ITEMS,
                STACK_CAPACITY,
                ADDITIONAL_STACK_PERCENTAGE,
                CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS,
                CHANCE_FOR_ONE_IN_STACK_CONSTRAINTS,
                USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE,
                ITEM_LENGTH_LB,
                ITEM_LENGTH_UB,
                ITEM_WIDTH_LB,
                ITEM_WIDTH_UB
            );
        }
        System.out.println("AVG PERCENTAGE OF ONE-ENTRIES: " + avgPercentage / NUMBER_OF_INSTANCES);
    }

    /**
     * Computes the manhattan distance between the specified item and stack.
     *
     * @param item - the item used in the dist computation
     * @param stack - the stack used in the dist computation
     * @param itemPositions - list of item positions
     * @param stackPositions - list of stack positions
     * @return manhattan distance of item and stack
     */
    public static int computeManhattanDist(int item, int stack, ArrayList<Coordinates> itemPositions, ArrayList<Coordinates> stackPositions) {
        Coordinates itemPosition = itemPositions.get(item);
        Coordinates stackPosition = stackPositions.get(stack);
        return Math.abs(itemPosition.getXCoord() - stackPosition.getXCoord()) + Math.abs(itemPosition.getYCoord() - stackPosition.getYCoord());
    }

    /**
     * Generates the positions of the fixed stacks in the storage area.
     *
     * @param numOfStacks - the number of stacks available in the instances
     * @return list of stack positions
     */
    public static ArrayList<Coordinates> generateStackPositions(int numOfStacks) {

        ArrayList<Coordinates> stackPositions = new ArrayList<>();

        for (int i = 0; i < numOfStacks; i++) {
            stackPositions.add(new Coordinates(i * STORAGE_AREA_SLOT_LENGTH, 0));
        }
        return stackPositions;
    }

    /**
     * Generates the original positions of the items on the truck.
     *
     * @return list of item positions
     */
    public static ArrayList<Coordinates> generateItemPositions() {

        ArrayList<Coordinates> itemPositions = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            itemPositions.add(new Coordinates(
                i * STORAGE_AREA_SLOT_LENGTH,
                STORAGE_AREA_SLOT_WIDTH + STORAGE_AREA_TRUCK_DISTANCE_FACTOR * STORAGE_AREA_SLOT_WIDTH
            ));
        }
        return itemPositions;
    }

    /**
     * Returns the percentage of one-entries.
     *
     * @param stackingConstraintMatrix - the matrix to be considered
     * @return the percentage of one-entries
     */
    public static float getPercentageOfOneEntries(int[][] stackingConstraintMatrix) {
        int numOfEntries = NUMBER_OF_ITEMS * NUMBER_OF_ITEMS;
        int numOfOnes = 0;
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < NUMBER_OF_ITEMS; j++) {
                numOfOnes += stackingConstraintMatrix[i][j];
            }
        }
        return ((float)numOfOnes / (float)numOfEntries) * 100;
    }

    /**
     * Generates the stacking constraint matrix. In this approach, an item is stackable on top of another item
     * if its length and width values are less than or equal to the length and width of the other item.
     * The length and width per item is a random value from a given range. Two items are stackable in both
     * directions if they share the same values.
     *
     * The transitivity is already given in this approach.
     *
     * @param dimOne - the first dimension of the matrix
     * @param dimTwo - the second dimension of the matrix
     * @return the generated matrix
     */
    public static int[][] generateStackingConstraintMatrixApproachTwo(int dimOne, int dimTwo) {

        // Generates n items with a random length and width from a given range.
        ArrayList<Item> items = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            int length = HeuristicUtil.getRandomValueInBetween(ITEM_LENGTH_LB, ITEM_LENGTH_UB);
            int width = HeuristicUtil.getRandomValueInBetween(ITEM_WIDTH_LB, ITEM_WIDTH_UB);
            Item item = new Item(i, length, width);
            items.add(item);
        }

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) { matrix[i][j] = 1; }

                // If the items share the same values, they're stackable in both directions.
                if (items.get(i).getLength() == items.get(j).getLength()
                    && items.get(i).getWidth() == items.get(j).getWidth()) {
                        matrix[i][j] = 1;
                        matrix[j][i] = 1;
                } else if (items.get(i).getLength() <= items.get(j).getLength()
                    && items.get(i).getWidth() <= items.get(j).getWidth()) {
                        matrix[i][j] = 1;
                } else {
                    matrix[i][j] = 0;
                }
            }
        }
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
