package SP.util;

import SP.representations.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

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
     * @param itemPairs - pairs of items
     * @return matched items from pairs
     */
    public static List<Integer> getMatchedItemsFromPairs(List<MCMEdge> itemPairs) {
        List<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge pair : itemPairs) {
            matchedItems.add(pair.getVertexOne());
            matchedItems.add(pair.getVertexTwo());
        }
        return matchedItems;
    }

    /**
     * Returns a list containing all the matched items that are part of an item triple.
     *
     * @param itemTriples - triples of items
     * @return matched items from triples
     */
    public static List<Integer> getMatchedItemsFromTriples(List<List<Integer>> itemTriples) {
        List<Integer> matchedItems = new ArrayList<>();
        for (List<Integer> triple : itemTriples) {
            for (int item : triple) {
                matchedItems.add(item);
            }
        }
        return matchedItems;
    }

    /**
     * Returns the list of unmatched items based on the list of matched items.
     *
     * @param matchedItems - list of matched items
     * @param items        - list containing every item
     * @return list of unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromMatchedItems(List<Integer> matchedItems, int[] items) {
        List<Integer> unmatchedItems = new ArrayList<>();
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
     * @param itemTriples - list of item triples
     * @param itemPairs   - list of item pairs
     * @param items       - list of all items
     * @return list containing the unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromTriplesAndPairs(
        List<List<Integer>> itemTriples, List<MCMEdge> itemPairs, int[] items
    ) {
        List<Integer> matchedItems = new ArrayList<>();
        matchedItems.addAll(getMatchedItemsFromTriples(itemTriples));
        matchedItems.addAll(getMatchedItemsFromPairs(itemPairs));
        return getUnmatchedItemsFromMatchedItems(matchedItems, items);
    }

    /**
     * Returns a list of unmatched items based on the list of matched triples.
     *
     * @param itemTriples - list of item triples
     * @return list containing the unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromTriples(List<List<Integer>> itemTriples, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromTriples(itemTriples), items);
    }

    /**
     * Returns the list of unmatched items based on the given list of item pairs.
     *
     * @param itemPairs - list of matched items
     * @param items     - list containing every item
     * @return list of unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromPairs(List<MCMEdge> itemPairs, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromPairs(itemPairs), items);
    }

    /**
     * Prepares the update of stack assignments in the post processing step by creating lists of items that
     * are assigned to the same stack and storing the savings for each item.
     *
     * @param maxSavingsMatching  - matching to be processed
     * @param itemSavings         - map to be filled with each item's saving
     * @param postProcessingGraph - post-processing graph containing the items and empty positions
     * @param stackAssignments    - map to be filled with the stacks and the items assigned to each stack
     */
    public static void prepareUpdateOfStackAssignmentsInPostProcessing(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching, Map<Integer, Double> itemSavings,
        BipartiteGraph postProcessingGraph, Map<Integer, List<Integer>> stackAssignments
    ) {
        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int item = GraphUtil.parseItemFromPostProcessingMatching(edge);
            int stack = GraphUtil.parseStackFromPostProcessingMatching(edge);
            itemSavings.put(item, postProcessingGraph.getGraph().getEdgeWeight(edge));
            if (stackAssignments.containsKey(stack)) {
                stackAssignments.get(stack).add(item);
            } else {
                stackAssignments.put(stack, new ArrayList<>());
            }
        }
    }

    /**
     * Updates the item position in the storage area in the post processing step and adds
     * the used stack to the list of blocked stacks.
     *
     * @param item          - item to be assigned to new position
     * @param stack         - stack the item gets assigned to
     * @param level         - level the item gets assigned to
     * @param blockedStacks - list of blocked stacks (only one item per stack in post-processing)
     */
    public static void updateItemPositionInPostProcessing(int item, int stack, int level, List<Integer> blockedStacks, Instance instance) {
        HeuristicUtil.removeItemFromOutdatedPosition(item, instance.getStacks());
        instance.getStacks()[stack][level] = item;
        blockedStacks.add(stack);
    }

    /**
     * Updates the stack assignments in the post processing step with the specified matching.
     * Only one item can be assigned to each stack, because otherwise incompatibilities in terms
     * of the stacking constraints could arise. Therefore each stack is blocked for additional assignments
     * after an item has been assigned to it.
     *
     * @param maxSavingsMatching - matching the stack assignments are updated with
     */
    public static void updateStackAssignmentsInPostProcessing(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching,
        BipartiteGraph postProcessingGraph, Instance instance
    ) {
        List<Integer> blockedStacks = new ArrayList<>();
        Map<Integer, List<Integer>> stackAssignments = new HashMap<>();
        Map<Integer, Double> itemSavings = new HashMap<>();

        HeuristicUtil.prepareUpdateOfStackAssignmentsInPostProcessing(maxSavingsMatching, itemSavings, postProcessingGraph, stackAssignments);

        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int item = GraphUtil.parseItemFromPostProcessingMatching(edge);
            int stack = GraphUtil.parseStackFromPostProcessingMatching(edge);
            int level = GraphUtil.parseLevelFromPostProcessingMatching(edge);

            if (blockedStacks.contains(stack)) { continue; }

            if (stackAssignments.get(stack).size() > 1) {
                int bestItem = stackAssignments.get(stack).get(0);
                for (int i = 1; i < stackAssignments.get(stack).size(); i++) {
                    if (itemSavings.get(stackAssignments.get(stack).get(i)) > itemSavings.get(bestItem)) {
                        bestItem = stackAssignments.get(stack).get(i);
                    }
                }
                HeuristicUtil.updateItemPositionInPostProcessing(bestItem, stack, level, blockedStacks, instance);
            } else {
                HeuristicUtil.updateItemPositionInPostProcessing(item, stack, level, blockedStacks, instance);
            }
        }
    }

    /**
     * Computes the average for the specified list of integers.
     *
     * @param values - list of integer values to compute the average for
     * @return average value
     */
    public static int getAvg(List<Integer> values) {
        int avg = 0;
        for (int val : values) {
            avg += val;
        }
        return avg / values.size();
    }

    /**
     * Checks whether the specified item can be assigned to the specified pair
     * that is stackable in both directions in a way that respects the stacking constraints.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param lowerItemOfPair     - lower item of the pair
     * @param upperItemOfPair     - upper item of the pair
     * @param item                - item to be checked for compatibility with the pair
     * @return whether or not the item is compatible with the pair
     */
    public static boolean itemAssignableToPairStackableInBothDirections(
        int[][] stackingConstraints, int lowerItemOfPair, int upperItemOfPair, int item
    ) {
        return stackingConstraints[lowerItemOfPair][item] == 1
            || stackingConstraints[item][upperItemOfPair] == 1
            || (stackingConstraints[upperItemOfPair][item] == 1 && stackingConstraints[item][lowerItemOfPair] == 1)
            || stackingConstraints[upperItemOfPair][item] == 1
            || (stackingConstraints[lowerItemOfPair][item] == 1 && stackingConstraints[item][upperItemOfPair] == 1)
            || stackingConstraints[item][lowerItemOfPair] == 1;
    }

    /**
     * Checks whether the specified item can be assigned to the specified pair
     * that is stackable in only one direction in a way that respects the stacking constraints.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param lowerItemOfPair     - lower item of the pair
     * @param upperItemOfPair     - upper item of the pair
     * @param item                - item to be checked for compatibility with the pair
     * @return whether or not the item is compatible with the pair
     */
    public static boolean itemAssignableToPairStackableInOneDirection(
        int[][] stackingConstraints, int lowerItemOfPair, int upperItemOfPair, int item
    ) {
        return stackingConstraints[lowerItemOfPair][item] == 1
            || stackingConstraints[item][upperItemOfPair] == 1
            || (stackingConstraints[upperItemOfPair][item] == 1 && stackingConstraints[item][lowerItemOfPair] == 1);
    }

    /**
     * Checks whether the specified items are stackable in at least one direction.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param itemOne             - first item of the pair to be checked
     * @param itemTwo             - second item of the pair to be checked
     * @return whether or not the pair of items is stackable
     */
    public static boolean pairStackableInAtLeastOneDirection(int[][] stackingConstraints, int itemOne, int itemTwo) {
        return stackingConstraints[itemOne][itemTwo] == 1 || stackingConstraints[itemTwo][itemOne] == 1;
    }

    /**
     * Checks whether the specified items are stackable in both directions.
     *
     * @param itemOne             - first item of the pair to be checked
     * @param itemTwo             - second item of the pair to be checked
     * @param stackingConstraints - stacking constraints to be respected
     * @return whether or not the given items are stackable in both directions
     */
    public static boolean pairStackableInBothDirections(int itemOne, int itemTwo, int[][] stackingConstraints) {
        return stackingConstraints[itemTwo][itemOne] == 1 && stackingConstraints[itemOne][itemTwo] == 1;
    }

    /**
     * Checks whether the specified item is compatible with the specified stack
     * in terms of the placement constraints.
     *
     * @param costs - matrix of transport costs indirectly encoding placement constraints
     * @param item  - item to check compatibility for
     * @param stack - stack to check compatibility for
     * @return whether or not the item is compatible with the stack
     */
    public static boolean itemCompatibleWithStack(double[][] costs, int item, int stack) {
        return costs[item][stack] < Integer.MAX_VALUE / costs.length;
    }

    /**
     * Creates an item array from the given list of items.
     *
     * @param items - the given list of items
     * @return an array of the given items
     */
    public static int[] getItemArrayFromItemList(List<Integer> items) {
        int[] itemArr = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemArr[i] = items.get(i);
        }
        return itemArr;
    }

    public static void removeItemFromOutdatedPosition(int item, int[][] stacks) {
        for (int stack = 0; stack < stacks.length; stack++) {
            for (int level = 0; level < stacks[stack].length; level++) {

                if (stacks[stack][level] == item) {
                    stacks[stack][level] = -1;
                    // TODO: lower all items that are stacked in the air
                }
            }
        }
    }

    /**
     * Returns the savings for the specified item.
     *
     * @param stackIdx - the index of the considered stack
     * @param costsBefore - the original costs for each item assignment
     * @param item - the item the savings are computed for
     * @return the savings for the specified item
     */
    public static double getSavingsForItem(int stackIdx, Map<Integer, Double> costsBefore, int item, double[][] costMatrix) {
        double costs = costMatrix[item][stackIdx];
        return costsBefore.get(item) - costs;
    }

    /**
     * Returns the maximum savings for the specified pair.
     *
     * @param itemOne - the first item of the pair
     * @param itemTwo - the second item of the pair
     * @param stackIdx - the target stack
     * @param costsBefore - the original costs for each item assignment
     * @param costMatrix - the costs for each item-stack assignment
     * @return the savings for the specified pair
     */
    public static double getSavingsForPair(int itemOne, int itemTwo, int stackIdx, HashMap<Integer, Double> costsBefore, double[][] costMatrix) {
        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        return savingsItemOne > savingsItemTwo ? savingsItemOne : savingsItemTwo;
    }

    public static double getSavingsForTriple(
        ArrayList<ArrayList<Integer>> itemTriples, int tripleIdx, int stackIdx, HashMap<Integer, Double> costsBefore, double[][] costMatrix
    ) {
        int itemOne = itemTriples.get(tripleIdx).get(0);
        int itemTwo = itemTriples.get(tripleIdx).get(1);
        int itemThree = itemTriples.get(tripleIdx).get(2);

        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double costsItemThree = costMatrix[itemThree][stackIdx];

        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        double savingsItemThree = costsBefore.get(itemThree) - costsItemThree;

        ArrayList<Double> savings = new ArrayList<>(Arrays.asList(savingsItemOne, savingsItemTwo, savingsItemThree));
        return Collections.max(savings);
    }

    public static void updateAssignmentsForCompatibleTriple(
        int itemOne, int itemTwo, int itemThree, int stack, HashMap<Integer, Double> costsBefore, Instance instance
    ) {
        double costsItemOne = instance.getCosts()[itemOne][stack];
        double costsItemTwo = instance.getCosts()[itemTwo][stack];
        double costsItemThree = instance.getCosts()[itemThree][stack];

        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        double savingsItemThree = costsBefore.get(itemThree) - costsItemThree;

        ArrayList<Double> savings = new ArrayList<>(Arrays.asList(savingsItemOne, savingsItemTwo, savingsItemThree));
        double maxSavings = Collections.max(savings);

        if (maxSavings == savingsItemOne) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemOne, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemOne;
        } else if (maxSavings == savingsItemTwo) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemTwo, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemTwo;
        } else {
            HeuristicUtil.removeItemFromOutdatedPosition(itemThree, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemThree;
        }
    }

    public static void updateAssignmentsForCompatiblePair(
        int itemOne, int stack, int itemTwo, HashMap<Integer, Double> costsBefore, Instance instance
    ) {

        double costsItemOne = instance.getCosts()[itemOne][stack];
        double costsItemTwo = instance.getCosts()[itemTwo][stack];
        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;

        if (savingsItemOne > savingsItemTwo) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemOne, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemOne;
        } else {
            HeuristicUtil.removeItemFromOutdatedPosition(itemTwo, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemTwo;
        }
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
     * Retrieves all the stacks that remain empty in the original solution.
     *
     * @param sol - the original solution
     * @return the list of empty stacks
     */
    public static ArrayList<String> retrieveEmptyStacks(Solution sol) {

        int stackCapacity = sol.getFilledStacks()[0].length;

        ArrayList<String> emptyStacks = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            int sumOfEntries = 0;
            for (int entry : sol.getFilledStacks()[stack]) {
                sumOfEntries += entry;
            }
            if (sumOfEntries == -stackCapacity) {
                emptyStacks.add("stack" + stack);
            }
        }
        return emptyStacks;
    }

    /**
     * Retrieves the empty positions from the storage area.
     *
     * @param sol - solution to retrieve the empty positions for
     * @return list of empty positions in the storage area
     */
    public static ArrayList<StackPosition> retrieveEmptyPositions(Solution sol) {
        ArrayList<StackPosition> emptyPositions = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int level = 0; level < sol.getFilledStacks()[stack].length; level++) {
                if (sol.getFilledStacks()[stack][level] == -1) {
                    emptyPositions.add(new StackPosition(stack, level));
                }
            }
        }
        return emptyPositions;
    }

    /**
     * Returns the costs for each item's assignment in the original solution.
     *
     * @param sol - the original solution
     * @return hashmap containing the original costs for each item assignment
     */
    public static HashMap<Integer, Double> getOriginalCosts(Solution sol, double[][] costMatrix) {
        HashMap<Integer, Double> originalCosts = new HashMap<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int entry : sol.getFilledStacks()[stack]) {
                if (entry != -1) {
                    originalCosts.put(entry, costMatrix[entry][stack]);
                }
            }
        }
        return originalCosts;
    }

    /**
     * Returns the best solution from the list of generated solutions.
     *
     * @param solutions - the list of generated solutions
     * @return the best solution based on the costs
     */
    public static Solution getBestSolution(List<Solution> solutions) {
        Solution bestSol = new Solution();
        for (Solution sol : solutions) {
            if (sol.computeCosts() < bestSol.computeCosts()) {
                bestSol = sol;
            }
        }
        return bestSol;
    }

    /**
     * Updates the stack assignments for the specified pair.
     *
     * @param itemOne - the first item of the pair
     * @param itemTwo - the second item of the pair
     * @param stack - the target stack
     * @param costsBefore - the costs for the original item assignments
     * @param instance - the considered instance of the stacking problem
     */
    public static void updateStackAssignmentsForPairs(
        int itemOne, int itemTwo, int stack, HashMap<Integer, Double> costsBefore, Instance instance
    ) {
        // both items compatible
        if (instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / instance.getItems().length
                && instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / instance.getItems().length) {

            HeuristicUtil.updateAssignmentsForCompatiblePair(itemOne, stack, itemTwo, costsBefore, instance);

            // item one compatible
        } else if (instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / instance.getItems().length) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemOne, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemOne;
            // item two compatible
        } else if (instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / instance.getItems().length) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemTwo, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemTwo;
        }
    }

    /**
     * Adds the edges for item pairs in the post-processing graph.
     * The costs for each edge correspond to the maximum savings for
     * moving an item of the pair to an empty stack.
     *
     * @param postProcessingGraph - the graph to add the edges to
     * @param itemPairs - the item pairs to be connected to compatible empty stacks
     * @param emptyStacks - the the empty stacks to be connected to compatible item pairs
     * @param originalCosts - the costs for the original item assignments
     */
    public static void addEdgesForItemPairs(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
            ArrayList<ArrayList<Integer>> itemPairs,
            ArrayList<String> emptyStacks,
            HashMap<Integer, Double> originalCosts,
            Instance instance
    ) {

        for (int pair = 0; pair < itemPairs.size(); pair++) {
            for (String emptyStack : emptyStacks) {

                DefaultWeightedEdge edge = postProcessingGraph.addEdge("pair" + itemPairs.get(pair), emptyStack);
                int stackIdx = Integer.parseInt(emptyStack.replace("stack", "").trim());
                double savings = 0.0;

                // both items compatible
                if (instance.getCosts()[itemPairs.get(pair).get(0)][stackIdx] < Integer.MAX_VALUE / instance.getItems().length
                    && instance.getCosts()[itemPairs.get(pair).get(1)][stackIdx] < Integer.MAX_VALUE / instance.getItems().length) {

                        savings = HeuristicUtil.getSavingsForPair(
                            itemPairs.get(pair).get(0), itemPairs.get(pair).get(1), stackIdx, originalCosts, instance.getCosts()
                        );

                // item one compatible
                } else if (instance.getCosts()[itemPairs.get(pair).get(0)][stackIdx] < Integer.MAX_VALUE / instance.getItems().length) {
                    int itemOne = itemPairs.get(pair).get(0);
                    savings = HeuristicUtil.getSavingsForItem(stackIdx, originalCosts, itemOne, instance.getCosts());
                // item two compatible
                } else if (instance.getCosts()[itemPairs.get(pair).get(1)][stackIdx] < Integer.MAX_VALUE / instance.getItems().length) {
                    int itemTwo = itemPairs.get(pair).get(1);
                    savings = HeuristicUtil.getSavingsForItem(stackIdx, originalCosts, itemTwo, instance.getCosts());
                }
                postProcessingGraph.setEdgeWeight(edge, savings);
            }
        }
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
        if (HeuristicUtil.pairStackableInBothDirections(pairItemOne, pairItemTwo, stackingConstraints)) {
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
     * Rounds the given value to the next half.
     *
     * @param val - the value to be rounded
     * @return the rounded value
     */
    public static float roundToHalf(float val) {
        return (float)(Math.round(val * 2) / 2.0);
    }

    /**
     * Returns a random value in between the specified limits in .5 steps.
     *
     * @param min - the lower bound of the value
     * @param max - the upper bound of the value
     * @return the random value in between the specified limits
     */
    public static float getRandomValueInBetweenHalfSteps(float min, float max) {
        Random r = new Random();
        float val = roundToHalf((float)(Math.round(min + r.nextFloat() * (max - min) * 10.0) / 10.0));
        if (val > max) { val = max; }
        return val;
    }

    /**
     * Returns a random integer in between the specified limits.
     *
     * @param min - the lower bound of the value
     * @param max - the upper bound of the value
     * @return the random value in between the specified limits (inclusive limits)
     */
    public static int getRandomIntegerInBetween(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static int getRandomIntegerInBetweenWithException(int min, int max, int exception) {
        Random r = new Random();
        int val = r.nextInt((max - min) + 1) + min;
        while (val == exception) {
            val = r.nextInt((max - min) + 1) + min;
        }
        return val;
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
