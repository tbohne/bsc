package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class ThreeCapHeuristic {

    private final int NUMBER_OF_USED_EDGE_RATING_SYSTEMS = 5;

    private Instance instance;
    private double startTime;
    private int timeLimit;

    public ThreeCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Returns a list of two permutations of the unmatched items.
     * The permutations are created by sorting the items based on their row / edge rating.
     *
     * @param matchedItems - the items that are already matched
     * @return list of of permutations of the unmatched items
     */
    public ArrayList<List<Integer>> getUnmatchedItemPermutations(ArrayList<MCMEdge> matchedItems) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItems(matchedItems, this.instance.getItems()));
        HeuristicUtil.removeExceedingItemPairsFromMatchedItems(matchedItems, unmatchedItems, this.instance.getStacks());
        ArrayList<Integer> unmatchedItemsSortedByRowRating = HeuristicUtil.getUnmatchedItemsSortedByRowRating(unmatchedItems, this.instance.getStackingConstraints());
        ArrayList<Integer> unmatchedItemsSortedByColRating = HeuristicUtil.getUnmatchedItemsSortedByColRating(unmatchedItems, this.instance.getStackingConstraints());

        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();
        unmatchedItemPermutations.add(unmatchedItemsSortedByRowRating);
        unmatchedItemPermutations.add(unmatchedItemsSortedByColRating);

        return unmatchedItemPermutations;
    }

    /**
     * Returns a list of permutations of the item pairs.
     * The permutations are generated using different rating systems as basis for the sorting procedure.
     *
     * @param itemMatching - matching containing the item pairs
     * @return list of item pair permutations
     */
    public ArrayList<ArrayList<MCMEdge>> getItemPairPermutations(EdmondsMaximumCardinalityMatching itemMatching) {

        ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(itemMatching);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_USED_EDGE_RATING_SYSTEMS; i++) {
            ArrayList<MCMEdge> tmpItemPairs = HeuristicUtil.getCopyOfEdgeList(itemPairs);
            itemPairPermutations.add(tmpItemPairs);
        }
        HeuristicUtil.applyRatingSystemsToItemPairPermutations(itemPairPermutations, this.instance.getStackingConstraints());
        this.sortItemPairPermutationsBasedOnRatings(itemPairPermutations);

        return itemPairPermutations;
    }

    /**
     * Sorts each item pair permutation based on the assigned edge ratings.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public void sortItemPairPermutationsBasedOnRatings(ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        for (int i = 0; i < NUMBER_OF_USED_EDGE_RATING_SYSTEMS; i++) {
            Collections.sort(itemPairPermutations.get(i));
        }
    }

    /**
     * Adds the vertices for item triples, item pairs, unmatched items and stacks to the specified graph
     * and fills the partitions that define the bipartite graph.
     *
     * @param itemTriples - the list of item triples
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @param graph - the graph to be created
     * @param partitionOne - the first partition of the bipartite graph
     * @param partitionTwo - the second partition of the bipartite graph
     */
    public void addVertices(ArrayList<ArrayList<Integer>> itemTriples, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph, Set<String> partitionOne, Set<String> partitionTwo) {

        for (ArrayList<Integer> triple : itemTriples) {
                graph.addVertex("triple" + triple);
                partitionOne.add("triple" + triple);
            }
            for (MCMEdge pair : itemPairs) {
                graph.addVertex("pair" + pair);
                partitionOne.add("pair" + pair);
            }
            for (int item : unmatchedItems) {
                graph.addVertex("item" + item);
                partitionOne.add("item" + item);
            }
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                graph.addVertex("stack" + stack);
                partitionTwo.add("stack" + stack);
            }
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition
     * and computes the minimum cost perfect matching for this graph.
     * Since the two partitions aren't equally sized and the used algorithm to compute the MinCostPerfectMatching
     * expects a complete bipartite graph, there are dummy items that are used to make the graph complete bipartite.
     * These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemTriples - the list of item triples
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return MinCostPerfectMatching between items and stacks
     */
    public KuhnMunkresMinimalWeightBipartitePerfectMatching getMinCostPerfectMatching(
            ArrayList<ArrayList<Integer>> itemTriples,
            ArrayList<MCMEdge> itemPairs,
            ArrayList<Integer> unmatchedItems
    ) {
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();
        this.addVertices(itemTriples, itemPairs, unmatchedItems, graph, partitionOne, partitionTwo);
        ArrayList<Integer> dummyItems = HeuristicUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);

        HeuristicUtil.addEdgesForItemTriples(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

        return new KuhnMunkresMinimalWeightBipartitePerfectMatching(graph,partitionOne, partitionTwo);
    }

    /**
     * Computes item triples from the list of item pairs and the list of unmatched items.
     *
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return list of item compatible item triples
     */
    public ArrayList<ArrayList<Integer>> computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = HeuristicUtil.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            itemPairs, unmatchedItems, this.instance.getStackingConstraints()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTriples = HeuristicUtil.parseItemTripleMCM(mcm);
        return itemTriples;
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
     * Tries to merge item pairs by splitting up pairs and assigning both items
     * to other pairs to form completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<Integer>> mergeItemPairs(ArrayList<MCMEdge> itemPairs) {
        ArrayList<MCMEdge> itemPairRemovalList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> completelyFilledStacks = new ArrayList<>();
        this.generateCompletelyFilledStacks(itemPairs, itemPairRemovalList, completelyFilledStacks);
        return completelyFilledStacks;
    }

    /**
     * Generates the solution to the given instance of the SLP by applying the heuristic described in solve().
     *
     * @param itemMatching - matching containing the item pairs
     * @return the generated solution
     */
    public Solution generateSolution(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching) {

        Solution sol = new Solution();

        for (ArrayList<MCMEdge> itemPairPermutation : this.getItemPairPermutations(itemMatching)) {

            if ((System.currentTimeMillis() - startTime) / 1000.0 >= this.timeLimit) { break; }

            for (List<Integer> unmatchedItems : this.getUnmatchedItemPermutations(itemPairPermutation)) {

                ArrayList<ArrayList<Integer>> triples = this.computeCompatibleItemTriples(itemPairPermutation, (ArrayList<Integer>) unmatchedItems);
                unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

                DefaultUndirectedGraph graph = HeuristicUtil.generateStackingConstraintGraphNewWay(
                    HeuristicUtil.getArrayFromList((ArrayList<Integer>) unmatchedItems), this.instance.getStackingConstraints(),
                    this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
                );
                EdmondsMaximumCardinalityMatching pairs = new EdmondsMaximumCardinalityMatching(graph);
                ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(pairs);

                triples.addAll(this.mergeItemPairs(itemPairs));
                unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(triples, itemPairs, this.instance.getItems());
                graph = HeuristicUtil.generateStackingConstraintGraphNewWay(
                        HeuristicUtil.getArrayFromList((ArrayList<Integer>) unmatchedItems), this.instance.getStackingConstraints(),
                        this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
                );
                pairs = new EdmondsMaximumCardinalityMatching(graph);
                itemPairs = HeuristicUtil.parseItemPairMCM(pairs);
                unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(triples, itemPairs, this.instance.getItems());

                System.out.println("triples: " + triples.size());
                System.out.println("pairs: " + itemPairs.size());
                System.out.println("items: " + unmatchedItems.size());
                System.out.println("stacks: " + this.instance.getStacks().length);

                if (triples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) { continue; }

                KuhnMunkresMinimalWeightBipartitePerfectMatching matching = this.getMinCostPerfectMatching(
                    triples, itemPairs, (ArrayList<Integer>) unmatchedItems
                );

                HeuristicUtil.parseAndAssignMinCostPerfectMatching(matching, this.instance.getStacks());

                sol = new Solution(0, this.timeLimit, this.instance);
                sol.transformStackAssignmentIntoValidSolutionIfPossible();
                if (sol.isFeasible()) { return sol; }
            }
        }
        return sol;
    }

    /**
     * Solves the given instance of the SLP. The objective is to minimize the transport costs
     * which is achieved by computing a min-cost-perfect-matching in the end.
     *
     * The heuristic is based on the following major steps:
     *      - compute item triples from unmatched items
     *      - compute item pairs from still unmatched items
     *      - compute still unmatched items
     *      - create bipartite graph between items and stacks
     *      - compute min-cost-perfect matching
     *      - assign items according to edges of MCPM
     *
     * @return the solution generated by the heuristic
     */
    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = HeuristicUtil.generateStackingConstraintGraphNewWay(
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                Integer.MAX_VALUE / this.instance.getItems().length,
                this.instance.getStacks()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);

            sol = generateSolution(itemMatching);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 3.");
        }
        return sol;
    }
}
