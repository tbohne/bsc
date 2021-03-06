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

    private final Instance instance;
    private double startTime;
    private final double timeLimit;

    // configurable rating system parameters
    private final int deviationThresholdLB;
    private final int deviationThresholdUB;
    private final int deviationThresholdStepSize;
    private final float penaltyFactor;

    // configurable divisor used in merge step
    private final float splitPairsDivisor;

    /**
     * Constructor
     *
     * @param instance                   - instance of stacking problem to be solved
     * @param timeLimit                  - time limit for the solving procedure
     * @param deviationThresholdLB       - lower bound for deviation threshold
     * @param deviationThresholdUB       - upper bound for deviation threshold
     * @param deviationThresholdStepSize - step size for increments of deviation threshold
     * @param splitPairsDivisor          - divisor used in merge step to determine the number of pairs to be separated
     */
    public ThreeCapHeuristic(
        Instance instance, double timeLimit, int deviationThresholdLB, int deviationThresholdUB,
        int deviationThresholdStepSize, float splitPairsDivisor, float penaltyFactor
    ) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.deviationThresholdLB = deviationThresholdLB;
        this.deviationThresholdUB = deviationThresholdUB;
        this.deviationThresholdStepSize = deviationThresholdStepSize;
        this.splitPairsDivisor = splitPairsDivisor;
        this.penaltyFactor = penaltyFactor;
    }

    /**
     * Solves the given instance of the stacking problem. The objective is to minimize the transport costs.
     *
     * Basic idea:
     *      - generate item pairs via MCM
     *      - merge item pairs to triples based on different rating systems
     *      - determine unmatched items based on triples
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
     * @param postProcessing    - determines whether or not the post-processing step should be executed
     *                            (allows to further focus on cost minimization)
     * @return solution generated by the heuristic
     */
    public Solution solve(boolean prioritizeRuntime, boolean postProcessing) {

        Solution bestSol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();

            List<MCMEdge> itemPairs = this.generateItemPairs(this.instance.getItems());
            List<List<List<Integer>>> itemTripleLists = this.mergePairsToTriples(itemPairs, prioritizeRuntime);
            List<Solution> solutions = this.generateSolutionsBasedOnListsOfTriples(itemTripleLists);

            bestSol = HeuristicUtil.getBestSolution(solutions);
            bestSol.sortItemsInStacksBasedOnTransitiveStackingConstraints();

            if (postProcessing) {
                // Since the last generated solution is not necessarily the best one,
                // the stack assignments for the best solution have to be restored before post-processing.
                this.instance.resetStacks();
                this.restoreStackAssignmentsForBestOriginalSolution(bestSol);

                double bestSolutionCost = bestSol.computeCosts();
                bestSol = this.postProcessing(bestSol);

                while (bestSol.computeCosts() < bestSolutionCost) {
                    bestSolutionCost = bestSol.computeCosts();
                    bestSol = this.postProcessing(bestSol);
                }
            }
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
        return bestSol;
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
    private void addVerticesForBipartiteGraphBetweenItemsAndStacks(
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
    private void parseAndAssignMinCostPerfectMatching(
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
                stacks[stack][2] = GraphUtil.parseItemOneOfPair((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemTwoOfPair((DefaultWeightedEdge) edge);
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
    private BipartiteGraph generateBipartiteGraphBetweenItemsAndStacks(
        List<List<Integer>> itemTriples, List<MCMEdge> itemPairs, List<Integer> unmatchedItems
    ) {
        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> itemPartition = new HashSet<>();
        Set<String> stackPartition = new HashSet<>();

        this.addVerticesForBipartiteGraphBetweenItemsAndStacks(
            itemTriples, itemPairs, unmatchedItems, graph, itemPartition, stackPartition
        );
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, itemPartition, stackPartition);

        GraphUtil.addEdgesBetweenItemTriplesAndStacks(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesBetweenItemPairsAndStacks(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesBetweenUnmatchedItemsAndStacks(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesBetweenDummyItemsAndStacks(graph, dummyItems, this.instance.getStacks());

        return new BipartiteGraph(itemPartition, stackPartition, graph);
    }

    /**
     * Computes item triples from the list of item pairs and the list of unmatched items.
     *
     * @param itemPairs      - list of item pairs
     * @param unmatchedItems - list of unmatched items
     * @return list of compatible item triples
     */
    private List<List<Integer>> computeCompatibleItemTriples(List<MCMEdge> itemPairs, List<Integer> unmatchedItems) {
        Graph<String, DefaultEdge> graph = GraphUtil.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            itemPairs, unmatchedItems, this.instance.getStackingConstraints()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        return GraphUtil.parseItemTriplesFromMCM(mcm);
    }

    /**
     * Generates item triples based on the given list of item pairs.
     * The first x item pairs get separated and the resulting items are assigned to the remaining
     * itemPairs.size() - x pairs to build up item triples.
     *
     * @param itemPairs   - pairs of items to be merged
     * @param itemTriples - list of triples to be filled
     */
    private void mergeItemPairs(List<MCMEdge> itemPairs, List<List<Integer>> itemTriples) {
        List<MCMEdge> splitPairs = new ArrayList<>();
        List<MCMEdge> assignPairs = new ArrayList<>();
        for (int i = 0; i < itemPairs.size(); i++) {
            if (i < Math.floor(itemPairs.size() / this.splitPairsDivisor)) {
                splitPairs.add(itemPairs.get(i));
            } else {
                assignPairs.add(itemPairs.get(i));
            }
        }
        List<Integer> separatedItems = new ArrayList<>();
        for (MCMEdge splitPair : splitPairs) {
            separatedItems.add(splitPair.getVertexOne());
            separatedItems.add(splitPair.getVertexTwo());
        }
        itemTriples.addAll(this.computeCompatibleItemTriples(assignPairs, separatedItems));
    }

    /**
     * Applies the rating system to the lists of item pairs using different deviation thresholds.
     * The rating systems are used to sort the lists.
     *
     * @param itemPairLists - lists of item pairs to be rated
     * @param itemPairs     - initial list of item pairs
     */
    private void applyRatingSystems(List<List<MCMEdge>> itemPairLists, List<MCMEdge> itemPairs) {

        // the first list (idx 0) should be unrated and unsorted
        int ratingSystemIdx = 1;

        for (int deviationThreshold = this.deviationThresholdLB; deviationThreshold < this.deviationThresholdUB;
            deviationThreshold += this.deviationThresholdStepSize) {

                if (itemPairLists.size() == ratingSystemIdx) {
                    itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairs));
                }
                RatingSystem.assignPairRating(
                    itemPairLists.get(ratingSystemIdx++),
                    this.instance.getStackingConstraints(),
                    this.instance.getCosts(),
                    this.instance.getStacks(),
                    deviationThreshold,
                    this.penaltyFactor
                );
        }
    }

    /**
     * Sorts the lists of item pairs based on their ratings.
     *
     * @param itemPairLists - lists of items pairs to be sorted
     */
    private void sortItemPairListsBasedOnRatings(List<List<MCMEdge>> itemPairLists) {
        for (int i = 0; i < itemPairLists.size(); i++) {
            // The first list is supposed to be unsorted.
            if (i != 0) {
                Collections.sort(itemPairLists.get(i));
            }
            Collections.reverse(itemPairLists.get(i));
        }
    }

    /**
     * Generates lists of item triples by merging the given item pairs.
     * The given item pairs are sorted based on different rating systems which leads to a number
     * of different lists of item triples.
     *
     * @param itemPairs         - list of item pairs to be merged in different ways
     * @param prioritizeRuntime - determines whether runtime is prioritized instead of solution quality
     */
    private List<List<List<Integer>>> mergePairsToTriples(List<MCMEdge> itemPairs, boolean prioritizeRuntime) {

        List<List<List<Integer>>> listsOfItemTriples = new ArrayList<>();
        List<List<MCMEdge>> listsOfItemPairs = new ArrayList<>();
        listsOfItemPairs.add(itemPairs);

        // If the runtime is prioritized, the rating system, which improves the solution quality
        // in many cases at the cost of a longer runtime, will not be used.
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
     * @return generated stacking constraint graph
     */
    private Graph<String, DefaultEdge> generateStackingConstraintGraph(int[] items) {
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
     * @param items - items to build up pairs from
     * @return generated item pairs
     */
    private List<MCMEdge> generateItemPairs(int[] items) {
        Graph<String, DefaultEdge> stackingConstraintGraph = this.generateStackingConstraintGraph(items);
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
     *
     * @param itemTripleLists - different lists of item triples
     * @return generated solutions based on the given lists of triples
     */
    private List<Solution> generateSolutionsBasedOnListsOfTriples(List<List<List<Integer>>> itemTripleLists) {

        List<Solution> solutions = new ArrayList<>();

        for (List<List<Integer>> itemTriples : itemTripleLists) {
            this.instance.resetStacks();
            List<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(itemTriples, this.instance.getItems());
            // items that are stored as pairs
            List<MCMEdge> itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));
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
            this.instance.lowerItemsThatAreStackedInTheAir();
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
    private List<Integer> getLevelsOfOtherSlots(int level) {
        List<Integer> levelsOfOtherSlots = new ArrayList<>();
        for (int i = 0; i < this.instance.getStackCapacity(); i++) {
            if (i != level) {
                levelsOfOtherSlots.add(i);
            }
        }
        return levelsOfOtherSlots;
    }

    /**
     * Adds an edge to the post-processing graph for stacks containing a single item.
     *
     * @param sol                 - solution to be processed
     * @param emptyPos            - empty position the compatible items get connected with
     * @param levelsOfOtherSlots  - levels the items of the pair are positioned at
     * @param item                - item potentially to be added to the stack
     * @param postProcessingGraph - graph the edge is added to
     * @param originalCosts       - current costs for each item assignment
     */
    private void addEdgeForStacksWithSingleItemToPostProcessingGraph(
        Solution sol, StackPosition emptyPos, List<Integer> levelsOfOtherSlots, int item,
        Graph<String, DefaultWeightedEdge> postProcessingGraph, Map<Integer, Double> originalCosts
    ) {
        int levelOfOtherSlot = sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1 ?
            levelsOfOtherSlots.get(0) : levelsOfOtherSlots.get(1);

        int otherItem = sol.getFilledStacks()[emptyPos.getStackIdx()][levelOfOtherSlot];

        // check pair for compatibility
        if (HeuristicUtil.pairStackableInAtLeastOneDirection(this.instance.getStackingConstraints(), item, otherItem)) {
            GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
        }
    }

    /**
     * Adds an edge to the post-processing graph for stacks already containing a pair of items.
     *
     * @param levelsOfOtherSlots  - levels the items of the pair are positioned at
     * @param sol                 - solution to be processed
     * @param emptyPos            - empty position the compatible items get connected with
     * @param postProcessingGraph - graph the edges are added to
     * @param item                - item potentially to be added to the stack
     * @param originalCosts       - current costs for each item assignment
     */
    private void addEdgeForStacksFilledWithPairsToPostProcessingGraph(
        List<Integer> levelsOfOtherSlots, Solution sol, StackPosition emptyPos,
        Graph<String, DefaultWeightedEdge> postProcessingGraph, int item, Map<Integer, Double> originalCosts
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
        if (HeuristicUtil.pairStackableInBothDirections(lowerItemOfPair, upperItemOfPair, this.instance.getStackingConstraints())) {
            if (HeuristicUtil.itemAssignableToPairStackableInBothDirections(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        // pair stackable in one direction
        } else {
            if (HeuristicUtil.itemAssignableToPairStackableInOneDirection(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        }
    }

    /**
     * Finds compatible empty positions for the items in the storage area and initiates the edge
     * addition for these items and empty positions to the post-processing graph.
     *
     * @param postProcessingGraph - graph the edges are added to
     * @param items               - items to be connected to compatible empty positions
     * @param emptyPositions      - empty positions the compatible items get connected with
     * @param originalCosts       - current costs for each item assignment
     * @param sol                 - solution to be processed
     */
    private void findCompatibleEmptyPositionsForItemsAndAddEdges(
        Graph<String, DefaultWeightedEdge> postProcessingGraph, List<Integer> items,
        List<StackPosition> emptyPositions, Map<Integer, Double> originalCosts, Solution sol
    ) {
        for (int item : items) {
            for (StackPosition emptyPos : emptyPositions) {

                if (HeuristicUtil.itemCompatibleWithStack(this.instance.getCosts(), item, emptyPos.getStackIdx())) {
                    // has always two entries
                    List<Integer> levelsOfOtherSlots = this.getLevelsOfOtherSlots(emptyPos.getLevel());

                    // two other items in stack
                    if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        && sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgeForStacksFilledWithPairsToPostProcessingGraph(
                                levelsOfOtherSlots, sol, emptyPos, postProcessingGraph, item, originalCosts
                            );
                    // one other item in stack
                    } else if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        || sol.getFilledStacks()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgeForStacksWithSingleItemToPostProcessingGraph(
                                sol, emptyPos, levelsOfOtherSlots, item, postProcessingGraph, originalCosts
                            );
                    // no other item in stack
                    } else {
                        GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
                    }
                }
            }
        }
    }

    /**
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of items and the second one consists of empty positions in stacks.
     * An edge between the partitions indicates that an item is compatible with the empty position.
     * The costs of the edges correspond to the savings that are generated if the item is assigned to the empty position.
     *
     * @param items          - list of items in the stacks
     * @param emptyPositions - list of empty positions in the stacks
     * @param originalCosts  - original costs for each item assignment
     * @param sol            - solution to be processed
     * @return generated bipartite post-processing graph
     */
    private BipartiteGraph generatePostProcessingGraph(
        List<Integer> items, List<StackPosition> emptyPositions, Map<Integer, Double> originalCosts, Solution sol
    ) {
        Graph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> itemPartition = new HashSet<>();
        Set<String> emptyPositionPartition = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(items, postProcessingGraph, itemPartition);
        GraphUtil.addVerticesForEmptyPositions(emptyPositions, postProcessingGraph, emptyPositionPartition);
        this.findCompatibleEmptyPositionsForItemsAndAddEdges(postProcessingGraph, items, emptyPositions, originalCosts, sol);

        return new BipartiteGraph(itemPartition, emptyPositionPartition, postProcessingGraph);
    }

    /**
     * Restores the stack assignments of the specified solution.
     * Since the last generated solution is not necessarily the best one,
     * in many cases the stack assignments in the instance for the best original solution has to be restored.
     *
     * @param sol - solution to restore the stack assignments from
     */
    private void restoreStackAssignmentsForBestOriginalSolution(Solution sol) {
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
     * @param sol - original solution to be processed
     * @return resulting solution
     */
    @SuppressWarnings("Duplicates")
    private Solution postProcessing(Solution sol) {
        List<StackPosition> emptyPositions = HeuristicUtil.retrieveEmptyPositions(sol);
        List<Integer> items = sol.getAssignedItems();
        Map<Integer, Double> originalCosts = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());

        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(items, emptyPositions, originalCosts, sol);

        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        HeuristicUtil.updateStackAssignmentsInPostProcessing(maxSavingsMatching, postProcessingGraph, this.instance);
        this.instance.lowerItemsThatAreStackedInTheAir();
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return sol;
    }
}
