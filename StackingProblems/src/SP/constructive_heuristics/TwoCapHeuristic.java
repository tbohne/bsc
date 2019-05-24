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

    private Instance instance;
    private double startTime;
    private double timeLimit;

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
    public BipartiteGraph generateBipartiteGraphBetweenItemsAndStacks(
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
    public DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraph() {
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
    public void parseAndAssignMinCostPerfectMatching(KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm, int[][] stacks) {
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
     * Solves the stacking problem with an approach that uses a maximum cardinality matching followed by a minimum
     * weight perfect matching to feasibly assign all items to the storage area while minimizing the costs.
     * Afterwards there's the option to start a post-processing of a solution which means that pairs of items
     * are separated and items get assigned to remaining empty stacks if that reduces the total costs.
     * Basic idea:
     *      - generate stacking constraint graph
     *      - compute MCM and interpret edges as item pairs
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *      - post-processing
     *
     * @param postProcessing - determines whether or not the post-processing step should be executed
     * @return the generated solution
     */
    public Solution solve(boolean postProcessing) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 2) {
            this.startTime = System.currentTimeMillis();
            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = this.generateStackingConstraintGraph();
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(
                stackingConstraintGraph
            );
            ArrayList<MCMEdge> itemPairs = GraphUtil.parseItemPairsFromMCM(itemMatching);
            ArrayList<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromPairs(
                itemPairs, this.instance.getItems()
            );
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
                sol = HeuristicUtil.postProcessing(sol, this.instance, this.startTime, this.timeLimit);
                while (sol.computeCosts() < bestSolutionCost) {
                    bestSolutionCost = sol.computeCosts();
                    sol = HeuristicUtil.postProcessing(sol, this.instance, this.startTime, this.timeLimit);
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
