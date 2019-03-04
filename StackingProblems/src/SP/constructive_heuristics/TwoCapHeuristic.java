package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.alg.partition.BipartitePartitioning;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Constructive heuristic to efficiently generate solutions of stacking
 * problems with a stack capacity of 2.
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
    public DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> generateBipartiteGraph(
        ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems
    ) {
        BipartitePartitioning<String, DefaultWeightedEdge> bipartiteGraph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForItemPairs(itemPairs, bipartiteGraph, partitionOne);
        GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, bipartiteGraph, partitionOne);
        GraphUtil.addVerticesForStacks(this.instance.getStacks(), bipartiteGraph, partitionTwo);

        ArrayList<Integer> dummyItems = GraphUtil.introduceDummyVertices(bipartiteGraph, partitionOne, partitionTwo);
        GraphUtil.addEdgesForItemPairs(bipartiteGraph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForUnmatchedItems(bipartiteGraph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        GraphUtil.addEdgesForDummyItems(bipartiteGraph, dummyItems, this.instance.getStacks());

        return bipartiteGraph;
    }

    /**
     * Parses the minimum cost perfect matching and assigns the items to the specified stacks.
     * The dummy items are ignored here, because they're not relevant for the assignment.
     * TODO: could be partially used in 3cap (unify and move to GraphUtil)
     *
     * @param minCostPM - the minimum cost perfect matching determining the stack assignments
     */
    public void parseMatchingAndAssignItems(KuhnMunkresMinimalWeightBipartitePerfectMatching minCostPM) {

        for (Object edge : minCostPM.getMatching().getEdges()) {

            // item case
            if (edge.toString().contains("item")) {
                int item = Integer.parseInt(edge.toString().split(":")[0].replace("(item", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
                this.instance.getStacks()[stack][0] = item;

            // item pair case
            } else if (edge.toString().contains("edge")) {
                int itemOne = Integer.parseInt(edge.toString().split(":")[0].split(",")[0].replace("(edge(", "").trim());
                int itemTwo = Integer.parseInt(edge.toString().split(":")[0].split(",")[1].replace(")", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
                this.instance.getStacks()[stack][0] = itemOne;
                this.instance.getStacks()[stack][1] = itemTwo;
            }
        }
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

            DefaultUndirectedGraph stackingConstraintGraph = GraphUtil.generateStackingConstraintGraph(
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                (Integer.MAX_VALUE / this.instance.getItems().length),
                this.instance.getStacks()
            );

            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);
            ArrayList<MCMEdge> itemPairs = GraphUtil.parseItemPairFromMCM(itemMatching);
            ArrayList<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromPairs(itemPairs, this.instance.getItems());


            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph = this.generateBipartiteGraph(itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm = new KuhnMunkresMinimalWeightBipartitePerfectMatching(
                    bipartiteGraph, partitionOne, partitionTwo
            );

            this.parseMatchingAndAssignItems(minCostPerfectMatching);
            this.fixOrderInStacks();
            sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        return sol;
    }
}
