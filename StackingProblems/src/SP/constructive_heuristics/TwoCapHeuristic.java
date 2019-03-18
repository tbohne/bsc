package SP.constructive_heuristics;

import SP.representations.BipartiteGraph;
import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
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
     * Solves the stacking problem with an approach that uses a maximum cardinality matching followed by a minimum
     * weight perfect matching to feasibly assign all items to the storage area while minimizing the costs.
     * Basic idea:
     *      - generate stacking constraint graph
     *      - compute MCM and interpret edges as item pairs
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *
     * @return the generated solution
     */
    public Solution solve() {

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
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        return sol;
    }
}
