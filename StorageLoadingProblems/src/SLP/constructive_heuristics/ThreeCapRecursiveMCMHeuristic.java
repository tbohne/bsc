package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class ThreeCapRecursiveMCMHeuristic {

    private Instance instance;
    private ArrayList<ArrayList<Integer>> stackAssignments;
    private int previousNumberOfRemainingItems;
    private double startTime;

    public ThreeCapRecursiveMCMHeuristic(Instance instance) {
        this.instance = instance;
        this.stackAssignments = new ArrayList<>();
        this.previousNumberOfRemainingItems = this.instance.getItems().length;
    }

    public void completeStackAssignment(DefaultUndirectedGraph<String, DefaultEdge> graph, int pairItemA, int unmatchedItem, int pairItemB, MCMEdge itemPair) {

        if (this.instance.getStackingConstraints()[pairItemA][unmatchedItem] == 1
            || this.instance.getStackingConstraints()[unmatchedItem][pairItemB] == 1) {

            if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
                graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
            }
        }
    }

    public void generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            ArrayList<MCMEdge> itemPairs,
            ArrayList<Integer> unmatchedItems,
            int numberOfUsedItemPairs
    ) {

        // adding the specified number of item pairs as nodes to the graph
        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            graph.addVertex("edge" + itemPairs.get(i));
        }

        // adding all unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            for (int j = 0; j < unmatchedItems.size(); j++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done
                if (this.instance.getStackingConstraints()[itemPairs.get(i).getVertexOne()][itemPairs.get(i).getVertexTwo()] == 1) {
                    this.completeStackAssignment(graph, itemPairs.get(i).getVertexTwo(), unmatchedItems.get(j),
                        itemPairs.get(i).getVertexOne(), itemPairs.get(i)
                    );
                } else {
                    this.completeStackAssignment(graph, itemPairs.get(i).getVertexOne(), unmatchedItems.get(j),
                        itemPairs.get(i).getVertexTwo(), itemPairs.get(i)
                    );
                }
            }
        }
    }

    public ArrayList<Integer> getCurrentListOfUnmatchedItems(int length, ArrayList<MCMEdge> edges, ArrayList<Integer> unassignedItems) {

        HeuristicUtil.assignColRatingToEdges(edges, this.instance.getStackingConstraints());
        Collections.sort(edges);

        ArrayList<Integer> matchedItemsOfChoice = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            matchedItemsOfChoice.add(edges.get(i).getVertexOne());
            matchedItemsOfChoice.add(edges.get(i).getVertexTwo());
        }
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : unassignedItems) {
            if (!matchedItemsOfChoice.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    public ArrayList<Integer> getUnassignedItemsFromStorageAreaSnapshot(ArrayList<ArrayList<Integer>> storageArea) {
        ArrayList<Integer> alreadyAssignedItems = new ArrayList<>();
        for (ArrayList<Integer> stack : storageArea) {
            for (int item : stack) {
                alreadyAssignedItems.add(item);
            }
        }
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

    public void recursiveMCMApproach(EdmondsMaximumCardinalityMatching mcm, int numberOfEdgesToBeUsed, ArrayList<Integer> remainingItems) {

        // base case
        if (remainingItems.size() == 0 || this.previousNumberOfRemainingItems == remainingItems.size()) {
            if (this.previousNumberOfRemainingItems != this.instance.getItems().length) { return; }
        }

        this.previousNumberOfRemainingItems = remainingItems.size();
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        HeuristicUtil.assignColRatingToEdges(itemPairs, this.instance.getStackingConstraints());
        Collections.sort(itemPairs);

        // COMPUTING COMPATIBLE ITEM TRIPLES FROM ITEM PAIRS AND REMAINING ITEMS
        ArrayList<Integer> unmatchedItems = this.getCurrentListOfUnmatchedItems(numberOfEdgesToBeUsed, itemPairs, remainingItems);
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(graph, itemPairs, unmatchedItems, numberOfEdgesToBeUsed);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
        HeuristicUtil.parseItemTripleMCM(currentStackAssignments, itemTriples);

        this.stackAssignments.addAll(currentStackAssignments);

        // COMPUTING COMPATIBLE ITEM PAIRS FROM REMAINING ITEMS
        unmatchedItems = this.getUnassignedItemsFromStorageAreaSnapshot(currentStackAssignments);
        EdmondsMaximumCardinalityMatching remainingItemPairs = HeuristicUtil.getMCMForUnassignedItems(unmatchedItems, this.instance.getStackingConstraints());
        itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, remainingItemPairs);
        numberOfEdgesToBeUsed = (int)Math.ceil(itemPairs.size() / 3);

        this.recursiveMCMApproach(remainingItemPairs, numberOfEdgesToBeUsed, unmatchedItems);
    }

    public void generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            ArrayList<MCMEdge> edges,
            int numberOfUsedEdges,
            ArrayList<Integer> remainingItems,
            ArrayList<ArrayList<MCMEdge>> itemPairPermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems
    ) {

        for (int i = 0; i < 10000; i++) {
            ArrayList<Integer> tmpRemainingItems = new ArrayList<>(remainingItems);
            ArrayList<MCMEdge> tmpItemPairs = new ArrayList<>();

            for (int j = 0; j < edges.size(); j++) {
                if (j < numberOfUsedEdges) {
                    tmpItemPairs.add(edges.get(j));
                } else {
                    tmpRemainingItems.add(edges.get(j).getVertexOne());
                    tmpRemainingItems.add(edges.get(j).getVertexTwo());
                }
            }
            // TODO: add check for already used shuffles
            Collections.shuffle(edges);
            itemPairPermutations.add(new ArrayList<>(tmpItemPairs));
            listsOfRemainingItems.add(new ArrayList<>(tmpRemainingItems));
        }
    }

    public int generateCurrentStackAssignments(
            ArrayList<MCMEdge> currentEdges,
            ArrayList<Integer> currentRemainingItems,
            ArrayList<ArrayList<Integer>> currentStackAssignments
    ) {

        int numberOfAssignments = 0;

        for (MCMEdge e : currentEdges) {
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

            if (numberOfAssignments > maxNumberOfAssignments) {
                maxNumberOfAssignments = numberOfAssignments;
                bestStackAssignments.clear();
                bestStackAssignments.addAll(currentStackAssignments);
                remainingItems.clear();
                remainingItems.addAll(currentRemainingItems);
            }
        }
    }

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

    public void assignUnstackableItemsToOwnStack(ArrayList<Integer> remainingItems) {
        for (int item : remainingItems) {
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(item);
            this.stackAssignments.add(stack);
        }
    }

    public void fillStorageAreaWithGeneratedStackAssignments() {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
                if (i < this.stackAssignments.size()) {
                    ArrayList<Integer> stack = this.stackAssignments.get(i);
                    if (j < stack.size()) {
                        this.instance.getStacks()[i][j] = stack.get(j);
                    }
                }
            }
        }
    }

    public void checkItemAssignments() {
        ArrayList<Integer> remainingItems = this.getUnassignedItemsFromStorageAreaSnapshot(this.stackAssignments);
        if (remainingItems.size() > 0) {
            System.out.println("Problem: Not all items have been assigned, even though the process is complete.");
        }
    }

    public void completeStackAssignmentsForRecursiveApproach() {
        ArrayList<Integer> remainingItems = this.getUnassignedItemsFromStorageAreaSnapshot(this.stackAssignments);
        EdmondsMaximumCardinalityMatching mcm = HeuristicUtil.getMCMForUnassignedItems(remainingItems, this.instance.getStackingConstraints());
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        this.updateRemainingItems(itemPairs, remainingItems);

        HeuristicUtil.assignColRatingToEdges(itemPairs, this.instance.getStackingConstraints());
        Collections.sort(itemPairs);
        // TODO: search for reasonable way to compute the number of used item pairs
        int numberOfUsedItemPairs = itemPairs.size() - (int)Math.ceil(itemPairs.size() / 2) + 10;

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        ArrayList<ArrayList<Integer>> listsOfRemainingItems = new ArrayList<>();
        this.generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            itemPairs, numberOfUsedItemPairs, remainingItems, itemPairPermutations, listsOfRemainingItems
        );

        ArrayList<ArrayList<Integer>> bestStackAssignments = new ArrayList<>();
        this.findBestRemainingStackAssignments(bestStackAssignments, itemPairPermutations, listsOfRemainingItems, remainingItems);
        this.stackAssignments.addAll(bestStackAssignments);
        this.assignUnstackableItemsToOwnStack(remainingItems);

        System.out.println("stacks used: " + this.stackAssignments.size());
        this.checkItemAssignments();
        this.fillStorageAreaWithGeneratedStackAssignments();
    }

    /**
     *
     * @param optimizeSolution - specifies whether the solution should be optimized or just valid
     * @return
     */
    public Solution solve(boolean optimizeSolution) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            HeuristicUtil.generateStackingConstraintGraph(graph, this.instance.getItems(), this.instance.getStackingConstraints());
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

            ArrayList<Integer> items = new ArrayList<>();
            for (int item : this.instance.getItems()) {
                items.add(item);
            }
            this.recursiveMCMApproach(mcm, this.instance.getStacks().length, items);
            this.completeStackAssignmentsForRecursiveApproach();

            sol = new Solution(0, false, this.instance);
            sol.transformStackAssignmentIntoValidSolutionIfPossible();
            System.out.println("sol feasible: " + sol.isFeasible());
            System.out.println(sol.getNumberOfAssignedItems());

            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 3.");
        }

        return sol;
    }
}
