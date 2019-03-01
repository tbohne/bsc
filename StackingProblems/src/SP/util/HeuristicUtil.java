package SP.util;

import SP.representations.MCMEdge;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;

import java.util.*;

/**
 * A collection of general utility methods used in the heuristics.
 *
 * @author Tim Bohne
 */
public class HeuristicUtil {

    /**
     * Returns a list containing all the matched items that are part of an item pair.
     *
     * @param itemPairs - the pairs of items
     * @return a list containing all the matched items
     */
    public static ArrayList<Integer> getMatchedItemsFromPairs(ArrayList<MCMEdge> itemPairs) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge pair : itemPairs) {
            matchedItems.add(pair.getVertexOne());
            matchedItems.add(pair.getVertexTwo());
        }
        return matchedItems;
    }

    /**
     * Returns a list containing all the matched items that are part of an item triple.
     *
     * @param itemTriples - the triples of items
     * @return a list containing all the matched items
     */
    public static ArrayList<Integer> getMatchedItemsFromTriples(ArrayList<ArrayList<Integer>> itemTriples) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (ArrayList<Integer> triple : itemTriples) {
            for (int item : triple) {
                matchedItems.add(item);
            }
        }
        return matchedItems;
    }

    /**
     * Returns the list of unmatched items based on the matched items from the triples and pairs.
     * The matched items and the list containing every item are used to determine the unmatched items.
     *
     * @param itemTriples - the list of item triples
     * @param itemPairs   - the list of item pairs
     * @param items       - the list of all items
     * @return a list containing the unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsFromTriplesAndPairs(
        ArrayList<ArrayList<Integer>> itemTriples,
        ArrayList<MCMEdge> itemPairs,
        int[] items
    ) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        matchedItems.addAll(getMatchedItemsFromTriples(itemTriples));
        matchedItems.addAll(getMatchedItemsFromPairs(itemPairs));
        return getUnmatchedItemsFromMatchedItems(matchedItems, items);
    }

    /**
     * Returns a list of unmatched items based on the list of matched triples.
     *
     * @param itemTriples - the list of item triples
     * @return a list containing the unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsFromTriples(ArrayList<ArrayList<Integer>> itemTriples, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromTriples(itemTriples), items);
    }

    /**
     * Returns the list of unmatched items based on the given list of item pairs.
     *
     * @param itemPairs - the list of matched items
     * @param items     - the list containing every item
     * @return the list of unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsFromPairs(ArrayList<MCMEdge> itemPairs, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromPairs(itemPairs), items);
    }

    /**
     * Returns the list of unmatched items based on the list of matched items.
     *
     * @param matchedItems - the list of matched items
     * @param items        - the list containing every item
     * @return a list containing the unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsFromMatchedItems(ArrayList<Integer> matchedItems, int[] items) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : items) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    /**
     * Returns whether the specified stack in the storage area is empty.
     *
     * @param stackIdx    - the index of the stack to be checked
     * @param storageArea - the storage area containing the stacks
     * @return whether or not the specified stack is empty
     */
    public static boolean stackEmpty(int stackIdx, int[][] storageArea) {
        return storageArea[stackIdx][2] == -1 && storageArea[stackIdx][1] == -1 && storageArea[stackIdx][0] == -1;
    }

    /**
     * Returns whether the specified pair of items is compatible with the specified stack
     * which means that both items are feasibly assignable to the stack without contradicting
     * the placement constraints.
     *
     * @param stackIdx         - the index of the stack to be checked
     * @param itemOne          - the first item of the pair to be checked
     * @param itemTwo          - the second item of the pair to be checked
     * @param costMatrix       - the matrix containing the edge costs
     * @param invalidEdgeCosts - the cost value used to implement the placement constraints
     * @return
     */
    public static boolean itemPairAndStackCompatible(int stackIdx, int itemOne, int itemTwo, int[][] costMatrix, int invalidEdgeCosts) {
        return costMatrix[itemOne][stackIdx] < invalidEdgeCosts && costMatrix[itemTwo][stackIdx] < invalidEdgeCosts;
    }

    /**
     * Returns whether the specified items are stackable in both directions.
     *
     * @param itemOne             - the first item to be checked
     * @param itemTwo             - the second item to be checked
     * @param stackingConstraints - the stacking constraints to be respected
     * @return whether or not the given items are stackable in both directions
     */
    public static boolean itemsStackableInBothDirections(int itemOne, int itemTwo, int[][] stackingConstraints) {
        return stackingConstraints[itemTwo][itemOne] == 1 && stackingConstraints[itemOne][itemTwo] == 1;
    }

    /**
     * Determines whether the specified current shuffle was already used before.
     *
     * @param currentShuffle      - the current shuffle to be checked
     * @param alreadyUsedShuffles - the list of already used shuffles
     * @return whether or not the current shuffle was already used
     */
    public static boolean isAlreadyUsedShuffle(ArrayList<Integer> currentShuffle, List<List<Integer>> alreadyUsedShuffles) {
        for (List<Integer> shuffle : alreadyUsedShuffles) {
            if (shuffle.equals(currentShuffle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an integer array from the given list of integers.
     *
     * @param list - the given list of integers
     * @return an array of the given integers
     */
    public static int[] getArrayFromList(ArrayList<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * Updates the list of completely filled stacks and prepares the corresponding
     * items to be removed from the remaining item pairs.
     *
     * @param itemPairRemovalList    - the list to keep track of the items that should be removed form the remaining item pairs
     * @param itemOneEdge            - the edge (item pair), the first item is assigned to
     * @param itemTwoEdge            - the edge (item pair), the second item is assigned to
     * @param startingPair           - the pair that is going to be assigned
     * @param itemOne                - the first item to be assigned
     * @param itemTwo                - the second item to be assigned
     * @param completelyFilledStacks - the list of completely filled stacks
     */
    public static void updateCompletelyFilledStacks(
        ArrayList<MCMEdge> itemPairRemovalList,
        MCMEdge itemOneEdge,
        MCMEdge itemTwoEdge,
        MCMEdge startingPair,
        int itemOne,
        int itemTwo,
        ArrayList<ArrayList<Integer>> completelyFilledStacks
    ) {
        itemPairRemovalList.add(itemOneEdge);
        itemPairRemovalList.add(itemTwoEdge);
        itemPairRemovalList.add(startingPair);

        ArrayList<Integer> itemOneStack = new ArrayList<>();
        itemOneStack.add(itemOne);
        itemOneStack.add(itemOneEdge.getVertexOne());
        itemOneStack.add(itemOneEdge.getVertexTwo());

        ArrayList<Integer> itemTwoStack = new ArrayList<>();
        itemTwoStack.add(itemTwo);
        itemTwoStack.add(itemTwoEdge.getVertexOne());
        itemTwoStack.add(itemTwoEdge.getVertexTwo());

        completelyFilledStacks.add(itemOneStack);
        completelyFilledStacks.add(itemTwoStack);
    }

    /**
     * Generates completely filled stacks from the list of item pairs.
     * Breaks up a pair and tries to assign both items two new pairs to build up completely filled stacks.
     *
     * @param itemPairs              - the list of item pairs
     * @param itemPairRemovalList    - the list of edges (item pairs) to be removed
     * @param completelyFilledStacks - list to store the completely filled stacks
     */
    public static void generateCompletelyFilledStacks(
        ArrayList<MCMEdge> itemPairs,
        ArrayList<MCMEdge> itemPairRemovalList,
        ArrayList<ArrayList<Integer>> completelyFilledStacks,
        int[][] stackingConstraints
    ) {
        for (MCMEdge startingPair : itemPairs) {
            if (itemPairRemovalList.contains(startingPair)) { continue; }

            int itemOne = startingPair.getVertexOne();
            int itemTwo = startingPair.getVertexTwo();
            boolean itemOneAssigned = false;
            boolean itemTwoAssigned = false;
            MCMEdge itemOneEdge = new MCMEdge(0, 0, 0);
            MCMEdge itemTwoEdge = new MCMEdge(0, 0, 0);

            for (MCMEdge potentialTargetPair : itemPairs) {
                if (itemPairRemovalList.contains(potentialTargetPair)) { continue; }
                if (itemOneAssigned && itemTwoAssigned) { break; }

                if (startingPair != potentialTargetPair) {
                    int potentialTargetPairItemOne = potentialTargetPair.getVertexOne();
                    int potentialTargetPairItemTwo = potentialTargetPair.getVertexTwo();

                    if (!itemOneAssigned && HeuristicUtil.itemAssignableToPair(
                            itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)) {
                        itemOneAssigned = true;
                        itemOneEdge = potentialTargetPair;
                        continue;
                    }
                    if (!itemTwoAssigned && HeuristicUtil.itemAssignableToPair(
                            itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)) {
                        itemTwoAssigned = true;
                        itemTwoEdge = potentialTargetPair;
                        continue;
                    }
                }
            }
            if (itemOneAssigned && itemTwoAssigned) {
                updateCompletelyFilledStacks(
                    itemPairRemovalList, itemOneEdge, itemTwoEdge, startingPair,
                    itemOne, itemTwo, completelyFilledStacks
                );
            }
        }
    }

    /**
     * Returns whether the given item is validly assignable to the given pair.
     *
     * @param item        - the item to be checked
     * @param pairItemOne - the first item of the pair
     * @param pairItemTwo - the second item of the pair
     * @return whether or not the item is assignable to the pair
     */
    public static boolean itemAssignableToPair(int item, int pairItemOne, int pairItemTwo, int[][] stackingConstraints) {
        // pair stackable in both directions
        if (HeuristicUtil.itemsStackableInBothDirections(pairItemOne, pairItemTwo, stackingConstraints)) {
            if (stackingConstraints[item][pairItemOne] == 1) {
                // itemOne above itemOneNew above itemTwoNew --> itemOne assigned
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                // itemTwoNew above itemOneNew above itemOne --> itemOne assigned
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                // itemOneNew above itemTwoNew above itemOne --> itemOne assigned
                return true;
            } else if (stackingConstraints[item][pairItemTwo] == 1) {
                // itemOne above itemTwoNew above itemOneNew --> itemOne assigned
                return true;
            }
            // pairItemOne above pairItemTwo
        } else if (stackingConstraints[pairItemOne][pairItemTwo] == 1) {
            if (stackingConstraints[item][pairItemOne] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                return true;
            }
            // pairItemTwo above pairItemOne
        } else {
            if (stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a random value in between the specified limits.
     *
     * @param min - the lower bound of the value
     * @param max - the upper bound of the value
     * @return the random value in between the specified limits
     */
    public static int getRandomValueInBetween(int min, int max) {
        Random r = new Random();
        return r.nextInt(max - min) + min;
    }

    /**
     * Returns the number of items that are assigned to a stack in the storage area.
     *
     * @param stacks - the stacks to be checked
     * @return the number of assigned items
     */
    public static int getNumberOfItemsInStacks(int[][] stacks) {
        int numberOfItems = 0;
        for (int i = 0; i < stacks.length; i++) {
            for (int j = 0; j < stacks[i].length; j++) {
                if (stacks[i][j] != -1) {
                    numberOfItems++;
                }
            }
        }
        return numberOfItems;
    }

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /************************************ DEPRECATED FROM HERE ON *****************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    public static boolean listContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){
            return true;
        }
        return false;
    }

    public static void copyStackAssignment(int[][] init, int[][] original, int[][] copy) {
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                copy[i][j] = init[i][j];
            }
        }
    }

    public static ArrayList<Integer> getArrayListOfItems(int[] itemsArr) {
        ArrayList<Integer> items = new ArrayList<>();
        for (int item : itemsArr) {
            items.add(item);
        }
        return items;
    }

    public static ArrayList<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }

    public static HashMap parseItemPairStackCombination(EdmondsMaximumCardinalityMatching mcm) {

        HashMap<Integer, ArrayList<Integer>> itemPairStackCombination = new HashMap<>();

        for (Object edge : mcm.getMatching().getEdges()) {
            int firstItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[0].trim());
            int secondItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[0].replace(")", "").trim());
            int stack = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[1].replace("stack", "").replace(")", "").trim());

            ArrayList<Integer> items = new ArrayList<>();
            items.add(firstItem);
            items.add(secondItem);
            itemPairStackCombination.put(stack, items);
        }

        return itemPairStackCombination;
    }

    /**
     * Applies each edge rating system to a copy of the item pair list.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public static void applyRatingSystemsToItemPairPermutations(ArrayList<ArrayList<MCMEdge>> itemPairPermutations, int[][] stackingConstraints) {
        int idx = 0;
        RatingSystem.assignRowRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignColRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignMaxRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignMinRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignSumRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
    }

    // TODO: exchange certain elements of this sequence with other (unused) ones (EXPERIMENTAL APPROACH)
    // IDEA:
    // - choose a number n (20%) of random elements to be replaced
    // - choose the next n unused elements from the ordered list
    // - exchange the elements
    public static ArrayList<MCMEdge> edgeExchange(List<MCMEdge> edges, int[][] stacks) {

        ArrayList tmpEdges = new ArrayList(edges);

        int numberOfEdgesToBeReplaced = (int) (0.3 * stacks.length);
        if (numberOfEdgesToBeReplaced > (edges.size() - stacks.length)) {
            numberOfEdgesToBeReplaced = edges.size() - stacks.length;
        }

        ArrayList<Integer> toBeReplaced = new ArrayList<>();

        for (int i = 0; i < numberOfEdgesToBeReplaced; i++) {
            toBeReplaced.add(HeuristicUtil.getRandomValueInBetween(0, stacks.length - 1));
        }
        for (int i = 0; i < toBeReplaced.size(); i++) {
            Collections.swap(tmpEdges, toBeReplaced.get(i), i + stacks.length);
        }

        return new ArrayList(tmpEdges);
    }
}
