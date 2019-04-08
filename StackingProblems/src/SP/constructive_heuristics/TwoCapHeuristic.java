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

    //////////////////////////////////////////////////////////////////////

    /**
     *
     *
     * @param itemPairs
     * @param emptyStacks
     * @param costMatrix
     * @param items
     * @param costsBefore
     * @return
     */
    public BipartiteGraph generatePostProcessingGraph(
        ArrayList<MCMEdge> itemPairs, ArrayList<String> emptyStacks, double[][] costMatrix,
        int[] items, HashMap<Integer, Double> costsBefore
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

                // BOTH ITEMS COMPATIBLE
                if (costMatrix[itemPairs.get(pair).getVertexOne()][stackIdx] < Integer.MAX_VALUE / items.length
                    && costMatrix[itemPairs.get(pair).getVertexTwo()][stackIdx] < Integer.MAX_VALUE / items.length) {

                        int itemOne = itemPairs.get(pair).getVertexOne();
                        int itemTwo = itemPairs.get(pair).getVertexTwo();
                        double costsItemOne = costMatrix[itemOne][stackIdx];
                        double costsItemTwo = costMatrix[itemTwo][stackIdx];
                        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
                        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
                        savings = savingsItemOne > savingsItemTwo ? savingsItemOne : savingsItemTwo;

                // ITEM ONE COMPATIBLE
                } else if (costMatrix[itemPairs.get(pair).getVertexOne()][stackIdx] < Integer.MAX_VALUE / items.length) {

                    int itemOne = itemPairs.get(pair).getVertexOne();
                    double costsItemOne = costMatrix[itemOne][stackIdx];
                    double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
                    savings = savingsItemOne;

                // ITEM TWO COMPATIBLE
                } else if (costMatrix[itemPairs.get(pair).getVertexTwo()][stackIdx] < Integer.MAX_VALUE / items.length) {

                    int itemTwo = itemPairs.get(pair).getVertexTwo();
                    double costsItemTwo = costMatrix[itemTwo][stackIdx];
                    double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
                    savings = savingsItemTwo;
                }
                graph.setEdgeWeight(edge, savings);
            }
        }
        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }


    //////////////////////////////////////////////////////////////////////

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

    public void removeOutdatedItemPos(int item) {
        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            for (int level = 0; level < this.instance.getStacks()[stack].length; level++) {
                if (this.instance.getStacks()[stack][level] == item) {
                    this.instance.getStacks()[stack][level] = -1;
                    // lower item if stacked "in the air"
                    if (level == this.instance.getGroundLevel() && this.instance.getStacks()[stack][this.instance.getTopLevel()] != -1) {
                        this.instance.getStacks()[stack][this.instance.getGroundLevel()] = this.instance.getStacks()[stack][this.instance.getTopLevel()];
                        this.instance.getStacks()[stack][this.instance.getTopLevel()] = -1;
                    }
                }
            }
        }
    }

    public ArrayList<String> findEmptyStacks(KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching) {
        ArrayList<String> emptyStacks = new ArrayList<>();
        for (Object edge : minCostPerfectMatching.getMatching().getEdges()) {
            if (edge.toString().contains("dummy")) {
                emptyStacks.add(edge.toString().split(":")[1].replace(")", "").trim());
            }
        }
        return emptyStacks;
    }

    public HashMap<Integer, Double> getCostsBefore(KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching) {
        HashMap<Integer, Double> costsBefore = new HashMap<>();
        for (DefaultWeightedEdge edge : minCostPerfectMatching.getMatching().getEdges()) {
            if (edge.toString().contains("pair")) {
                int itemOne = GraphUtil.parseItemOneOfPair(edge);
                int itemTwo = GraphUtil.parseItemTwoOfPair(edge);
                int stack = GraphUtil.parseStackForPair(edge);
                costsBefore.put(itemOne, this.instance.getCosts()[itemOne][stack]);
                costsBefore.put(itemTwo, this.instance.getCosts()[itemTwo][stack]);
            }
        }
        return costsBefore;
    }

    /**
     *
     *
     * @param maxSavingsMatching
     * @param costsBefore
     */
    public void updateStackAssignments(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching,
        HashMap<Integer, Double> costsBefore
    ) {

        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int itemOne = GraphUtil.parseItemOneOfPair(edge);
            int itemTwo = GraphUtil.parseItemTwoOfPair(edge);
            int stack = GraphUtil.parseStackForPair(edge);

            // BOTH ITEMS COMPATIBLE
            if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length
                && this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {

                double costsItemOne = this.instance.getCosts()[itemOne][stack];
                double costsItemTwo = this.instance.getCosts()[itemTwo][stack];
                double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
                double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;

                if (savingsItemOne > savingsItemTwo) {
                    this.removeOutdatedItemPos(itemOne);
                    this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemOne;
                } else {
                    this.removeOutdatedItemPos(itemTwo);
                    this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemTwo;
                }

                // ITEM ONE COMPATIBLE
            } else if (this.instance.getCosts()[itemOne][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {

                this.removeOutdatedItemPos(itemOne);
                this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemOne;

                // ITEM TWO COMPATIBLE
            } else if (this.instance.getCosts()[itemTwo][stack] < Integer.MAX_VALUE / this.instance.getItems().length) {

                this.removeOutdatedItemPos(itemTwo);
                this.instance.getStacks()[stack][this.instance.getGroundLevel()] = itemTwo;
            }
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
     * @return the result of the post-processing
     */
    public Solution postProcessing(
        Solution sol,
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching,
       ArrayList<MCMEdge> itemPairs
    ) {

        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        ArrayList<String> emptyStacks = this.findEmptyStacks(minCostPerfectMatching);
        HashMap<Integer, Double> costsBefore = this.getCostsBefore(minCostPerfectMatching);
        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(
                itemPairs, emptyStacks, this.instance.getCosts(), this.instance.getItems(), costsBefore
        );
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

        System.out.println(maxSavingsMatching.getMatching().getEdges().size());

        this.updateStackAssignments(maxSavingsMatching, costsBefore);
        sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
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
