package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class ThreeCapRecursiveMCMHeuristic {

    private Instance instance;
    private ArrayList<ArrayList<Integer>> stackAssignments;
    private int previousNumberOfRemainingItems;
    private double startTime;
    private int timeLimit;
    private ArrayList<ArrayList<MCMEdge>> alreadyUsedEdgeShuffles;

    public ThreeCapRecursiveMCMHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.stackAssignments = new ArrayList<>();
        this.previousNumberOfRemainingItems = this.instance.getItems().length;
        this.alreadyUsedEdgeShuffles = new ArrayList<>();
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
            ArrayList<Integer> unmatchedItems
    ) {

        // adding the specified number of item pairs as nodes to the graph
        for (int i = 0; i < itemPairs.size(); i++) {
            graph.addVertex("edge" + itemPairs.get(i));
        }

        // adding all unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < itemPairs.size(); i++) {
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

        HeuristicUtil.assignColRatingToEdgesNewWay(edges, this.instance.getStackingConstraints());
        Collections.sort(edges);

        if (length > edges.size()) {
            length = edges.size();
        }

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

    public void generateCompletelyFilledStacks(ArrayList<MCMEdge> itemPairs, ArrayList<MCMEdge> itemPairRemovalList, ArrayList<ArrayList<Integer>> completelyFilledStacks) {
        for (MCMEdge pair : itemPairs) {

            if (itemPairRemovalList.contains(pair)) { continue; }

            int itemOne = pair.getVertexOne();
            int itemTwo = pair.getVertexTwo();
            boolean itemOneAssigned = false;
            boolean itemTwoAssigned = false;
            MCMEdge itemOneEdge = new MCMEdge(0, 0, 0);
            MCMEdge itemTwoEdge = new MCMEdge(0, 0, 0);

            for (MCMEdge pair2 : itemPairs) {

                if (itemPairRemovalList.contains(pair2)) { continue; }
                if (itemOneAssigned && itemTwoAssigned) { break; }

                if (pair != pair2) {

                    int pairItemOne = pair2.getVertexOne();
                    int pairItemTwo = pair2.getVertexTwo();

                    if (!itemOneAssigned && this.itemAssignableToPair(itemOne, pairItemOne, pairItemTwo)) {
                        itemOneAssigned = true;
                        itemOneEdge = pair2;
                        continue;
                    }

                    if (!itemTwoAssigned && this.itemAssignableToPair(itemTwo, pairItemOne, pairItemTwo)) {
                        itemTwoAssigned = true;
                        itemTwoEdge = pair2;
                        continue;
                    }
                }
            }

            if (itemOneAssigned && itemTwoAssigned) {
                itemPairRemovalList.add(itemOneEdge);
                itemPairRemovalList.add(itemTwoEdge);
                itemPairRemovalList.add(pair);

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
        }
    }

    public void removedAlreadyUsedItemPairs(ArrayList<MCMEdge> itemPairRemovalList, ArrayList<MCMEdge> itemPairs) {
        for (MCMEdge e : itemPairRemovalList) {
            itemPairs.remove(itemPairs.indexOf(e));
        }
    }

    public ArrayList<Integer> computeUnmatchedItems(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<Integer>> completelyFilledStacks) {
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
     * // TODO: find new name
     */
    public void threeCapApproachTwo(EdmondsMaximumCardinalityMatching mcm) {

        // PARSE AND SORT ITEM PAIRS
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        HeuristicUtil.assignColRatingToEdgesNewWay(itemPairs, this.instance.getStackingConstraints());
        Collections.sort(itemPairs);

        // GENERATE COMPLETELY FILLED STACKS FROM ITEM PAIRS
        // Idea: If both items of a pair can be assigned to another pair, two completely filled stacks are generated.
        ArrayList<MCMEdge> itemPairRemovalList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> completelyFilledStacks = new ArrayList<>();
        this.generateCompletelyFilledStacks(itemPairs, itemPairRemovalList, completelyFilledStacks);
        this.stackAssignments.addAll(completelyFilledStacks);
        this.removedAlreadyUsedItemPairs(itemPairRemovalList, itemPairs);

        ArrayList<Integer> unmatchedItems = this.computeUnmatchedItems(itemPairs, completelyFilledStacks);

        // COMPUTING COMPATIBLE ITEM TRIPLES FROM ITEM PAIRS AND REMAINING ITEMS
        if (itemPairs.size() > 0 && unmatchedItems.size() > 0) {
            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            this.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(graph, itemPairs, unmatchedItems);
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);
            ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
            HeuristicUtil.parseItemTripleMCM(currentStackAssignments, itemTriples);
            this.stackAssignments.addAll(currentStackAssignments);
        }
    }

    public void generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            ArrayList<MCMEdge> edges,
            ArrayList<Integer> remainingItems,
            ArrayList<ArrayList<MCMEdge>> itemPairPermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems
    ) {

        HeuristicUtil.assignColRatingToEdgesNewWay(edges, this.instance.getStackingConstraints());
        Collections.sort(edges);

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

    public void findBestRemainingStackAssignments(
            ArrayList<ArrayList<Integer>> bestStackAssignments,
            ArrayList<ArrayList<MCMEdge>> edgePermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems,
            ArrayList<Integer> remainingItems
    ) {

        int maxNumberOfAssignments = 0;

        System.out.println("EDGE PERM: " + edgePermutations);

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

        ArrayList<Integer> usedStacks = new ArrayList<>();

        for (ArrayList<Integer> currStack : this.stackAssignments) {

            for (int i = 0; i < this.instance.getStacks().length; i++) {

                if (usedStacks.contains(i)) {
                    continue;
                }

                boolean itemsCompatible = true;
                for (int item : currStack) {
                    if (this.instance.getStackConstraints()[item][i] != 1) {
                        itemsCompatible = false;
                    }
                }
                if (!itemsCompatible) {
                    continue;
                }
                for (int level = 0; level < this.instance.getStacks()[i].length; level++) {
                    if (level < currStack.size()) {
                        this.instance.getStacks()[i][level] = currStack.get(level);
                    }
                }
                usedStacks.add(i);
                break;
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

        HeuristicUtil.assignColRatingToEdgesNewWay(itemPairs, this.instance.getStackingConstraints());
        Collections.sort(itemPairs);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        ArrayList<ArrayList<Integer>> listsOfRemainingItems = new ArrayList<>();
        this.generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            itemPairs, remainingItems, itemPairPermutations, listsOfRemainingItems
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
     * Solves the SLP with an approach that recursively generates maximum cardinality matchings for lists of items.
     *
     * @param optimizeSolution - specifies whether the solution should be optimized or just valid
     * @return the solution generated by the heuristic
     */
    public Solution solve(boolean optimizeSolution) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            HeuristicUtil.generateStackingConstraintGraphNewWay(
                graph,
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getStacks(),
                this.instance.getStackConstraints()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

            ArrayList<Integer> items = new ArrayList<>();
            for (int item : this.instance.getItems()) {
                items.add(item);
            }
            this.threeCapApproachTwo(mcm);
            this.completeStackAssignmentsForRecursiveApproach();

            sol = new Solution(0, this.timeLimit, this.instance);
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
