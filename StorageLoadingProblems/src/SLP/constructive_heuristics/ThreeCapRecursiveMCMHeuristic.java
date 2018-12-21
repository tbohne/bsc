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
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;
    private ArrayList<ArrayList<Integer>> stackAssignments;
    private int previousNumberOfRemainingItems;
    private double startTime;

    public ThreeCapRecursiveMCMHeuristic(Instance instance) {
        this.instance = instance;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        this.stackAssignments = new ArrayList<>();
        this.previousNumberOfRemainingItems = this.instance.getItems().length;
    }

    public int getRandomValueInBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    public ArrayList<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }


    public void copyStackAssignment(int[][] init, int[][] copy) {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[0].length; j++) {
                copy[i][j] = init[i][j];
            }
        }
    }

    public boolean listContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){
            return true;
        }
        return false;
    }

    public EdmondsMaximumCardinalityMatching<String, DefaultEdge> getMCMForUnassignedItems(ArrayList<Integer> unassignedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int item : unassignedItems) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < unassignedItems.size(); i++) {
            for (int j = 0; j < unassignedItems.size(); j++) {
                if (unassignedItems.get(i) != unassignedItems.get(j) && this.instance.getStackingConstraints()[unassignedItems.get(i)][unassignedItems.get(j)] == 1
                        || this.instance.getStackingConstraints()[unassignedItems.get(j)][unassignedItems.get(i)] == 1) {

                    if (!graph.containsEdge("v" + unassignedItems.get(j), "v" + unassignedItems.get(i))) {
                        graph.addEdge("v" + unassignedItems.get(i), "v" + unassignedItems.get(j));
                    }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        return mcm;
    }

    public void generateBipartiteGraph(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            ArrayList<MCMEdge> edges,
            ArrayList<Integer> unmatchedItems,
            int numberOfUsedItemPairs
    ) {

        // adding the specified number of item pairs as nodes to the graph
        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            graph.addVertex("edge" + edges.get(i));
        }

        // adding all unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            for (int j = 0; j < unmatchedItems.size(); j++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done

                if (this.instance.getStackingConstraints()[edges.get(i).getVertexOne()][edges.get(i).getVertexTwo()] == 1) {
                    if (this.instance.getStackingConstraints()[edges.get(i).getVertexTwo()][unmatchedItems.get(j)] == 1
                            || this.instance.getStackingConstraints()[unmatchedItems.get(j)][edges.get(i).getVertexOne()] == 1) {

                        if (!graph.containsEdge("v" + unmatchedItems.get(j), "edge" + edges.get(i))) {
                            graph.addEdge("edge" + edges.get(i), "v" + unmatchedItems.get(j));
                        }
                    }
                } else {
                    if (this.instance.getStackingConstraints()[edges.get(i).getVertexOne()][unmatchedItems.get(j)] == 1
                            || this.instance.getStackingConstraints()[unmatchedItems.get(j)][edges.get(i).getVertexTwo()] == 1) {

                        if (!graph.containsEdge("v" + unmatchedItems.get(j), "edge" + edges.get(i))) {
                            graph.addEdge("edge" + edges.get(i), "v" + unmatchedItems.get(j));
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Integer> getCurrentListOfUnmatchedItems(int length, ArrayList<MCMEdge> edges, ArrayList<Integer> unassignedItems) {
        ArrayList<Integer> MCMItems = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            MCMItems.add(edges.get(i).getVertexOne());
            MCMItems.add(edges.get(i).getVertexTwo());
        }
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : unassignedItems) {
            if (!MCMItems.contains(item)) {
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

//        System.out.println("remaining items: " + remainingItems.size());

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
        this.generateBipartiteGraph(graph, itemPairs, unmatchedItems, numberOfEdgesToBeUsed);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
        HeuristicUtil.parseItemTripleMCM(currentStackAssignments, itemTriples);

        this.stackAssignments.addAll(currentStackAssignments);

        // COMPUTING COMPATIBLE ITEM PAIRS FROM REMAINING ITEMS
        unmatchedItems = this.getUnassignedItemsFromStorageAreaSnapshot(currentStackAssignments);
        EdmondsMaximumCardinalityMatching remainingItemPairs = this.getMCMForUnassignedItems(unmatchedItems);
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
        EdmondsMaximumCardinalityMatching mcm = this.getMCMForUnassignedItems(remainingItems);
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

    public Solution capThreeApproach(boolean optimizeSolution) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        HeuristicUtil.generateStackingConstraintGraph(graph, this.instance.getItems(), this.instance.getStackingConstraints());
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<Integer> items = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            items.add(item);
        }
        this.recursiveMCMApproach(mcm, this.instance.getStacks().length, items);
        this.completeStackAssignmentsForRecursiveApproach();

        Solution sol = new Solution(0, false, this.instance);
        sol.transformStackAssignmentIntoValidSolutionIfPossible();
        System.out.println("sol feasible: " + sol.isFeasible());
        System.out.println(sol.getNumberOfAssignedItems());

        return sol;
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
            sol = this.capThreeApproach(optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        }
        return sol;
    }
}
