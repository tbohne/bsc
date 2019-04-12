package SP.constructive_heuristics;

import SP.representations.BipartiteGraph;
import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    private int timeLimit;

    /**
     * Constructor
     *
     * @param instance  - the instance of the stacking problem to be solved
     * @param timeLimit - the time limit for the solving procedure
     */
    public TwoCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Fixes the order of items inside the stacks based on the stacking constraints.
     */
    public void fixOrderInStacks() {
        for (int[] stack : this.instance.getStacks()) {
            if (stack[0] != -1 && stack[1] != -1) {
                if (this.instance.getStackingConstraints()[stack[0]][stack[1]] != 1) {
                    int tmp = stack[0];
                    stack[0] = stack[1];
                    stack[1] = tmp;
                }
            }
        }
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition.
     * Since the two partitions aren't necessarily equally sized and the used algorithm to compute the
     * Min-Cost-Perfect-Matching expects a complete bipartite graph, there are dummy items that are used to
     * make the graph complete bipartite. These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemPairs      - the pairs of items that are assigned to a stack together
     * @param unmatchedItems - the items that are assigned to their own stack
     * @return the generated bipartite graph
     */
    public BipartiteGraph generateBipartiteGraph(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForItemPairs(itemPairs, graph, partitionOne);
        GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, graph, partitionOne);
        GraphUtil.addVerticesForStacks(this.instance.getStacks(), graph, partitionTwo);
        ArrayList<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);

        GraphUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Returns the maximum savings for the specified pair.
     *
     * @param itemPairs - the list of item pairs
     * @param pairIdx - the index of the pair to be considered
     * @param costMatrix - the matrix containing the costs for each item-stack-assignment
     * @param stackIdx - the index of the considered stack
     * @param costsBefore - the original costs for each item assignment
     * @return the savings for the specified pair
     */
    public double getSavingsForPair(
        ArrayList<MCMEdge> itemPairs, int pairIdx, double[][] costMatrix, int stackIdx, HashMap<Integer, Double> costsBefore
    ) {
        int itemOne = itemPairs.get(pairIdx).getVertexOne();
        int itemTwo = itemPairs.get(pairIdx).getVertexTwo();
        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        return savingsItemOne > savingsItemTwo ? savingsItemOne : savingsItemTwo;
    }

    /**
     * Returns the savings for the specified item.
     *
     * @param costMatrix - the matrix containing the costs for each item-stack-assignment
     * @param stackIdx - the index of the considered stack
     * @param costsBefore - the original costs for each item assignment
     * @param item - the item the savings are computed for
     * @return the savings for the specified item
     */
    public double getSavingsForItem(double[][] costMatrix, int stackIdx, HashMap<Integer, Double> costsBefore, int item) {
        double costs = costMatrix[item][stackIdx];
        return costsBefore.get(item) - costs;
    }

    /**
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of item-pairs and the second one consists of empty stacks.
     * An edge between the partitions means that at least one item of the pair is compatible to the empty stack.
     * The costs of the edges correspond to the maximum savings that are generated if an item of the pair is
     * assigned to the empty stack.
     *
     * @param itemPairs - the list of item pairs
     * @param emptyStacks - the list of remaining empty stacks
     * @param costMatrix - the matrix containing the costs for each item-stack-assignment
     * @param items - the array containing the instance's items
     * @param costsBefore - the original costs for each item assignment
     * @return the generated bipartite graph
     */
    public BipartiteGraph generatePostProcessingGraph(
        ArrayList<MCMEdge> itemPairs,
        ArrayList<String> emptyStacks,
        double[][] costMatrix,
        int[] items,
        HashMap<Integer,
        Double> costsBefore
    ) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForItemPairs(itemPairs, graph, partitionOne);
        GraphUtil.addVerticesForEmptyStacks(emptyStacks, graph, partitionTwo);

        for (int pair = 0; pair < itemPairs.size(); pair++) {
            for (String emptyStack : emptyStacks) {

                DefaultWeightedEdge edge = graph.addEdge("pair" + itemPairs.get(pair), emptyStack);
                int stackIdx = Integer.parseInt(emptyStack.replace("stack", "").trim());
                double savings = 0.0;

                // both items compatible
                if (costMatrix[itemPairs.get(pair).getVertexOne()][stackIdx] < Integer.MAX_VALUE / items.length
                    && costMatrix[itemPairs.get(pair).getVertexTwo()][stackIdx] < Integer.MAX_VALUE / items.length) {
                        savings = this.getSavingsForPair(itemPairs, pair, costMatrix, stackIdx, costsBefore);
                // item one compatible
                } else if (costMatrix[itemPairs.get(pair).getVertexOne()][stackIdx] < Integer.MAX_VALUE / items.length) {
                    int itemOne = itemPairs.get(pair).getVertexOne();
                    savings = this.getSavingsForItem(costMatrix, stackIdx, costsBefore, itemOne);
                // item two compatible
                } else if (costMatrix[itemPairs.get(pair).getVertexTwo()][stackIdx] < Integer.MAX_VALUE / items.length) {
                    int itemTwo = itemPairs.get(pair).getVertexTwo();
                    savings = this.getSavingsForItem(costMatrix, stackIdx, costsBefore, itemTwo);
                }
                graph.setEdgeWeight(edge, savings);
            }
        }
        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Encapsulates the stacking constraint graph generation.
     *
     * @return the generated stacking constraint graph
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
     * Updates the stack assignments based on the matching that results in the maximum savings.
     *
     * @param maxSavingsMatching - the matching that results in the maximum savings
     * @param costsBefore - the hashmap containing the original costs for each item assignment
     */
    public void updateStackAssignments(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching,
        HashMap<Integer, Double> costsBefore
    ) {
        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int itemOne = GraphUtil.parseItemOneOfPair(edge);
            int itemTwo = GraphUtil.parseItemTwoOfPair(edge);
            int stack = GraphUtil.parseStackForPair(edge);
            HeuristicUtil.updateStackAssignmentsForPairs(itemOne, itemTwo, stack, costsBefore, this.instance);
        }
    }

    /**
     * This approach should be used in situations where the number of used stacks is irrelevant
     * and only the minimization of transport costs matters.
     *
     * Takes the generated solution and moves items from completely filled stacks to empty
     * stacks if it's possible to reduce the total costs by doing so.
     * Initially, a bipartite graph between item pairs in a completely filled stack and empty stacks is generated.
     * An edge between the two partitions indicates that at least one of the two items is assignable to the empty stack.
     * The costs of the edge correspond to the resulting savings if the item that induces higher savings is assigned to
     * the empty stack. A maximum weight matching is computed on that graph to retrieve an assignment that leads
     * to a maximum cost reduction.
     *
     * @param sol - the generated solution to be processed
     * @param minCostPerfectMatching - the min-cost-perfect-matching the solution is based on
     * @param itemPairs - the generated pairs of items
     * @return the result of the post-processing procedure
     */
    public Solution postProcessing(
        Solution sol,
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching,
       ArrayList<MCMEdge> itemPairs
    ) {

        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        ArrayList<String> emptyStacks = HeuristicUtil.retrieveEmptyStacks(sol);
        HashMap<Integer, Double> costsBefore = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());
        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(
            itemPairs, emptyStacks, this.instance.getCosts(), this.instance.getItems(), costsBefore
        );
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        System.out.println(maxSavingsMatching.getMatching().getEdges().size());

        this.updateStackAssignments(maxSavingsMatching, costsBefore);
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());

        return sol;
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
            BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            GraphUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());
            this.fixOrderInStacks();
            sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
            if (postProcessing) {
                sol = this.postProcessing(sol, minCostPerfectMatching, itemPairs);
            }
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        return sol;
    }
}
