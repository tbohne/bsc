package SP.constructive_heuristics;

import SP.representations.*;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
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
 * problems with a stack capacity of 2. The goal is to minimize the transport costs
 * while respecting all given constraints.
 *
 * @author Tim Bohne
 */
public class TwoCapHeuristic {

    private final Instance instance;
    private double startTime;
    private final double timeLimit;

    /**
     * Constructor
     *
     * @param instance  - instance of the stacking problem to be solved
     * @param timeLimit - time limit for the solving procedure
     */
    public TwoCapHeuristic(Instance instance, double timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Generates the complete bipartite graph consisting of an item partition and a stack partition.
     * Since the two partitions aren't necessarily equally sized which is expected by the used algorithm to compute
     * the minimum weight perfect matching, dummy items get introduced.
     * These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemPairs      - pairs of items that are assigned to a stack together
     * @param unmatchedItems - items that are assigned to their own stack
     * @return generated bipartite graph
     */
    private BipartiteGraph generateBipartiteGraphBetweenItemsAndStacks(
            List<MCMEdge> itemPairs, List<Integer> unmatchedItems
    ) {
        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> itemPartition = new HashSet<>();
        Set<String> stackPartition = new HashSet<>();

        GraphUtil.addVerticesForItemPairs(itemPairs, graph, itemPartition);
        GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, graph, itemPartition);
        GraphUtil.addVerticesForStacks(this.instance.getStacks(), graph, stackPartition);

        if (itemPartition.size() > stackPartition.size()) {
            System.out.println("Number of item pairs and unmatched items exceeds the" +
                " number of available stacks, instance cannot be solved.");
            return null;
        }
        List<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, itemPartition, stackPartition);
        GraphUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

        return new BipartiteGraph(itemPartition, stackPartition, graph);
    }


    /**
     * Encapsulates the stacking constraint graph generation.
     *
     * @return generated stacking constraint graph
     */
    private DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraph() {
        return GraphUtil.generateStackingConstraintGraph(
            this.instance.getItems(),
            this.instance.getStackingConstraints(),
            this.instance.getCosts(),
            (Integer.MAX_VALUE / this.instance.getItems().length),
            this.instance.getStacks()
        );
    }

    /**
     * Parses the given minimum weight perfect matching between items and stacks and assigns
     * the pairs and unmatched items to the stacks in the determined way.
     *
     * @param mwpm   - minimum weight perfect matching to be parsed
     * @param stacks - stacks the parsed items are going to be assigned to
     */
    private void parseAndAssignMinCostPerfectMatching(KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm, int[][] stacks) {
        for (Object edge : mwpm.getMatching().getEdges()) {
            if (edge.toString().contains("pair")) {
                int stack = GraphUtil.parseStackForPair((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemOneOfPairBasedOnMatching((DefaultWeightedEdge) edge);
                stacks[stack][0] = GraphUtil.parseItemTwoOfPairBasedOnMatching((DefaultWeightedEdge) edge);
            } else if (edge.toString().contains("item")) {
                int stack = GraphUtil.parseStack((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItem((DefaultWeightedEdge) edge);
            }
        }
    }

    /**
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of items and the second one consists of empty positions in stacks.
     * An edge between the partitions indicates that an item is compatible to the empty position.
     * The costs of the edges correspond to the savings that are generated if the item is assigned to the empty position.
     *
     * @param items          - list of items in the stacks
     * @param emptyPositions - list of empty positions in the stacks
     * @param costsBefore    - original costs for each item assignment
     * @param sol            - solution to be processed
     * @return generated bipartite post-processing graph
     */
    private BipartiteGraph generatePostProcessingGraph(
            List<Integer> items, List<StackPosition> emptyPositions, Map<Integer, Double> costsBefore, Solution sol
    ) {
        Graph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> itemPartition = new HashSet<>();
        Set<String> emptyPositionPartition = new HashSet<>();
        GraphUtil.addVerticesForUnmatchedItems(items, postProcessingGraph, itemPartition);
        GraphUtil.addVerticesForEmptyPositions(emptyPositions, postProcessingGraph, emptyPositionPartition);

        this.findCompatibleEmptyPositionsForItems(postProcessingGraph, items, emptyPositions, costsBefore, sol);
        return new BipartiteGraph(itemPartition, emptyPositionPartition, postProcessingGraph);
    }

    /**
     * Finds compatible empty positions for the items in the storage area and initiates the edge addition
     * for these pairs of items and empty positions to the post processing graph.
     *
     * @param postProcessingGraph - graph the edges are added to
     * @param items               - items to be connected to compatible empty positions
     * @param emptyPositions      - empty positions the compatible items get connected with
     * @param originalCosts       - current costs for each item assignment
     * @param sol                 - solution to be processed
     */
    private void findCompatibleEmptyPositionsForItems(
            Graph<String, DefaultWeightedEdge> postProcessingGraph, List<Integer> items, List<StackPosition> emptyPositions,
            Map<Integer, Double> originalCosts, Solution sol
    ) {
        for (int item : items) {
            for (StackPosition emptyPos : emptyPositions) {

                if (HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), item, emptyPos.getStackIdx())) {

                    int levelOfOtherSlot = emptyPos.getLevel() == 0 ? 1 : 0;

                    // other item in stack
                    if (sol.getFilledStacks()[emptyPos.getStackIdx()][levelOfOtherSlot] != -1) {
                        int otherItem = sol.getFilledStacks()[emptyPos.getStackIdx()][levelOfOtherSlot];
                        // item compatible with other item
                        if (HeuristicUtil.itemsStackableInAtLeastOneDirection(instance.getStackingConstraints(), item, otherItem)) {
                            GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, instance);
                        }
                        // no other item in stack
                    } else {
                        GraphUtil.addEdgeToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, instance);
                    }
                }
            }
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
     * @param sol - generated solution to be processed
     * @return resulting solution
     */
    private Solution postProcessing(Solution sol) {

        List<StackPosition> emptyPositions = HeuristicUtil.retrieveEmptyPositions(sol);
        List<Integer> items = sol.getAssignedItems();
        Map<Integer, Double> costsBefore = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());

        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(items, emptyPositions, costsBefore, sol);
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        HeuristicUtil.updateStackAssignments(maxSavingsMatching, postProcessingGraph, this.instance);
        this.instance.lowerItemsThatAreStackedInTheAir();
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);

        sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return sol;
    }

    /**
     * Solves the given instance of the stacking problem. The objective is to minimize the transport costs.
     *
     * Basic idea:
     *      - generate stacking constraint graph
     *      - compute MCM and interpret edges as item pairs
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *      - post processing
     *
     * @param postProcessing - determines whether or not the post-processing step should be executed
     *                         (allows to further focus on cost minimization)
     * @return solution generated by the heuristic
     */
    public Solution solve(boolean postProcessing) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 2) {
            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = this.generateStackingConstraintGraph();
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(
                stackingConstraintGraph
            );
            List<MCMEdge> itemPairs = GraphUtil.parseItemPairsFromMCM(itemMatching);
            List<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromPairs(itemPairs, this.instance.getItems());
            BipartiteGraph bipartiteGraph = this.generateBipartiteGraphBetweenItemsAndStacks(itemPairs, unmatchedItems);

            if (bipartiteGraph == null) { return sol; }

            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            this.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            this.instance.lowerItemsThatAreStackedInTheAir();
            sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
            sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();

            if (postProcessing) {
                System.out.println("costs before post processing: " + sol.getObjectiveValue());
                double bestSolutionCost = sol.computeCosts();

                sol = this.postProcessing(sol);
                while (sol.computeCosts() < bestSolutionCost) {
                    bestSolutionCost = sol.computeCosts();
                    sol = this.postProcessing(sol);
                }
                System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());
            }
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        sol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
        return sol;
    }
}
