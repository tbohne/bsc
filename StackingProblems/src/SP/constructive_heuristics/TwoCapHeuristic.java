package SP.constructive_heuristics;

import SP.representations.*;
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
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of item-pairs and the second one consists of empty stacks.
     * An edge between the partitions means that at least one item of the pair is compatible to the empty stack.
     * The costs of the edges correspond to the maximum savings that are generated if an item of the pair is
     * assigned to the empty stack.
     *
     * @param itemPairs - the list of item pairs
     * @param emptyStacks - the list of remaining empty stacks
     * @param costsBefore - the original costs for each item assignment
     * @return the generated bipartite graph
     */
    public BipartiteGraph generatePostProcessingGraph(
        ArrayList<ArrayList<Integer>> itemPairs,
        ArrayList<String> emptyStacks,
        HashMap<Integer, Double> costsBefore
    ) {
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();
        GraphUtil.addVerticesForListOfItemPairs(itemPairs, postProcessingGraph, partitionOne);
        GraphUtil.addVerticesForEmptyStacks(emptyStacks, postProcessingGraph, partitionTwo);
        HeuristicUtil.addEdgesForItemPairs(postProcessingGraph, itemPairs, emptyStacks, costsBefore, this.instance);
        return new BipartiteGraph(partitionOne, partitionTwo, postProcessingGraph);
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

    public void updateStackAssignmentsNewWay(MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching) {

        ArrayList<Integer> blockedStacks = new ArrayList<>();

        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int item = Integer.parseInt(edge.toString().split(":")[0].replace("(", "").replace("item", "").trim());
            int stack = Integer.parseInt(edge.toString().split(":")[2].split(",")[0].trim());
            int level = Integer.parseInt(edge.toString().split(":")[3].replace(")", "").trim());

            if (blockedStacks.contains(stack)) { continue; }

            HeuristicUtil.removeItemFromOutdatedPosition(item, this.instance.getStacks());
            this.instance.getStacks()[stack][level] = item;
            blockedStacks.add(stack);
        }
    }

    /**
     * Parses the given minimum weight perfect matching and assigns it to the given stacks.
     *
     * @param mwpm   - the minimum weight perfect matching to be parsed
     * @param stacks - the stacks the parsed items are going to be assigned to
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
     * Returns the list of item pairs based on the list of MCM edges.
     *
     * @param edges - the list of edges
     * @return the list of item pairs
     */
    public ArrayList<ArrayList<Integer>> getListOfPairsFromEdges(ArrayList<MCMEdge> edges) {
        ArrayList<ArrayList<Integer>> itemPairs = new ArrayList<>();
        for (MCMEdge e : edges) {
            ArrayList<Integer> itemPair = new ArrayList<>();
            itemPair.add(e.getVertexOne());
            itemPair.add(e.getVertexTwo());
            itemPairs.add(itemPair);
        }
        return itemPairs;
    }

    public ArrayList<StorageAreaPosition> retrieveEmptyPositions(Solution sol) {
        ArrayList<StorageAreaPosition> emptyPositions = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStorageArea().length; stack++) {
            // top to bottom
            for (int level = 0; level < sol.getFilledStorageArea()[stack].length; level++) {
                if (sol.getFilledStorageArea()[stack][level] == -1) {
                    emptyPositions.add(new StorageAreaPosition(stack, level));
                }
            }
        }
        return emptyPositions;
    }

    public void addEdgesForCompatibleItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
            ArrayList<Integer> items,
            ArrayList<StorageAreaPosition> emptyPositions,
            HashMap<Integer, Double> originalCosts,
            Solution sol
    ) {

        for (int item = 0; item < items.size(); item++) {
            for (StorageAreaPosition emptyPos : emptyPositions) {

                // item compatible with stack
                if (this.instance.getCosts()[item][emptyPos.getStackIdx()] < Integer.MAX_VALUE / this.instance.getItems().length) {

                    int levelOfOtherSlot = emptyPos.getLevel() == 0 ? 1 : 0;
                    if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelOfOtherSlot] != -1) {
                        int otherItem = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelOfOtherSlot];

                        // item compatible with other item
                        if (levelOfOtherSlot == 0) {
                            // 0 --> top?
                            if (this.instance.getStackingConstraints()[otherItem][item] == 1) {
                                DefaultWeightedEdge edge = postProcessingGraph.addEdge("item" + item, "pos" + emptyPos);
                                double savings = HeuristicUtil.getSavingsForItem(emptyPos.getStackIdx(), originalCosts, item, this.instance.getCosts());
                                postProcessingGraph.setEdgeWeight(edge, savings);
                            }
                        } else {
                            if (this.instance.getStackingConstraints()[item][otherItem] == 1) {
                                DefaultWeightedEdge edge = postProcessingGraph.addEdge("item" + item, "pos" + emptyPos);
                                double savings = HeuristicUtil.getSavingsForItem(emptyPos.getStackIdx(), originalCosts, item, this.instance.getCosts());
                                postProcessingGraph.setEdgeWeight(edge, savings);
                            }
                        }

                    // no other item in stack
                    } else {
                        DefaultWeightedEdge edge = postProcessingGraph.addEdge("item" + item, "pos" + emptyPos);
                        double savings = HeuristicUtil.getSavingsForItem(emptyPos.getStackIdx(), originalCosts, item, this.instance.getCosts());
                        postProcessingGraph.setEdgeWeight(edge, savings);
                    }
                }
            }
        }
    }

    public BipartiteGraph generatePostProcessingGraphNewWay(
        ArrayList<Integer> items,
        ArrayList<StorageAreaPosition> emptyPositions,
        HashMap<Integer, Double> costsBefore,
        Solution sol
    ) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(
                DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(items, postProcessingGraph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(emptyPositions, postProcessingGraph, partitionTwo);

        this.addEdgesForCompatibleItems(postProcessingGraph, items, emptyPositions, costsBefore, sol);

        return new BipartiteGraph(partitionOne, partitionTwo, postProcessingGraph);
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
     * @param sol - the generated solution to be processed
     * @return the result of the post-processing procedure
     */
    public Solution postProcessing(Solution sol) {
        System.out.println("costs before post processing: " + sol.getObjectiveValue());
        ArrayList<StorageAreaPosition> emptyPositions = this.retrieveEmptyPositions(sol);
        ArrayList<Integer> items = sol.getAssignedItems();
        HashMap<Integer, Double> costsBefore = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());
        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraphNewWay(items, emptyPositions, costsBefore, sol);
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );
        this.updateStackAssignmentsNewWay(maxSavingsMatching);
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
            this.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());
            this.fixOrderInStacks();
            sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);

            if (postProcessing) {
                double bestSolutionCost = sol.computeCosts();
                sol = this.postProcessing(sol);
                while (sol.computeCosts() < bestSolutionCost) {
                    bestSolutionCost = sol.computeCosts();
                    sol = this.postProcessing(sol);
                }
            }
            sol.lowerItemsThatAreStackedInTheAir();
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        return sol;
    }
}
