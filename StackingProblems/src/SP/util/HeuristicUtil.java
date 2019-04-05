package SP.util;

import SP.representations.MCMEdge;

import java.io.File;
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
    public static boolean itemPairAndStackCompatible(int stackIdx, int itemOne, int itemTwo, double[][] costMatrix, int invalidEdgeCosts) {
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
    public static boolean alreadyUsedShuffle(ArrayList<Integer> currentShuffle, List<List<Integer>> alreadyUsedShuffles) {
        for (List<Integer> shuffle : alreadyUsedShuffles) {
            if (shuffle.equals(currentShuffle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an item array from the given list of items.
     *
     * @param items - the given list of items
     * @return an array of the given items
     */
    public static int[] getItemArrayFromItemList(ArrayList<Integer> items) {
        int[] itemArr = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemArr[i] = items.get(i);
        }
        return itemArr;
    }

    /**
     * Updates the list of completely filled stacks and prepares the corresponding
     * items to be removed from the remaining item pairs.
     *
     * @param itemPairRemovalList    - the list to keep track of the items that should
     *                                 be removed form the remaining item pairs
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
     * Returns the percentage deviation between the expected and the actual value.
     *
     * @param expected - the expected value
     * @param actual   - the actual value
     * @return the percentage deviation between the expected and the actual value
     */
    public static int getPercentageDeviation(float expected, float actual) {
        float diff = Math.abs(expected - actual);
        return Math.round(diff / expected * 100);
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
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                return true;
            } else if (stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1 && stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1 && stackingConstraints[item][pairItemOne] == 1) {
                return true;
            }
        // pairItemOne above pairItemTwo
        } else if (stackingConstraints[pairItemOne][pairItemTwo] == 1) {
            if (stackingConstraints[item][pairItemOne] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1 && stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            }
        // pairItemTwo above pairItemOne
        } else {
            if (stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1 && stackingConstraints[item][pairItemOne] == 1) {
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
    public static float getRandomValueInBetween(float min, float max) {
        Random r = new Random();
        return (float)(Math.round(min + r.nextFloat() * (max - min) * 10.0) / 10.0);
    }

    /**
     * Returns a random integer in between the specified limits.
     *
     * @param min - the lower bound of the value
     * @param max - the upper bound of the value
     * @return the random value in between the specified limits
     */
    public static int getRandomIntegerInBetween(int min, int max) {
        Random r = new Random();
        return min + r.nextInt() * (max - min);
    }

    /**
     * Returns the number of items that are assigned to a stack in the storage area.
     *
     * @param stacks - the stacks to be checked
     * @return the number of assigned items
     */
    public static int getNumberOfItemsAssignedToStacks(int[][] stacks) {
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

    /**
     * Returns whether the specified list of items contains duplicates.
     *
     * @param items - ths list of items to be checked
     * @return whether or not the list contains duplicates
     */
    public static boolean itemListContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){ return true; }
        return false;
    }

    /**
     * Returns a list of items corresponding to the specified array of items.
     *
     * @param itemsArr - the array of items to be replaces by a list of items
     * @return the list of items
     */
    public static ArrayList<Integer> getArrayListOfItems(int[] itemsArr) {
        ArrayList<Integer> items = new ArrayList<>();
        for (int item : itemsArr) {
            items.add(item);
        }
        return items;
    }

    public static ArrayList<MCMEdge> getCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> copy = new ArrayList<>();
        for (MCMEdge edge : edges) {
            copy.add(new MCMEdge(edge));
        }
        return copy;
    }

    /**
     * Returns a reversed copy of the specified edge list.
     *
     * @param edges - the list of edges to be reversed
     * @return the reversed edge list
     */
    public static ArrayList<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }

    /**
     * Sorts the given map by value.
     *
     * @param map - the map to be sorted
     * @return the sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Creates a string containing the names of all solutions.
     *
     * @return string containing the names of all solutions
     */
    public static String createStringContainingAllSolutionNames(String prefix) {
        File dir = new File(prefix);
        File[] dirListing = dir.listFiles();
        String str = "";
        for (File f : dirListing) {
            str += f.toString() + " ";
        }
        return str;
    }
}
