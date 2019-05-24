package SP.constructive_heuristics;

import SP.representations.*;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import SP.util.RatingSystem;
import org.jgrapht.Graph;
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
    private double timeLimit;

    // configurable rating system parameters
    private int deviationThresholdLB;
    private int deviationThresholdUB;
    private int deviationThresholdStepSize;

    /**
     * Constructor
     *
     * @param instance                   - instance of stacking problem to be solved
     * @param timeLimit                  - time limit for the solving procedure
     * @param deviationThresholdLB       - lower bound for deviation threshold
     * @param deviationThresholdUB       - upper bound for deviation threshold
     * @param deviationThresholdStepSize - step size for increments of deviation threshold
     */
    public ThreeCapHeuristic(
        Instance instance, double timeLimit, int deviationThresholdLB, int deviationThresholdUB, int deviationThresholdStepSize
    ) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.deviationThresholdLB = deviationThresholdLB;
        this.deviationThresholdUB = deviationThresholdUB;
        this.deviationThresholdStepSize = deviationThresholdStepSize;
    }

    /**
     * Adds the vertices for item triples, item pairs, unmatched items and stacks to the specified graph
     * and fills the partitions. The items are part of one partitions and the stacks are part of the other.
     *
     * @param itemTriples    - item triples to be added as vertices
     * @param itemPairs      - item pairs to be added as vertices
     * @param unmatchedItems - unmatched items to be added as vertices
     * @param graph          - graph the vertices are added to
     * @param itemPartition  - partition in which the items are placed
     * @param stackPartition - partition in which the stacks are placed
     */
    public void addVerticesForBipartiteGraphBetweenItemsAndStacks(
        List<List<Integer>> itemTriples, List<MCMEdge> itemPairs, List<Integer> unmatchedItems,
        Graph<String, DefaultWeightedEdge> graph, Set<String> itemPartition, Set<String> stackPartition
    ) {
        GraphUtil.addVerticesForItemTriples(itemTriples, graph, itemPartition);
        GraphUtil.addVerticesForItemPairs(itemPairs, graph, itemPartition);
        GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, graph, itemPartition);
        GraphUtil.addVerticesForStacks(this.instance.getStacks(), graph, stackPartition);
    }

    /**
     * Parses the given minimum weight perfect matching between items and stacks and assigns the
     * triples, pairs and unmatched items to the stacks in the determined way.
     *
     * @param minCostPerfectMatching - minimum weight perfect matching to be parsed
     * @param stacks                 - stacks the items are going to be assigned to based on the matching
     */
    public void parseAndAssignMinCostPerfectMatching(
        KuhnMunkresMinimalWeightBipartitePerfectMatching minCostPerfectMatching, int[][] stacks
    ) {
        for (Object edge : minCostPerfectMatching.getMatching().getEdges()) {
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
     * Generates the complete bipartite graph consisting of an item partition and a stack partition.
     * Since the two partitions aren't necessarily equally sized which is expected by the used algorithm to compute
     * the minimum weight perfect matching, dummy items get introduced.
     * These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemTriples    - list of item triples
     * @param itemPairs      - list of item pairs
     * @param unmatchedItems - list of unmatched items
     * @return bipartite graph between items and stacks
     */
    public BipartiteGraph generateBipartiteGraphBetweenItemsAndStacks(
        List<List<Integer>> itemTriples, List<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems
    ) {
        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> itemPartition = new HashSet<>();
        Set<String> stackPartition = new HashSet<>();

        this.addVerticesForBipartiteGraphBetweenItemsAndStacks(
            itemTriples, itemPairs, unmatchedItems, graph, itemPartition, stackPartition
        );
        List<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, itemPartition, stackPartition);

        GraphUtil.addEdgesForItemTriples(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

        return new BipartiteGraph(itemPartition, stackPartition, graph);
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
    public void mergeItemPairs(List<MCMEdge> itemPairs, List<List<Integer>> itemTriples) {
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
    public void applyRatingSystems(List<List<MCMEdge>> itemPairLists, List<MCMEdge> itemPairs) {

        // the first list (idx 0) should be unrated and unsorted
        int ratingSystemIdx = 1;

        for (int deviationThreshold = this.deviationThresholdLB; deviationThreshold < this.deviationThresholdUB; deviationThreshold += this.deviationThresholdStepSize) {

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
    public void sortItemPairListsBasedOnRatings(List<List<MCMEdge>> itemPairLists) {
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
    public List<List<List<Integer>>> mergePairsToTriples(List<MCMEdge> itemPairs, boolean prioritizeRuntime) {
        List<List<List<Integer>>> listsOfItemTriples = new ArrayList<>();
        List<List<MCMEdge>> listsOfItemPairs = new ArrayList<>();
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
    public List<Solution> generateSolutionsBasedOnListsOfTriples(List<List<List<Integer>>> itemTripleLists) {

        List<Solution> solutions = new ArrayList<>();

        for (List<List<Integer>> itemTriples : itemTripleLists) {
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
            BipartiteGraph bipartiteGraph = this.generateBipartiteGraphBetweenItemsAndStacks(itemTriples, itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            this.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            Solution sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
            solutions.add(new Solution(sol));
        }
        return solutions;
    }

    /**
     * Returns a list of the remaining levels in a stack based on the specified level.
     *
     * @param level - level used to determine the remaining levels
     * @return list of remaining levels in the stack
     */
    public ArrayList<Integer> getLevelsOfOtherSlots(int level) {
        ArrayList<Integer> levelsOfOtherSlots = new ArrayList<>();
        for (int i = 0; i < this.instance.getStackCapacity(); i++) {
            if (i != level) {
                levelsOfOtherSlots.add(i);
            }
        }
        return levelsOfOtherSlots;
    }

    /**
     * Adds edges to the post-processing graph for stacks containing a single item.
     *
     * @param sol                 - the solution to be processed
     * @param emptyPos            - empty positions the compatible items get connected with
     * @param levelsOfOtherSlots  - the levels the items of the pair are positioned at
     * @param item                - the item potentially to be added to the stack
     * @param postProcessingGraph - graph the edges are added to
     * @param originalCosts       - the current costs for each item assignment
     */
    public void addEdgesForStacksWithSingleItem(
        Solution sol,
        StackPosition emptyPos,
        ArrayList<Integer> levelsOfOtherSlots,
        int item,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        HashMap<Integer, Double> originalCosts
    ) {

        int levelOfOtherSlot;
        if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1) {
            levelOfOtherSlot = levelsOfOtherSlots.get(0);
        } else {
            levelOfOtherSlot = levelsOfOtherSlots.get(1);
        }
        int otherItem = sol.getFilledStacks()[emptyPos.getStackIdx()][levelOfOtherSlot];

        // check pair for compatibility
        if (this.instance.getStackingConstraints()[item][otherItem] == 1
            || this.instance.getStackingConstraints()[otherItem][item] == 1) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
        }
    }

    /**
     * Adds edges to the post-processing graph for stacks already containing a pair of items.
     *
     * @param levelsOfOtherSlots  - the levels the items of the pair are positioned at
     * @param sol                 - the solution to be processed
     * @param emptyPos            - empty positions the compatible items get connected with
     * @param postProcessingGraph - graph the edges are added to
     * @param item                - the item potentially to be added to the stack
     * @param originalCosts       - the current costs for each item assignment
     */
    public void addEdgesForStacksFilledWithPairs(
        ArrayList<Integer> levelsOfOtherSlots,
        Solution sol,
        StackPosition emptyPos,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        int item,
        HashMap<Integer, Double> originalCosts
    ) {

        int lowerItemOfPair;
        int upperItemOfPair;

        if (levelsOfOtherSlots.get(0) < levelsOfOtherSlots.get(1)) {
            upperItemOfPair = sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
            lowerItemOfPair = sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
        } else {
            upperItemOfPair = sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
            lowerItemOfPair = sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
        }

        // pair stackable in both directions
        if (this.instance.getStackingConstraints()[lowerItemOfPair][upperItemOfPair] == 1
            && this.instance.getStackingConstraints()[upperItemOfPair][lowerItemOfPair] == 1) {

            if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInBothDirections(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        // pair stackable in one direction
        } else {
            if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInOneDirection(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        }
    }

    /**
     * Finds compatible empty positions for the items in the storage area and
     * initiates the edge addition for these items and empty positions
     * to the post-processing graph.
     *
     * @param postProcessingGraph - graph the edges are added to
     * @param items               - items to be connected to compatible empty positions
     * @param emptyPositions      - empty positions the compatible items get connected with
     * @param originalCosts       - the current costs for each item assignment
     * @param sol                 - the solution to be processed
     */
    public void findCompatibleEmptyPositionsForItemsAndAddEdges(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        List<Integer> items,
        ArrayList<StackPosition> emptyPositions,
        HashMap<Integer, Double> originalCosts,
        Solution sol
    ) {

        for (int item : items) {
            for (StackPosition emptyPos : emptyPositions) {

                // item compatible with stack
                if (this.instance.getCosts()[item][emptyPos.getStackIdx()] < Integer.MAX_VALUE / this.instance.getItems().length) {

                    // has always two entries
                    ArrayList<Integer> levelsOfOtherSlots = this.getLevelsOfOtherSlots(emptyPos.getLevel());

                    // two other items in stack
                    if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        && sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgesForStacksFilledWithPairs(
                                levelsOfOtherSlots, sol, emptyPos, postProcessingGraph, item, originalCosts
                            );
                    // one other item in stack
                    } else if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        || sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgesForStacksWithSingleItem(
                                sol, emptyPos, levelsOfOtherSlots, item, postProcessingGraph, originalCosts
                            );
                    // no other item in stack
                    } else {
                        GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
                    }
                }
            }
        }
    }

    /**
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of items and the second one consists of empty positions in stacks.
     * An edge between the partitions means that an item is compatible to the empty position.
     * The costs of the edges correspond to the savings that are generated if the item is assigned to the empty position.
     *
     * @param items          - list of items in the storage area
     * @param emptyPositions - list of empty positions in the storage area
     * @param originalCosts  - original costs for each item assignment
     * @param sol            - solution to be processed
     * @return the generated bipartite graph
     */
    public BipartiteGraph generatePostProcessingGraph(
        List<Integer> items,
        ArrayList<StackPosition> emptyPositions,
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
        this.findCompatibleEmptyPositionsForItemsAndAddEdges(postProcessingGraph, items, emptyPositions, originalCosts, sol);

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
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            ArrayList<Integer> stackEntries = new ArrayList<>();
            for (int entry : sol.getFilledStacks()[stack]) {
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
     * Restores the storage area of the specified solution.
     * Since the last generated solution is not necessarily the best one,
     * in many cases the storage area in the instance for the best original solution has to be restored.
     *
     * @param sol - the solution to restore the storage area from
     */
    public void restoreStorageAreaForBestOriginalSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            this.instance.getStacks()[i] = sol.getFilledStacks()[i].clone();
        }
    }

    /**
     * This approach should be used in situations where the number of used stacks is irrelevant
     * and only the minimization of transport costs matters.
     *
     * Takes the generated solution and moves items to empty positions if it's possible to
     * reduce the total costs by doing so.
     * Initially, a bipartite graph between items and empty positions is generated.
     * An edge between the two partitions indicates that an item is assignable to the empty position.
     * The costs of the edge correspond to the resulting savings if the item is assigned to the empty position.
     * A maximum weight matching is computed on that graph to retrieve an assignment that leads
     * to a maximum cost reduction.
     *
     * @param sol - the original solution
     * @return the result of the post-processing procedure
     */
    public Solution postProcessing(Solution sol) {
//        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        ArrayList<StackPosition> emptyPositions = HeuristicUtil.retrieveEmptyPositions(sol);
        List<Integer> items = sol.getAssignedItems();
        HashMap<Integer, Double> originalCosts = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());
        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(items, emptyPositions, originalCosts, sol);
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

//        System.out.println("moved items: " + maxSavingsMatching.getMatching().getEdges().size());
        HeuristicUtil.updateStackAssignments(maxSavingsMatching, postProcessingGraph, this.instance);
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        sol.lowerItemsThatAreStackedInTheAir();
        sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
//        System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());
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
//            System.out.println("pairs: " + itemPairs.size() + " both dir: " + cnt);
            ///////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////

            List<List<List<Integer>>> itemTripleLists = this.mergePairsToTriples(itemPairs, prioritizeRuntime);
            List<Solution> solutions = this.generateSolutionsBasedOnListsOfTriples(itemTripleLists);
            bestSol = HeuristicUtil.getBestSolution(solutions);

            bestSol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
            bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
            bestSol.lowerItemsThatAreStackedInTheAir();

            if (postProcessing) {
                System.out.println("costs before post processing: " + bestSol.getObjectiveValue());
                // Since the last generated solution is not necessarily the best one,
                // the stack assignments for the best solution have to be restored before post-processing.
                this.instance.resetStacks();
                this.restoreStorageAreaForBestOriginalSolution(bestSol);

                double bestSolutionCost = bestSol.computeCosts();
                bestSol = this.postProcessing(bestSol);
                while (bestSol.computeCosts() < bestSolutionCost) {
                    this.instance.resetStacks();
                    this.restoreStorageAreaForBestOriginalSolution(bestSol);
                    bestSolutionCost = bestSol.computeCosts();
                    bestSol = this.postProcessing(bestSol);
                }
            }
            System.out.println("final costs: " + bestSol.computeCosts() + " still feasible? " + bestSol.isFeasible());
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
        return bestSol;
    }
}
