package SP.constructive_heuristics;

import SP.representations.*;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import SP.util.RatingSystem;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

/**
 * Constructive heuristic to efficiently generate feasible solutions to stacking
 * problems with a stack capacity of 3. The goal is to minimize the transport costs
 * while respecting all given constraints.
 *
 * @author Tim Bohne
 */
public class ThreeCapHeuristic {

    private Instance instance;
    private double startTime;
    private int timeLimit;

    /**
     * Constructor
     *
     * @param instance  - the instance of the stacking problem to be solved
     * @param timeLimit - the time limit for the solving procedure
     */
    public ThreeCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Adds the vertices for item triples, item pairs, unmatched items and stacks to the specified graph
     * and fills the partitions that define the bipartite graph.
     *
     * @param itemTriples    - the list of item triples
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @param graph          - the graph to be created
     * @param partitionOne   - the first partition of the bipartite graph
     * @param partitionTwo   - the second partition of the bipartite graph
     */
    public void addVertices(ArrayList<ArrayList<Integer>> itemTriples, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph, Set<String> partitionOne, Set<String> partitionTwo) {

            GraphUtil.addVerticesForItemTriples(itemTriples, graph, partitionOne);
            GraphUtil.addVerticesForItemPairs(itemPairs, graph, partitionOne);
            GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, graph, partitionOne);
            GraphUtil.addVerticesForStacks(this.instance.getStacks(), graph, partitionTwo);
    }

    /**
     * Parses the given minimum weight perfect matching and assigns it to the given stacks.
     *
     * @param mwpm   - the minimum weight perfect matching to be parsed
     * @param stacks - the stacks the parsed items are going to be assigned to
     */
    public void parseAndAssignMinCostPerfectMatching(KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm, int[][] stacks) {
        for (Object edge : mwpm.getMatching().getEdges()) {
            if (edge.toString().contains("triple")) {
                int stack = GraphUtil.parseStackForTriple((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItemOneOfTriple((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemTwoOfTriple((DefaultWeightedEdge) edge);
                stacks[stack][0] = GraphUtil.parseItemThreeOfTriple((DefaultWeightedEdge) edge);
            } else if (edge.toString().contains("pair")) {
                int stack = GraphUtil.parseStackForPair((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItemOneOfPairBasedOnMatching((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemTwoOfPairBasedOnMatching((DefaultWeightedEdge) edge);
            } else if (edge.toString().contains("item")) {
                int stack = GraphUtil.parseStack((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItem((DefaultWeightedEdge) edge);
            }
        }
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition.
     * Since the two partitions aren't necessarily equally sized and the used algorithm to compute the
     * MinCostPerfectMatching expects a complete bipartite graph, there are dummy items that are used to make
     * the graph complete bipartite. These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemTriples    - the list of item triples
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return MinCostPerfectMatching between items and stacks
     */
    public BipartiteGraph generateBipartiteGraph(ArrayList<ArrayList<Integer>> itemTriples,
        ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {

            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
                DefaultWeightedEdge.class
            );
            Set<String> partitionOne = new HashSet<>();
            Set<String> partitionTwo = new HashSet<>();
            this.addVertices(itemTriples, itemPairs, unmatchedItems, graph, partitionOne, partitionTwo);
            ArrayList<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);

            GraphUtil.addEdgesForItemTriples(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

            return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Computes item triples from the list of item pairs and the list of unmatched items.
     *
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return list of compatible item triples
     */
    public ArrayList<ArrayList<Integer>> computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = GraphUtil.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            itemPairs, unmatchedItems, this.instance.getStackingConstraints()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTriples = GraphUtil.parseItemTripleFromMCM(mcm);
        return itemTriples;
    }

    /**
     * Generates item triples based on the given list of item pairs.
     * The first x item pairs get splitted and the resulting items are assigned to the remaining
     * itemPairs.size() - x pairs to build up item triples.
     * The maximum number of item pairs that could be splitted and theoretically reassigned to the remaining pairs is
     * itemPairs.size() / 3. However, because of the stacking constraints there should be more options to assign
     * the items to. Therefore x = itemPairs.size() / 3.4 is used. The 3.4 is the result of an experimental comparison
     * of the results for different x-values.
     *
     * @param itemPairs   - the pairs of items to be merged
     * @param itemTriples - the list of triples to be generated
     */
    public void mergeItemPairs(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<Integer>> itemTriples) {
        ArrayList<MCMEdge> splitPairs = new ArrayList<>();
        ArrayList<MCMEdge> assignPairs = new ArrayList<>();
        for (int i = 0; i < itemPairs.size(); i++) {
            if (i < Math.floor(itemPairs.size() / 3.4)) {
                splitPairs.add(itemPairs.get(i));
            } else {
                assignPairs.add(itemPairs.get(i));
            }
        }
        ArrayList<Integer> splittedItems = new ArrayList<>();
        for (MCMEdge splitPair : splitPairs) {
            splittedItems.add(splitPair.getVertexOne());
            splittedItems.add(splitPair.getVertexTwo());
        }
        itemTriples.addAll(this.computeCompatibleItemTriples(assignPairs, splittedItems));
    }

    /**
     * Applies the 3Cap rating system to the lists of item pairs using different deviation thresholds.
     * The rating systems are used to sort the lists.
     *
     * @param itemPairLists - the lists of item pairs to be rated
     * @param itemPairs     - the initial list of item pairs
     * @return the number of applied rating systems
     */
    public void applyRatingSystems(ArrayList<ArrayList<MCMEdge>> itemPairLists, ArrayList<MCMEdge> itemPairs) {

        // the first list (idx 0) should be unrated and unsorted
        int ratingSystemIdx = 1;

        for (int deviationThreshold = 65; deviationThreshold < 125; deviationThreshold += 5) {
            if (itemPairLists.size() == ratingSystemIdx) {
                itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairs));
            }
            RatingSystem.assignThreeCapPairRating(
                itemPairLists.get(ratingSystemIdx++),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                this.instance.getStacks(),
                deviationThreshold
            );
        }
    }

    /**
     * Sorts the lists of item pairs based on their ratings.
     *
     * @param itemPairLists - the lists of items pairs to be sorted
     */
    public void sortItemPairListsBasedOnRatings(ArrayList<ArrayList<MCMEdge>> itemPairLists) {
        for (int i = 0; i < itemPairLists.size(); i++) {
            // The first list is supposed to be unsorted.
            if (i != 0) {
                Collections.sort(itemPairLists.get(i));
            }
            Collections.reverse(itemPairLists.get(i));
        }
    }

    /**
     * Generates lists of completely filled stacks by merging the given item pairs to triples.
     * The given item pairs are sorted based on different rating systems which leads to a number
     * of different lists of item triples.
     *
     * @param itemPairs         - the list of item pairs to be merged in different ways
     * @param prioritizeRuntime - determines whether runtime is prioritized instead of solution quality
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> mergePairsToTriples(ArrayList<MCMEdge> itemPairs, boolean prioritizeRuntime) {
        ArrayList<ArrayList<ArrayList<Integer>>> listsOfItemTriples = new ArrayList<>();
        ArrayList<ArrayList<MCMEdge>> listsOfItemPairs = new ArrayList<>();
        listsOfItemPairs.add(itemPairs);

        // If the runtime is prioritized, the rating system, which in some cases can increases
        // the solution quality at the cost of a longer runtime, will not be used.
        if (!prioritizeRuntime) {
            this.applyRatingSystems(listsOfItemPairs, itemPairs);
        }
        this.sortItemPairListsBasedOnRatings(listsOfItemPairs);

        for (int i = 0; i < listsOfItemPairs.size(); i++) {
            if (i >= listsOfItemTriples.size()) {
                listsOfItemTriples.add(new ArrayList<>());
            }
            this.mergeItemPairs(listsOfItemPairs.get(i), listsOfItemTriples.get(i));
        }
        return listsOfItemTriples;
    }

    /**
     * Encapsulates the stacking constraint graph generation.
     *
     * @return the generated stacking constraint graph
     */
    public DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraph(int[] items) {
        return GraphUtil.generateStackingConstraintGraph(
            items,
            this.instance.getStackingConstraints(),
            this.instance.getCosts(),
            Integer.MAX_VALUE / this.instance.getItems().length,
            this.instance.getStacks()
        );
    }

    /**
     * Generates compatible item pairs that are stackable in at least one direction.
     *
     * @param items - the items to build up pairs from
     * @return the generated item pairs
     */
    public ArrayList<MCMEdge> generateItemPairs(int[] items) {
        DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = this.generateStackingConstraintGraph(items);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(
            stackingConstraintGraph
        );
        return GraphUtil.parseItemPairsFromMCM(itemMatching);
    }

    /**
     * Generates solutions to the stacking problem based on the different lists of item triples.
     *
     * Basic idea:
     *      - generate pairs via MCM
     *      - consider remaining items to be unmatched
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *
     * @param itemTripleLists - the different lists of item triples
     * @return the generated solutions based on the given lists of triples
     */
    public ArrayList<Solution> generateSolutionsBasedOnListsOfTriples(ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists) {

        ArrayList<Solution> solutions = new ArrayList<>();

        int cnt = 0;

        for (ArrayList<ArrayList<Integer>> itemTriples : itemTripleLists) {
            this.instance.resetStacks();
            ArrayList<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(itemTriples, this.instance.getItems());
            // items that are stored as pairs
            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));
            // items that are stored in their own stack
            unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(itemTriples, itemPairs, this.instance.getItems());
            // unable to assign the items feasibly to the given number of stacks
            if (itemTriples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) {
                continue;
            }
            BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(itemTriples, itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            this.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            Solution sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
//            System.out.println(cnt++ + ": " + sol.computeCosts());
            solutions.add(new Solution(sol));
        }

        return solutions;
    }

    /**
     * Adds the edges for item triples in the post-processing graph.
     * The costs for each edge correspond to the maximum savings for
     * moving an item of the triple to an empty stack.
     *
     * @param postProcessingGraph - the graph to add the edges to
     * @param itemTriples - the item triples to be connected to compatible empty stacks
     * @param emptyStacks - the the empty stacks to be connected to compatible item triples
     * @param originalCosts - the costs for the original item assignments
     */
    public void addEdgesForItemTriples(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        ArrayList<ArrayList<Integer>> itemTriples,
        ArrayList<String> emptyStacks,
        HashMap<Integer, Double> originalCosts
    ) {

        for (int triple = 0; triple < itemTriples.size(); triple++) {
            for (String emptyStack : emptyStacks) {

                DefaultWeightedEdge edge = postProcessingGraph.addEdge("triple" + itemTriples.get(triple), emptyStack);
                int stackIdx = Integer.parseInt(emptyStack.replace("stack", "").trim());
                double savings = 0.0;

                // all three items compatible
                if (this.instance.getCosts()[itemTriples.get(triple).get(0)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length
                    && this.instance.getCosts()[itemTriples.get(triple).get(1)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length
                    && this.instance.getCosts()[itemTriples.get(triple).get(2)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {

                        savings = HeuristicUtil.getSavingsForTriple(itemTriples, triple, stackIdx, originalCosts, this.instance.getCosts());

                // zero and one compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(0)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length
                    && this.instance.getCosts()[itemTriples.get(triple).get(1)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {

                        savings = HeuristicUtil.getSavingsForPair(
                            itemTriples.get(triple).get(0), itemTriples.get(triple).get(1), stackIdx, originalCosts, this.instance.getCosts()
                        );

                // one and two compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(1)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length
                    && this.instance.getCosts()[itemTriples.get(triple).get(2)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {

                        savings = HeuristicUtil.getSavingsForPair(
                            itemTriples.get(triple).get(1), itemTriples.get(triple).get(2), stackIdx, originalCosts, this.instance.getCosts()
                        );

                // zero and two compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(0)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length
                    && this.instance.getCosts()[itemTriples.get(triple).get(2)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {

                        savings = HeuristicUtil.getSavingsForPair(
                            itemTriples.get(triple).get(0), itemTriples.get(triple).get(2), stackIdx, originalCosts, this.instance.getCosts()
                        );

                // zero compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(0)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {
                    savings = HeuristicUtil.getSavingsForItem(stackIdx, originalCosts, itemTriples.get(triple).get(0), this.instance.getCosts());
                // one compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(1)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {
                    savings = HeuristicUtil.getSavingsForItem(stackIdx, originalCosts, itemTriples.get(triple).get(1), this.instance.getCosts());
                // two compatible
                } else if (this.instance.getCosts()[itemTriples.get(triple).get(2)][stackIdx] < Integer.MAX_VALUE / this.instance.getItems().length) {
                    savings = HeuristicUtil.getSavingsForItem(stackIdx, originalCosts, itemTriples.get(triple).get(2), this.instance.getCosts());
                }
                postProcessingGraph.setEdgeWeight(edge, savings);
            }
        }
    }

    /**
     * Generates the bipartite post-processing graph consisting of the item
     * tuples in one partition and the remaining empty stacks in another.
     *
     * @param emptyStacks - the remaining empty stacks in the storage area
     * @param itemTriples - the list of item triples in the storage area
     * @param itemPairs - the list of item pairs in the storage area
     * @param originalCosts - the costs for the original item assignments
     * @return the generated bipartite graph
     */
    public BipartiteGraph generatePostProcessingGraph(
        ArrayList<String> emptyStacks,
        ArrayList<ArrayList<Integer>> itemTriples,
        ArrayList<ArrayList<Integer>> itemPairs,
        HashMap<Integer, Double> originalCosts
    ) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForItemTriples(itemTriples, graph, partitionOne);
        GraphUtil.addVerticesForListOfItemPairs(itemPairs, graph, partitionOne);
        GraphUtil.addVerticesForEmptyStacks(emptyStacks, graph, partitionTwo);

        HeuristicUtil.addEdgesForItemPairs(graph, itemPairs, emptyStacks, originalCosts, this.instance);
        this.addEdgesForItemTriples(graph, itemTriples, emptyStacks, originalCosts);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    public ArrayList<Integer> getLevelsOfOtherSlots(int level) {
        ArrayList<Integer> levelsOfOtherSlots = new ArrayList<>();
        for (int i = 0; i < this.instance.getStackCapacity(); i++) {
            if (i != level) {
                levelsOfOtherSlots.add(i);
            }
        }
        return levelsOfOtherSlots;
    }

    // TODO: move to util and apply to 2Cap
    public void addEdgesForCompatibleItems(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        int item,
        StorageAreaPosition emptyPos,
        HashMap<Integer, Double> originalCosts
    ) {
        DefaultWeightedEdge edge = postProcessingGraph.addEdge("item" + item, "pos" + emptyPos);
        double savings = HeuristicUtil.getSavingsForItem(emptyPos.getStackIdx(), originalCosts, item, this.instance.getCosts());
        postProcessingGraph.setEdgeWeight(edge, savings);
    }

    public void findCompatibleEmptyPositionsForItems(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        ArrayList<Integer> items,
        ArrayList<StorageAreaPosition> emptyPositions,
        HashMap<Integer, Double> originalCosts,
        Solution sol
    ) {

        for (int item : items) {
            for (StorageAreaPosition emptyPos : emptyPositions) {

                // item compatible with stack
                if (this.instance.getCosts()[item][emptyPos.getStackIdx()] < Integer.MAX_VALUE / this.instance.getItems().length) {

                    // has always two entries
                    ArrayList<Integer> levelsOfOtherSlots = this.getLevelsOfOtherSlots(emptyPos.getLevel());

                    // 0 --> top level

                    // two other items in stack
                    if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        && sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {

                            int lowerItemOfPair;
                            int upperItemOfPair;
                            if (levelsOfOtherSlots.get(0) < levelsOfOtherSlots.get(1)) {
                                upperItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
                                lowerItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
                            } else {
                                upperItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
                                lowerItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
                            }

                            // pair stackable in both directions
                            if (this.instance.getStackingConstraints()[lowerItemOfPair][upperItemOfPair] == 1
                                && this.instance.getStackingConstraints()[upperItemOfPair][lowerItemOfPair] == 1) {
                                    if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInBothDirections(
                                        this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
                                    )) {
                                        this.addEdgesForCompatibleItems(postProcessingGraph, item, emptyPos, originalCosts);
                                    }
                            // pair stackable in one direction
                            } else {

                                if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInOneDirection(
                                    this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
                                )) {
                                    this.addEdgesForCompatibleItems(postProcessingGraph, item, emptyPos, originalCosts);
                                }
                            }
                    // one other item in stack
                    } else if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        || sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {

                        int levelOfOtherSlot;
                        if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1) {
                            levelOfOtherSlot = levelsOfOtherSlots.get(0);
                        } else {
                            levelOfOtherSlot = levelsOfOtherSlots.get(1);
                        }

                        int otherItem = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelOfOtherSlot];

                        // check pair for compatibility
                        if (this.instance.getStackingConstraints()[item][otherItem] == 1
                            || this.instance.getStackingConstraints()[otherItem][item] == 1) {

                                this.addEdgesForCompatibleItems(postProcessingGraph, item, emptyPos, originalCosts);
                        }

                    // no other item in stack
                    } else {
                        this.addEdgesForCompatibleItems(postProcessingGraph, item, emptyPos, originalCosts);
                    }
                }
            }
        }
    }

    public BipartiteGraph generatePostProcessingGraphNewWay(
        ArrayList<Integer> items,
        ArrayList<StorageAreaPosition> emptyPositions,
        HashMap<Integer, Double> originalCosts,
        Solution sol
    ) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(items, postProcessingGraph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(emptyPositions, postProcessingGraph, partitionTwo);

        this.findCompatibleEmptyPositionsForItems(postProcessingGraph, items, emptyPositions, originalCosts, sol);

        return new BipartiteGraph(partitionOne, partitionTwo, postProcessingGraph);
    }

    /**
     * Retrieves all the tuples of items from the storage area of the original solution.
     *
     * @param sol - the original solution
     * @param tupleSize - the size of the tuples to be retrieved
     * @return the list of item pairs
     */
    public ArrayList<ArrayList<Integer>> retrieveItemTuples(Solution sol, int tupleSize) {
        ArrayList<ArrayList<Integer>> itemTuples = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStorageArea().length; stack++) {
            ArrayList<Integer> stackEntries = new ArrayList<>();
            for (int entry : sol.getFilledStorageArea()[stack]) {
                stackEntries.add(entry);
            }
            ArrayList<Integer> itemTuple = new ArrayList<>();
            if (Collections.frequency(stackEntries, -1) == this.instance.getStackCapacity() - tupleSize) {
                for (int entry : stackEntries) {
                    if (entry != -1) {
                        itemTuple.add(entry);
                    }
                }
            }
            if (itemTuple.size() == tupleSize) {
                itemTuples.add(itemTuple);
            }
        }
        return itemTuples;
    }

    /**
     * Updates the stack assignments for the specified triple.
     *
     * @param itemOne - the first item of the pair
     * @param itemTwo - the second item of the pair
     * @param itemThree - the third item of the pair
     * @param stack - the target stack
     * @param costsBefore - the costs for the original item assignments
     */
    public void updateStackAssignmentsForTriples(int itemOne, int itemTwo, int itemThree, int stack, HashMap<Integer, Double> costsBefore) {

        // all three items compatible
        if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length
            && this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length
            && this.instance.getCosts()[itemThree][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {

                HeuristicUtil.updateAssignmentsForCompatibleTriple(itemOne, itemTwo, itemThree, stack, costsBefore, this.instance);
        // one and two compatible
        } else if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length
            && this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
                HeuristicUtil.updateAssignmentsForCompatiblePair(itemOne, stack, itemTwo, costsBefore, this.instance);
        // two and three compatible
        } else if (this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length
            && this.instance.getCosts()[itemThree][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
                HeuristicUtil.updateAssignmentsForCompatiblePair(itemTwo, stack, itemThree, costsBefore, this.instance);
        // one and three compatible
        } else if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length
            && this.instance.getCosts()[itemThree][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
                HeuristicUtil.updateAssignmentsForCompatiblePair(itemOne, stack, itemThree, costsBefore, this.instance);
        // one compatible
        } else if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemOne, this.instance.getStacks());
            this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemOne;
        // two compatible
        } else if (this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemTwo, this.instance.getStacks());
            this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemTwo;
        // three compatible
        } else if (this.instance.getCosts()[itemThree][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {
            HeuristicUtil.removeItemFromOutdatedPosition(itemThree, this.instance.getStacks());
            this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemThree;
        }
    }

    /**
     * Updates the stack assignments based on the matching that results in the maximum savings.
     *
     * @param maxSavingsMatching - matching that results in the maximum savings
     * @param originalCosts - hashmap containing the original costs for each item assignment
     */
    public void updateStackAssignments(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching, HashMap<Integer, Double> originalCosts
    ) {
        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            // triple edge
            if (edge.toString().contains("triple")) {
                int itemOne = GraphUtil.parseItemOneOfTriple(edge);
                int itemTwo = GraphUtil.parseItemTwoOfTriple(edge);
                int itemThree = GraphUtil.parseItemThreeOfTriple(edge);
                int stack = GraphUtil.parseStackForTriple(edge);
                this.updateStackAssignmentsForTriples(itemOne, itemTwo, itemThree, stack, originalCosts);
            // pair edge
            } else {
                int itemOne = GraphUtil.parseItemOneOfPair(edge);
                int itemTwo = GraphUtil.parseItemTwoOfPair(edge);
                int stack = GraphUtil.parseStackForPair(edge);
                HeuristicUtil.updateStackAssignmentsForPairs(itemOne, itemTwo, stack, originalCosts, this.instance);
            }
        }
    }

    /**
     * Restores the storage area of the specified solution.
     * Since the last generated solution is not necessarily the best one,
     * in many cases the storage area in the instance for the best original solution has to be restored.
     *
     * @param sol - the solution to restore the storage area from
     */
    public void restoreStorageAreaForBestOriginalSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStorageArea().length; i++) {
            this.instance.getStacks()[i] = sol.getFilledStorageArea()[i].clone();
        }
    }

    /**
     * This approach should be used in situations where the number of used stacks is irrelevant
     * and only the minimization of transport costs matters.
     *
     * Takes the generated solution and moves items from completely filled stacks to empty
     * stacks if it's possible to reduce the total costs by doing so.
     * Initially, a bipartite graph between item pairs and triples and empty stacks is generated.
     * An edge between the two partitions indicates that at least one of the items in the tuple is assignable to
     * the empty stack. The costs of the edge correspond to the resulting savings if the item that induces higher
     * savings is assigned to the empty stack. A maximum weight matching is computed on that graph to retrieve an
     * assignment that leads to a maximum cost reduction.
     *
     * @param sol - the original solution
     * @return the result of the post-processing procedure
     */
    public Solution postProcessing(Solution sol) {
        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        // Since the last generated solution is not necessarily the best one,
        // the stack assignments for the best solution have to be restored before post-processing.
        this.instance.resetStacks();
        this.restoreStorageAreaForBestOriginalSolution(sol);

        ArrayList<String> emptyStacks = HeuristicUtil.retrieveEmptyStacks(sol);
        HashMap<Integer, Double> originalCosts = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());
        ArrayList<ArrayList<Integer>> itemPairs = this.retrieveItemTuples(sol, 2);
        ArrayList<ArrayList<Integer>> itemTriples = this.retrieveItemTuples(sol, 3);

        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(
            emptyStacks, itemTriples, itemPairs, originalCosts
        );
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        System.out.println("moved items: " + maxSavingsMatching.getMatching().getEdges().size());

        this.updateStackAssignments(maxSavingsMatching, originalCosts);
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        sol.lowerItemsThatAreStackedInTheAir();
        System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());
        return sol;
    }

    public Solution postProcessingNewWay(Solution sol) {
        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        // Since the last generated solution is not necessarily the best one,
        // the stack assignments for the best solution have to be restored before post-processing.
        this.instance.resetStacks();
        this.restoreStorageAreaForBestOriginalSolution(sol);

        ArrayList<StorageAreaPosition> emptyPositions = HeuristicUtil.retrieveEmptyPositions(sol);
        ArrayList<Integer> items = sol.getAssignedItems();
        HashMap<Integer, Double> originalCosts = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());

        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraphNewWay(
            items, emptyPositions, originalCosts, sol
        );

        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        System.out.println("moved items: " + maxSavingsMatching.getMatching().getEdges().size());

        HeuristicUtil.updateStackAssignments(maxSavingsMatching, postProcessingGraph, this.instance);

        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        sol.lowerItemsThatAreStackedInTheAir();
        System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());
        return sol;

    }

    /**
     * Solves the given instance of the stacking problem. The objective is to minimize the transport costs.
     *
     * Basic idea:
     *      - generate item pairs via MCM
     *      - merge item pairs to triples based on different rating systems
     *      - compute unmatched items based on triples
     *      - generate pairs again via MCM
     *      - consider remaining items to be unmatched
     *      - generate bipartite graph (items, stacks) for each rating system
     *      - compute MWPM and interpret edges as stack assignments for each rating system
     *      - generate corresponding solutions
     *      - determine best solution based on costs
     *      - fix order of items inside the stacks
     *      - post-processing
     *
     * @param prioritizeRuntime - determines whether runtime is prioritized instead of solution quality
     * @param postProcessing - determines whether or not the post-processing step should be executed
     * @return the solution generated by the heuristic
     */
    public Solution solve(boolean prioritizeRuntime, boolean postProcessing) {

        Solution bestSol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();
            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(this.instance.getItems());

            // TODO: remove dbg logs
            ///////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////
            int cnt = 0;
            for (MCMEdge e : itemPairs) {
                if (this.instance.getStackingConstraints()[e.getVertexOne()][e.getVertexTwo()] == 1
                        && this.instance.getStackingConstraints()[e.getVertexTwo()][e.getVertexOne()] == 1) {
                    cnt++;
                }
            }
            System.out.println("pairs: " + itemPairs.size() + " both dir: " + cnt);
            ///////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////

            ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists = this.mergePairsToTriples(itemPairs, prioritizeRuntime);
            ArrayList<Solution> solutions = this.generateSolutionsBasedOnListsOfTriples(itemTripleLists);
            bestSol = HeuristicUtil.getBestSolution(solutions);

            bestSol.transformStackAssignmentsIntoValidSolutionIfPossible();
            bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
            
            if (postProcessing) {
//                bestSol = this.postProcessing(bestSol);
                bestSol = this.postProcessingNewWay(bestSol);
                bestSol.transformStackAssignmentsIntoValidSolutionIfPossible();
                bestSol.lowerItemsThatAreStackedInTheAir();
            }
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }

        return bestSol;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////// DEPRECATED //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns an item pair the given item can be assigned to based on a best-fit strategy.
     * For each triple that results from assigning the given item to a potential target pair,
     * the cost value for the cheapest feasible stack assignment is used to update the overall
     * minimum cost value. Finally, the target pair that leads to the cheapest assignment is chosen.
     *
     * @param items                - the list of items that are part of the instance
     * @param potentialTargetPairs - the list of item pairs the item could be assigned to
     * @param costs                - the matrix containing the costs of item-stack assignments
     * @param stacks               - the available stacks
     * @param item                 - the item to be assigned to a pair
     * @return
     */
    public static MCMEdge getTargetPairForItemBestFit(
            int[] items, ArrayList<MCMEdge> potentialTargetPairs, double[][] costs, int[][] stacks, int item
    ) {
        MCMEdge bestTargetPair = new MCMEdge(0, 0, 0);
        if (potentialTargetPairs.size() > 0) {
            bestTargetPair = potentialTargetPairs.get(0);
        }
        double minCost = Integer.MAX_VALUE / items.length;
        int costsForIncompatibleStack = Integer.MAX_VALUE / items.length;

        for (MCMEdge targetPair : potentialTargetPairs) {
            ArrayList<Double> costValues = new ArrayList<>();

            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[item][stack] < costsForIncompatibleStack
                        && costs[targetPair.getVertexOne()][stack] < costsForIncompatibleStack
                        && costs[targetPair.getVertexTwo()][stack] < costsForIncompatibleStack
                        ) {
                    costValues.add(costs[item][stack]
                            + costs[targetPair.getVertexOne()][stack]
                            + costs[targetPair.getVertexTwo()][stack]
                    );
                }
            }
            if (Collections.min(costValues) < minCost) {
                minCost = Collections.min(costValues);
                bestTargetPair = targetPair;
            }
        }
        return bestTargetPair;
    }

    /**
     * Assigns the items of the splitted pair to their most promising target pairs.
     *
     * @param potentialItemOnePairs  - list of pairs item one is assignable to
     * @param potentialItemTwoPairs  - list of pairs item two is assignable to
     * @param splitPair              - the pair to be splitted
     * @param completelyFilledStacks - the list of item triples
     * @param itemPairRemovalList    - the list of item pairs to be removed
     * @param itemOne                - the first item of the splitted pair
     * @param itemTwo                - the second item of the splitted pair
     */
    public void assignItemsToPairs(
            ArrayList<MCMEdge> potentialItemOnePairs, ArrayList<MCMEdge> potentialItemTwoPairs, MCMEdge splitPair,
            ArrayList<ArrayList<Integer>> completelyFilledStacks, ArrayList<MCMEdge> itemPairRemovalList, int itemOne, int itemTwo
    ) {
        boolean itemOneAssigned = false;
        boolean itemTwoAssigned = false;

        MCMEdge itemOneTargetPair = getTargetPairForItemBestFit(
                this.instance.getItems(), potentialItemOnePairs, this.instance.getCosts(), this.instance.getStacks(), itemOne
        );
        if (!(itemOneTargetPair.getVertexOne() == 0 && itemOneTargetPair.getVertexTwo() == 0)) {
            itemOneAssigned = true;
        }
        this.extendPotentialItemPairsForSecondItem(potentialItemOnePairs, itemOneTargetPair, itemTwo, potentialItemTwoPairs);
        MCMEdge itemTwoTargetPair = getTargetPairForItemBestFit(
                this.instance.getItems(), potentialItemTwoPairs, this.instance.getCosts(), this.instance.getStacks(), itemTwo
        );
        if (!(itemTwoTargetPair.getVertexOne() == 0 && itemTwoTargetPair.getVertexTwo() == 0)) {
            itemTwoAssigned = true;
        }
        if (itemOneAssigned && itemTwoAssigned) {
            HeuristicUtil.updateCompletelyFilledStacks(
                    itemPairRemovalList, itemOneTargetPair, itemTwoTargetPair, splitPair, itemOne, itemTwo, completelyFilledStacks
            );
        }
    }

    /**
     * Generates completely filled stacks from the list of item pairs.
     * Breaks up a pair and tries to assign both items two new pairs to build up completely filled stacks.
     *
     * @param itemPairs              - the list of item pairs
     * @param itemPairRemovalList    - the list of edges (item pairs) to be removed

     */
    public ArrayList<ArrayList<ArrayList<Integer>>> generateCompletelyFilledStacks(
            ArrayList<MCMEdge> itemPairs, ArrayList<MCMEdge> itemPairRemovalList, int numberOfItemPairPermutations
    ) {

        ArrayList<ArrayList<ArrayList<Integer>>> listsOfCompletelyFilledStacks = new ArrayList<>();
        ArrayList<ArrayList<MCMEdge>> itemPairLists = new ArrayList<>();

        this.applyRatingSystems(itemPairLists, itemPairs);
        this.sortItemPairListsBasedOnRatings(itemPairLists);

        for (int i = 0; i < numberOfItemPairPermutations; i++) {

            for (MCMEdge splitPair : itemPairLists.get(i)) {

                if (itemPairRemovalList.contains(splitPair)) { continue; }

                int itemOne = splitPair.getVertexOne();
                int itemTwo = splitPair.getVertexTwo();
                ArrayList<MCMEdge> potentialItemOnePairs = new ArrayList<>();
                ArrayList<MCMEdge> potentialItemTwoPairs = new ArrayList<>();

                for (MCMEdge potentialTargetPair : itemPairLists.get(i)) {

                    if (itemPairRemovalList.contains(potentialTargetPair)) { continue; }

                    if (splitPair != potentialTargetPair) {
                        int potentialTargetPairItemOne = potentialTargetPair.getVertexOne();
                        int potentialTargetPairItemTwo = potentialTargetPair.getVertexTwo();

                        if (HeuristicUtil.itemAssignableToPair(
                                itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo, this.instance.getStackingConstraints()
                        )) {
                            potentialItemOnePairs.add(potentialTargetPair);
                            continue;
                        }
                        if (HeuristicUtil.itemAssignableToPair(
                                itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo, this.instance.getStackingConstraints()
                        )) {
                            potentialItemTwoPairs.add(potentialTargetPair);
                            continue;
                        }
                    }
                }
                this.assignItemsToPairs(
                    potentialItemOnePairs, potentialItemTwoPairs, splitPair, listsOfCompletelyFilledStacks.get(i), itemPairRemovalList, itemOne, itemTwo
                );
            }
        }
        return listsOfCompletelyFilledStacks;
    }

    /**
     * Extends the potential item pairs for the second item of the splitted pair in the merge step
     * with the pairs from the first item that are not used and also compatible with the second item.
     *
     * @param potentialItemOnePairs - the list of potential item pairs the first item can be assigned to
     * @param itemOneTargetPair     - the pair the first item gets assigned to
     * @param itemTwo               - the item whose potential target pairs gets updated
     * @param potentialItemTwoPairs - the list of potential item pairs the second item can be assigned to
     */
    public void extendPotentialItemPairsForSecondItem(
            ArrayList<MCMEdge> potentialItemOnePairs, MCMEdge itemOneTargetPair, int itemTwo, ArrayList<MCMEdge> potentialItemTwoPairs
    ) {
        for (MCMEdge potentialPair : potentialItemOnePairs) {
            if (potentialPair != itemOneTargetPair && HeuristicUtil.itemAssignableToPair(
                    itemTwo, potentialPair.getVertexOne(), potentialPair.getVertexTwo(), this.instance.getStackingConstraints()
            )) {
                potentialItemTwoPairs.add(potentialPair);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
