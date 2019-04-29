package SP.experiments;

import SP.representations.Position;
import SP.representations.Instance;
import SP.io.InstanceWriter;
import SP.representations.Item;
import SP.util.HeuristicUtil;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.Random;

/**
 * Provides functionalites to configure and generate test instances for stacking problems.
 *
 * @author Tim Bohne
 */
public class TestDataGenerator {

    public static final String INSTANCE_PREFIX = "res/instances/";

    /******************************* CONFIGURATION *************************************/
    public static final int NUMBER_OF_INSTANCES = 1;
    public static final int NUMBER_OF_ITEMS = 500;
    public static final int STACK_CAPACITY = 3;

    // The number of stacks m is initially m = n / b,
    // this number specifies the percentage by which the initial m gets increased.
    public static final int ADDITIONAL_STACK_PERCENTAGE = 20;

    public static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS = 0.0055F;
    public static final float CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS = 0.7F;

    // Configuration of 2nd approach to stacking constraint generation:
    public static final boolean USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE = false;

    public static final float ITEM_LENGTH_LB = 1.0F;
    public static final float ITEM_LENGTH_UB = 6.0F;
    public static final float ITEM_WIDTH_LB = 1.0F;
    public static final float ITEM_WIDTH_UB = 2.4F;

    public static final float STORAGE_AREA_SLOT_LENGTH = 2.0F;
    public static final float STORAGE_AREA_SLOT_WIDTH = 1.0F;

//    public static final float STORAGE_AREA_SLOT_LENGTH = 6.06F;
//    public static final float STORAGE_AREA_SLOT_WIDTH = 2.44F;
    public static final float STORAGE_AREA_TRUCK_DISTANCE_FACTOR = 5.0F;

    public static final int NUMBER_OF_ROWS_IN_STORAGE_AREA = 2;

    /***********************************************************************************/

    /**
     * Computes the manhattan distance between the specified item and stack.
     *
     * @param item           - the item used in the dist computation
     * @param stack          - the stack used in the dist computation
     * @param stackPositions - list of stack positions
     * @return manhattan distance between original item position and stack
     */
    public static double computeManhattanDist(int item, int stack, Item[] items, ArrayList<Position> stackPositions) {
        Position itemPosition = items[item].getPosition();
        Position stackPosition = stackPositions.get(stack);
        return Math.abs(itemPosition.getXCoord() - stackPosition.getXCoord()) + Math.abs(itemPosition.getYCoord() - stackPosition.getYCoord());
    }

    /**
     * Generates the positions of the fixed stacks in the storage area in a square.
     *
     * @param numOfStacks - the number of stacks available in the instances
     * @return list of stack positions
     */
    public static ArrayList<Position> generateStackPositionsSquare(int numOfStacks) {
        ArrayList<Position> stackPositions = new ArrayList<>();
        int stacksPerRow = (int)Math.ceil(Math.sqrt(numOfStacks));
        float xCoord = 0;
        float yCoord = 0;

        for (int i = 0; i < numOfStacks; i++) {
            if (i != 0 && i % stacksPerRow == 0) {
                yCoord += STORAGE_AREA_SLOT_WIDTH;
                xCoord = 0;
            }
            stackPositions.add(new Position(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
        }
        return stackPositions;
    }

    /**
     * Generates the positions of the fixed stacks in the storage area
     * on the specified number of rows.
     *
     * @param numOfStacks - the number of stacks available in the instances
     * @return list of stack positions
     */
    public static ArrayList<Position> generateStackPositionsOnSpecifiedRows(int numOfStacks) {
        ArrayList<Position> stackPositions = new ArrayList<>();

        float xCoord = 0;
        float yCoord = 0;
        int perRow = numOfStacks / NUMBER_OF_ROWS_IN_STORAGE_AREA;
        int remainders = numOfStacks % NUMBER_OF_ROWS_IN_STORAGE_AREA;
        boolean remainderAttached = false;
        int rowCnt = 0;

        for (int i = 0; i < numOfStacks; i++) {
            if (rowCnt == perRow || remainderAttached) {
                if (remainders > 0 && !remainderAttached) {
                    remainderAttached = true;
                    remainders--;
                } else {
                    xCoord = 0;
                    yCoord += STORAGE_AREA_SLOT_WIDTH;
                    remainderAttached = false;
                    rowCnt = 0;
                }
            }
            stackPositions.add(new Position(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
            rowCnt++;
        }
        return stackPositions;
    }

    /**
     * Generates the original positions of the items on the trucks.
     * In practical settings, there are often two tracks for arriving vehicles.
     * Therefore the items are positioned in two rows.
     *
     * @return list of item positions
     */
    public static ArrayList<Position> generateItemPositions(ArrayList<Position> stackPositions) {
        ArrayList<Position> itemPositions = new ArrayList<>();

        float xCoord = 0;
        float yCoord = (float)stackPositions.get(stackPositions.size() - 1).getYCoord()
            + STORAGE_AREA_SLOT_WIDTH + STORAGE_AREA_TRUCK_DISTANCE_FACTOR * STORAGE_AREA_SLOT_WIDTH;

        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            if (i == NUMBER_OF_ITEMS / 2) {
                xCoord = 0;
                yCoord += STORAGE_AREA_SLOT_WIDTH;
            }
            itemPositions.add(new Position(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
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
     * Generates n items with a random length and width from a given range.
     *
     * @return the generated items
     */
    public static Item[] generateItems(ArrayList<Position> stackPositions) {
        ArrayList<Position> itemPositions = generateItemPositions(stackPositions);
        Item[] items = new Item[NUMBER_OF_ITEMS];
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            float length = HeuristicUtil.getRandomValueInBetweenHalfSteps(ITEM_LENGTH_LB, ITEM_LENGTH_UB);
            float width = HeuristicUtil.getRandomValueInBetweenHalfSteps(ITEM_WIDTH_LB, ITEM_WIDTH_UB);
            items[i] = new Item(i, length, width, itemPositions.get(i));
        }
        return items;
    }

    /**
     * Generates the stacking constraint matrix. In this approach, an item is stackable on top of another item
     * if its length and width values are less than or equal to the length and width of the other item.
     * The length and width per item is a random value from a given range. Two items are stackable in both
     * directions if the have the same width and length. The transitivity is already given in this approach.
     *
     * @param dimOne - the first dimension of the matrix
     * @param dimTwo - the second dimension of the matrix
     * @return the generated stacking constraint matrix
     */
    public static int[][] generateStackingConstraintMatrixApproachTwo(int dimOne, int dimTwo, Item[] items) {
        int[][] stackingConstraintMatrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {

            System.out.println("###########################");
            System.out.println(items[i].getLength());
            System.out.println(items[i].getWidth());
            System.out.println("###########################");

            for (int j = 0; j < dimTwo; j++) {

                if (i == j) { stackingConstraintMatrix[i][j] = 1; }

                // If the items have the same length and width, they're stackable in both directions.
                if (items[i].getLength() == items[j].getLength()
                    && items[i].getWidth() == items[j].getWidth()) {
                        stackingConstraintMatrix[i][j] = 1;
                        stackingConstraintMatrix[j][i] = 1;
                } else if (items[i].getLength() <= items[j].getLength()
                    && items[i].getWidth() <= items[j].getWidth()) {
                        stackingConstraintMatrix[i][j] = 1;
                } else {
                    stackingConstraintMatrix[i][j] = 0;
                }
            }
        }
        return stackingConstraintMatrix;
    }

    /**
     * Generates the stacking constraint matrix with "random" 0/1 entries based on a given chance.
     *
     * @param dimOne     - the first dimension of the matrix
     * @param dimTwo     - the second dimension of the matrix
     * @param transitive - specifies whether or not the stacking constraints should be transitive
     * @return the generated stacking constraint matrix
     */
    public static int[][] generateStackingConstraintMatrixApproachOne(int dimOne, int dimTwo, boolean transitive) {
        int[][] stackingConstraintMatrix = new int[dimOne][dimTwo];
        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {
                if (i == j) {
                    stackingConstraintMatrix[i][j] = 1;
                } else {
                    if (Math.random() >= 1.0 - CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS) {
                        stackingConstraintMatrix[i][j] = 1;
                    } else {
                        stackingConstraintMatrix[i][j] = 0;
                    }
                }
            }
        }
        if (transitive) {
            stackingConstraintMatrix = makeMatrixTransitive(stackingConstraintMatrix);
        }
        return stackingConstraintMatrix;
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

    /**
     * Computes the number of stacks available in the instances.
     *
     * @return the number of available stacks
     */
    public static int computeNumberOfStacks() {
        int numOfStacks = (int)Math.ceil((float)NUMBER_OF_ITEMS / (float)STACK_CAPACITY);
        if (ADDITIONAL_STACK_PERCENTAGE > 0) {
            numOfStacks = (int)(Math.ceil(numOfStacks + numOfStacks * ((float)ADDITIONAL_STACK_PERCENTAGE / 100.0)));
        }
        return numOfStacks;
    }

    /**
     * Generates the matrix containing the transport costs for item-stack-assignments.
     * The placement constraint that forbid certain item-stack-assignments are indirectly implemented
     * via high cost entries.
     *
     * @param costs          - the matrix of costs to be filled
     * @param numOfStacks    - the number of available stacks
     * @param stackPositions - the positions of the stacks in the storage area
     */
    public static void generateCosts(double[][] costs, int numOfStacks, Item[] items, ArrayList<Position> stackPositions) {
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                costs[i][j] = computeManhattanDist(i, j, items, stackPositions);
            }
        }
        // Implements the placement constraints via high cost entries.
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                if (Math.random() >= CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS) {
                    costs[i][j] = Integer.MAX_VALUE / NUMBER_OF_ITEMS;
                }
            }
        }
    }

    public static void main(String[] args) {
        int numOfStacks = TestDataGenerator.computeNumberOfStacks();
        float avgPercentage = 0;

        for (int idx = 0; idx < NUMBER_OF_INSTANCES; idx++) {

            ArrayList<Position> stackPositions = generateStackPositionsOnSpecifiedRows(numOfStacks);
            Item[] items = generateItems(stackPositions);

            int[][] stackingConstraintMatrix;
            if (USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE) {
                stackingConstraintMatrix = TestDataGenerator.generateStackingConstraintMatrixApproachOne(NUMBER_OF_ITEMS, NUMBER_OF_ITEMS, true);
            } else {
                stackingConstraintMatrix = generateStackingConstraintMatrixApproachTwo(NUMBER_OF_ITEMS, NUMBER_OF_ITEMS, items);
            }
            avgPercentage += getPercentageOfOneEntries(stackingConstraintMatrix);

            double[][] costs = new double[NUMBER_OF_ITEMS][numOfStacks];
            TestDataGenerator.generateCosts(costs, numOfStacks, items, stackPositions);

            String idxString = idx < 10 ? "0" + idx : String.valueOf(idx);
            String instanceName = "slp_instance_" + NUMBER_OF_ITEMS + "_" + numOfStacks + "_" + STACK_CAPACITY + "_" + idxString;
            Instance instance = new Instance(
                items, numOfStacks, stackPositions,
                STACK_CAPACITY, stackingConstraintMatrix, costs, instanceName
            );
            InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);

            InstanceWriter.writeConfig(
                INSTANCE_PREFIX + "instance_set_config.csv", NUMBER_OF_INSTANCES, NUMBER_OF_ITEMS,
                STACK_CAPACITY, ADDITIONAL_STACK_PERCENTAGE, CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS,
                CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS, USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE,
                ITEM_LENGTH_LB, ITEM_LENGTH_UB, ITEM_WIDTH_LB, ITEM_WIDTH_UB
            );
        }
        System.out.println("AVG PERCENTAGE OF ONE-ENTRIES: " + avgPercentage / NUMBER_OF_INSTANCES);
    }
}
