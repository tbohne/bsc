package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class ThreeCapRecursiveMCMHeuristic {

    private Instance instance;
    private ArrayList<ArrayList<Integer>> stackAssignments;
    private double startTime;
    private int timeLimit;
    private ArrayList<ArrayList<MCMEdge>> alreadyUsedEdgeShuffles;

    public ThreeCapRecursiveMCMHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.stackAssignments = new ArrayList<>();
        this.alreadyUsedEdgeShuffles = new ArrayList<>();
    }

    /**
     * Adds an edge between compatible pairs and unmatched items.
     *
     * @param graph - the graph to be extended
     * @param pairItemOne - the first item of the pair
     * @param unmatchedItem - the unmatched item
     * @param pairItemTwo - the second item of the pair
     * @param itemPair - the edge representing the item pair
     */
    public void addEdgeForCompatibleItemTriple(DefaultUndirectedGraph<String, DefaultEdge> graph, int pairItemOne, int unmatchedItem, int pairItemTwo, MCMEdge itemPair) {

        if (this.instance.getStackingConstraints()[pairItemOne][unmatchedItem] == 1
            || this.instance.getStackingConstraints()[unmatchedItem][pairItemTwo] == 1) {

                if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
                    graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
                }
        }
    }

    /**
     * Generates the bipartite graph between item pairs and unmatched items.
     *
     * @param graph - the graph to be generated
     * @param itemPairs - the item pairs
     * @param unmatchedItems - the unmatched items
     */
    public void generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            DefaultUndirectedGraph<String, DefaultEdge> graph, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems
    ) {

        // adding the item pairs as nodes to the graph
        for (int i = 0; i < itemPairs.size(); i++) {
            graph.addVertex("edge" + itemPairs.get(i));
        }

        // adding the unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < unmatchedItems.size(); j++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done
                if (this.instance.getStackingConstraints()[itemPairs.get(i).getVertexOne()][itemPairs.get(i).getVertexTwo()] == 1) {
                    this.addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexTwo(), unmatchedItems.get(j),
                        itemPairs.get(i).getVertexOne(), itemPairs.get(i)
                    );
                } else {
                    this.addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexOne(), unmatchedItems.get(j),
                        itemPairs.get(i).getVertexTwo(), itemPairs.get(i)
                    );
                }
            }
        }
    }

    /**
     * Returns the unassigned items based on the items that are assigned so far.
     *
     * @return the list of unassigned items
     */
    public ArrayList<Integer> getUnassignedItems() {
        ArrayList<Integer> alreadyAssignedItems = new ArrayList<>();
        for (ArrayList<Integer> stack : this.stackAssignments) {
            for (int item : stack) {
                alreadyAssignedItems.add(item);
            }
        }
        ArrayList<Integer> unassignedItems = new ArrayList<>();
        for (int i : this.instance.getItems()) {
            if (!alreadyAssignedItems.contains(i)) {
                unassignedItems.add(i);
            }
        }
        return unassignedItems;
    }

    /**
     * Returns whether the given item is validly assignable to the given pair.
     *
     * @param item - the item to be checked
     * @param pairItemOne - the first item of the pair
     * @param pairItemTwo - the second item of the pair
     * @return whether or not the item is assignable to the pair
     */
    public boolean itemAssignableToPair(int item, int pairItemOne, int pairItemTwo) {

        // pair stackable in both directions
        if (HeuristicUtil.itemsStackableInBothDirections(pairItemOne, pairItemTwo, this.instance.getStackingConstraints())) {
            if (this.instance.getStackingConstraints()[item][pairItemOne] == 1) {
                // itemOne above itemOneNew above itemTwoNew --> itemOne assigned
                return true;
            } else if (this.instance.getStackingConstraints()[pairItemOne][item] == 1) {
                // itemTwoNew above itemOneNew above itemOne --> itemOne assigned
                return true;
            } else if (this.instance.getStackingConstraints()[pairItemTwo][item] == 1) {
                // itemOneNew above itemTwoNew above itemOne --> itemOne assigned
                return true;
            } else if (this.instance.getStackingConstraints()[item][pairItemTwo] == 1) {
                // itemOne above itemTwoNew above itemOneNew --> itemOne assigned
                return true;
            }
        // pairItemOne above pairItemTwo
        } else if (this.instance.getStackingConstraints()[pairItemOne][pairItemTwo] == 1) {
            if (this.instance.getStackingConstraints()[item][pairItemOne] == 1) {
                return true;
            } else if (this.instance.getStackingConstraints()[pairItemTwo][item] == 1) {
                return true;
            }
        // pairItemTwo above pairItemOne
        } else {
            if (this.instance.getStackingConstraints()[item][pairItemTwo] == 1) {
                return true;
            } else if (this.instance.getStackingConstraints()[pairItemOne][item] == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the list of completely filled stacks and prepares the corresponding items to be removed from the remaining item pairs.
     *
     * @param itemPairRemovalList - the list to keep track of the items that should be removed form the remaining item pairs
     * @param itemOneEdge - the edge (item pair), the first item is assigned to
     * @param itemTwoEdge - the edge (item pair), the second item is assigned to
     * @param startingPair - the pair that is going to be assigned
     * @param itemOne - the first item to be assigned
     * @param itemTwo - the second item to be assigned
     * @param completelyFilledStacks - the list of completely filled stacks
     */
    public void updateCompletelyFilledStacks(
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
     * (Breaks up a pair and tries to assign both items two new pairs to build up completely filled stacks).
     *
     * @param itemPairs - the list of item pairs
     * @param itemPairRemovalList - the list of edges (item pairs) to be removed
     * @param completelyFilledStacks - list to store the completely filled stacks
     */
    public void generateCompletelyFilledStacks(ArrayList<MCMEdge> itemPairs, ArrayList<MCMEdge> itemPairRemovalList, ArrayList<ArrayList<Integer>> completelyFilledStacks) {
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

                    if (!itemOneAssigned && this.itemAssignableToPair(itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo)) {
                        itemOneAssigned = true;
                        itemOneEdge = potentialTargetPair;
                        continue;
                    }
                    if (!itemTwoAssigned && this.itemAssignableToPair(itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo)) {
                        itemTwoAssigned = true;
                        itemTwoEdge = potentialTargetPair;
                        continue;
                    }
                }
            }

            if (itemOneAssigned && itemTwoAssigned) {
                this.updateCompletelyFilledStacks(itemPairRemovalList, itemOneEdge, itemTwoEdge, startingPair, itemOne, itemTwo, completelyFilledStacks);
            }
        }
    }

    /**
     * Removes the item pairs that are already part of completely filled stacks from the list of remaining item pairs.
     *
     * @param itemPairRemovalList - the item pairs to be removed
     * @param itemPairs - the list, the pairs will be removed from
     */
    public void removeAlreadyUsedItemPairs(ArrayList<MCMEdge> itemPairRemovalList, ArrayList<MCMEdge> itemPairs) {
        for (MCMEdge e : itemPairRemovalList) {
            itemPairs.remove(itemPairs.indexOf(e));
        }
    }

    /**
     * Returns the unmatched items (not part of pair and not assigned so far).
     *
     * @param itemPairs - the list of matched items
     * @param completelyFilledStacks - the list of completely filled stacks
     * @return the unmatched items
     */
    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<Integer>> completelyFilledStacks) {
        ArrayList<Integer> assignedItems = new ArrayList<>();
        for (ArrayList<Integer> stack : completelyFilledStacks) {
            for (int item : stack) {
                assignedItems.add(item);
            }
        }
        for (MCMEdge edge : itemPairs) {
            assignedItems.add(edge.getVertexOne());
            assignedItems.add(edge.getVertexTwo());
        }
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            if (!assignedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    /**
     * Parses and sorts the item pairs resulting from the edges of the maximum cardinality matching.
     * The sorting procedure is just relevant for cases in which there are more item pairs than stacks.
     *
     * @param mcm - the maximum cardinality matching to be parsed
     * @return the parsed and sorted list of item pairs
     */
    public ArrayList<MCMEdge> parseAndSortItemPairs(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(mcm);
        HeuristicUtil.assignColRatingToEdgesNewWay(itemPairs, this.instance.getStackingConstraints());
        Collections.sort(itemPairs);
        return itemPairs;
    }

    /**
     * Tries to merge item pairs by splitting up pairs and assigning both items
     * to other pairs to form completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<Integer>> mergeItemPairs(ArrayList<MCMEdge> itemPairs) {
        ArrayList<MCMEdge> itemPairRemovalList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> completelyFilledStacks = new ArrayList<>();
        this.generateCompletelyFilledStacks(itemPairs, itemPairRemovalList, completelyFilledStacks);
        this.stackAssignments.addAll(completelyFilledStacks);
        this.removeAlreadyUsedItemPairs(itemPairRemovalList, itemPairs);
        return completelyFilledStacks;
    }

    /**
     * Computes compatible item triples and adds them to the list of stack assignments.
     *
     * @param itemPairs - list of item pairs
     * @param unmatchedItems - list of unmatched items
     */
    public void computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(graph, itemPairs, unmatchedItems);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTripleStackAssignments = HeuristicUtil.parseItemTripleMCM(itemTriples);
        this.stackAssignments.addAll(itemTripleStackAssignments);
    }

    /**
     * Generates permutations for the item pairs as well as the corresponding lists of remaining items.
     *
     * @param edges - the pairs of items
     * @param remainingItems - the list of remaining items
     * @param itemPairPermutations - the list of item pair permutations
     * @param listsOfRemainingItems - the list of lists of remaining items
     */
    public void generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            ArrayList<MCMEdge> edges,
            ArrayList<Integer> remainingItems,
            ArrayList<ArrayList<MCMEdge>> itemPairPermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems
    ) {

        HeuristicUtil.assignColRatingToEdgesNewWay(edges, this.instance.getStackingConstraints());
        Collections.sort(edges);

        // TODO: remove hard coded value
        for (int i = 0; i < 1000; i++) {
            ArrayList<Integer> tmpRemainingItems = new ArrayList<>(remainingItems);
            ArrayList<MCMEdge> tmpItemPairs = new ArrayList<>();

            for (int j = 0; j < edges.size(); j++) {
                if (j < this.instance.getStacks().length) {
                    tmpItemPairs.add(edges.get(j));
                } else {
                    tmpRemainingItems.add(edges.get(j).getVertexOne());
                    tmpRemainingItems.add(edges.get(j).getVertexTwo());
                }
            }

            this.alreadyUsedEdgeShuffles.add(new ArrayList<>(edges));
            int unsuccessfulShuffleAttempts = 0;
            while (alreadyUsedEdgeShuffles.contains(edges) && unsuccessfulShuffleAttempts < 10) {
                Collections.shuffle(edges);
                unsuccessfulShuffleAttempts++;
            }
            itemPairPermutations.add(new ArrayList<>(tmpItemPairs));
            listsOfRemainingItems.add(new ArrayList<>(tmpRemainingItems));
        }
    }

    /**
     * Generates the stack assignments for the given pairs and remaining items.
     *
     * @param currentEdges - the list of item pairs
     * @param currentRemainingItems - the list of remaining items
     * @param currentStackAssignments - the list of stack assignments
     * @return the number of performed assignments
     */
    public int generateCurrentStackAssignments(
            ArrayList<MCMEdge> currentEdges,
            ArrayList<Integer> currentRemainingItems,
            ArrayList<ArrayList<Integer>> currentStackAssignments
    ) {

        int numberOfAssignments = 0;

        for (MCMEdge e : currentEdges) {

            // TODO: sort items here and prefer the inflexible ones

            ArrayList<Integer> currentStack = new ArrayList<>();
            currentStack.add(e.getVertexOne());
            currentStack.add(e.getVertexTwo());

            for (int item : currentRemainingItems) {
                if (this.instance.getStackingConstraints()[e.getVertexOne()][e.getVertexTwo()] == 1) {
                    if (this.instance.getStackingConstraints()[e.getVertexTwo()][item] == 1
                        || this.instance.getStackingConstraints()[item][e.getVertexOne()] == 1)
                    {
                        numberOfAssignments++;
                        currentStack.add(item);
                        currentRemainingItems.remove(currentRemainingItems.indexOf(item));
                        break;
                    }
                } else {
                    if (this.instance.getStackingConstraints()[e.getVertexOne()][item] == 1
                        || this.instance.getStackingConstraints()[item][e.getVertexTwo()] == 1)
                    {
                        numberOfAssignments++;
                        currentStack.add(item);
                        currentRemainingItems.remove(currentRemainingItems.indexOf(item));
                        break;
                    }
                }
            }
            currentStackAssignments.add(currentStack);
        }
        return numberOfAssignments;
    }

    /**
     * Finds the best remaining stack assignments.
     *
     * @param bestStackAssignments - the best stack assignments
     * @param edgePermutations - the list of permutations of item pairs
     * @param listsOfRemainingItems - the list of permutations of the remaining items
     * @param remainingItems - the remaining items
     */
    public void findBestRemainingStackAssignments(
            ArrayList<ArrayList<Integer>> bestStackAssignments,
            ArrayList<ArrayList<MCMEdge>> edgePermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems,
            ArrayList<Integer> remainingItems
    ) {

        int maxNumberOfAssignments = 0;

        for (ArrayList<MCMEdge> currentEdges : edgePermutations) {
            ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
            ArrayList<Integer> currentRemainingItems = new ArrayList<>(listsOfRemainingItems.get(edgePermutations.indexOf(currentEdges)));
            int numberOfAssignments = this.generateCurrentStackAssignments(currentEdges, currentRemainingItems, currentStackAssignments);

            if (numberOfAssignments >= maxNumberOfAssignments) {
                maxNumberOfAssignments = numberOfAssignments;
                bestStackAssignments.clear();
                bestStackAssignments.addAll(currentStackAssignments);
                remainingItems.clear();
                remainingItems.addAll(currentRemainingItems);
            }
        }
    }

    /**
     * Updates the list of remaining items.
     *
     * @param edges - the item pairs
     * @param remainingItems - the list of remaining items
     */
    public void updateRemainingItems(ArrayList<MCMEdge> edges, ArrayList<Integer> remainingItems) {
        for (MCMEdge e : edges) {
            if (remainingItems.contains(e.getVertexOne())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexOne()));
            }
            if (remainingItems.contains(e.getVertexTwo())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexTwo()));
            }
        }
    }

    /**
     * Assigns the unstackable items to its own stacks.
     *
     * @param remainingItems - the remaining (unstackable) items
     */
    public void assignUnstackableItemsToOwnStack(ArrayList<Integer> remainingItems) {
        for (int item : remainingItems) {
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(item);
            this.stackAssignments.add(stack);
        }
    }

    /**
     * Fills the storage area with the generated stack assignments.
     * TODO: The costs could be considered here.
     */
    public void fillStorageAreaWithGeneratedStackAssignments() {

        ArrayList<Integer> usedStacks = new ArrayList<>();

        for (ArrayList<Integer> currStack : this.stackAssignments) {

            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                if (usedStacks.contains(stack)) { continue; }

                boolean itemsCompatible = true;
                for (int item : currStack) {
                    if (this.instance.getCosts()[item][stack] >= Integer.MAX_VALUE / this.instance.getItems().length) {
                        itemsCompatible = false;
                    }
                }
                if (!itemsCompatible) { continue; }
                for (int level = 0; level < this.instance.getStacks()[stack].length; level++) {
                    if (level < currStack.size()) {
                        this.instance.getStacks()[stack][level] = currStack.get(level);
                    }
                }
                usedStacks.add(stack);
                break;
            }
        }
    }

    /**
     * Checks whether all items have been assigned to a stack.
     */
    public void checkItemAssignments() {
        ArrayList<Integer> remainingItems = this.getUnassignedItems();
        if (remainingItems.size() > 0) {
            System.out.println("Problem: Not all items have been assigned, even though the process is complete.");
        }
    }

    /**
     * Applies the best remaining stack assignments.
     *
     * @param itemPairPermutations - the list of permutations of the item pairs
     * @param listsOfRemainingItems - the list of permutations of the remaining items
     * @param remainingItems - the list of remaining items
     */
    public void applyBestRemainingStackAssignments(
            ArrayList<ArrayList<MCMEdge>> itemPairPermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems,
            ArrayList<Integer> remainingItems
    ) {
        ArrayList<ArrayList<Integer>> bestStackAssignments = new ArrayList<>();
        this.findBestRemainingStackAssignments(bestStackAssignments, itemPairPermutations, listsOfRemainingItems, remainingItems);
        this.stackAssignments.addAll(bestStackAssignments);
        this.assignUnstackableItemsToOwnStack(remainingItems);
    }

    /**
     * Completes the process by assigning the finally unassigned items to free positions in the stacks.
     * First, there is a final mcm that defines pairs of the remaining items if possible.
     * After that permutations of these pairs are generated and for each permutation the list of remaining (unmatched)
     * items is computes as well. Finally, the best assignment of all these items is chosen and gets assigned.
     */
    public void completeStackAssignments() {
        ArrayList<Integer> remainingItems = this.getUnassignedItems();

        EdmondsMaximumCardinalityMatching mcm = HeuristicUtil.getMCMForItemList(remainingItems, this.instance.getStackingConstraints());
        ArrayList<MCMEdge> itemPairs = this.parseAndSortItemPairs(mcm);
        this.updateRemainingItems(itemPairs, remainingItems);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        ArrayList<ArrayList<Integer>> listsOfRemainingItems = new ArrayList<>();
        this.generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            itemPairs, remainingItems, itemPairPermutations, listsOfRemainingItems
        );

        this.applyBestRemainingStackAssignments(itemPairPermutations, listsOfRemainingItems, remainingItems);
        this.checkItemAssignments();
        this.fillStorageAreaWithGeneratedStackAssignments();
    }

    /**
     * TODO: find new name...
     */
    public void threeCapApproachTwo(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> itemPairs = this.parseAndSortItemPairs(mcm);
        ArrayList<ArrayList<Integer>> completelyFilledStacks = this.mergeItemPairs(itemPairs);
        ArrayList<Integer> unmatchedItems = this.getUnmatchedItems(itemPairs, completelyFilledStacks);
        if (itemPairs.size() > 0 && unmatchedItems.size() > 0) {
            this.computeCompatibleItemTriples(itemPairs, unmatchedItems);
        }
    }

    /**
     * TODO: describe approach
     * Solves the SP with an approach that ...
     *
     * @param optimizeSolution - specifies whether the solution should be optimized or just valid
     * @return the solution generated by the heuristic
     */
    public Solution solve(boolean optimizeSolution) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> graph = HeuristicUtil.generateStackingConstraintGraph(
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                Integer.MAX_VALUE / this.instance.getItems().length,
                this.instance.getStacks()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

            this.threeCapApproachTwo(mcm);
            this.completeStackAssignments();

            sol = new Solution(0, this.timeLimit, this.instance);
            sol.transformStackAssignmentIntoValidSolutionIfPossible();
            System.out.println("sol feasible: " + sol.isFeasible());
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return sol;
    }
}
