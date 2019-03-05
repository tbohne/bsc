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
     * Tries to merge item pairs by splitting up pairs and assigning both items
     * to other pairs to form completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<Integer>> mergeItemPairs(ArrayList<MCMEdge> itemPairs) {
        ArrayList<MCMEdge> itemPairRemovalList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> completelyFilledStacks = new ArrayList<>();
        HeuristicUtil.generateCompletelyFilledStacks(
            itemPairs, itemPairRemovalList, completelyFilledStacks, this.instance.getStackingConstraints()
        );
        return completelyFilledStacks;
    }

    /**
     * Generates the solution to the given instance of the SP by applying the heuristic described in solve().
     *
     * @param itemMatching - matching containing the item pairs
     * @return the generated solution
     */
    public Solution generateSolution(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching) {

        ArrayList<MCMEdge> itemPairs = GraphUtil.parseItemPairFromMCM(itemMatching);
        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItemsFromPairs(itemPairs, this.instance.getItems()));

        ArrayList<ArrayList<Integer>> triples = this.computeCompatibleItemTriples(itemPairs, unmatchedItems);

        // items that are not part of a triple
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

        // build pairs again
        DefaultUndirectedGraph<String, DefaultEdge> graph = GraphUtil.generateStackingConstraintGraph(
            HeuristicUtil.getItemArrayFromItemList(unmatchedItems), this.instance.getStackingConstraints(),
            this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> pairs = new EdmondsMaximumCardinalityMatching<>(graph);
        itemPairs = GraphUtil.parseItemPairFromMCM(pairs);

        // The remaining unmatched items are not assignable to the pairs,
        // therefore pairs are merged together to form triples if possible.
        triples.addAll(this.mergeItemPairs(itemPairs));

        // compute finally unmatched items
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

        graph = GraphUtil.generateStackingConstraintGraph(
                HeuristicUtil.getItemArrayFromItemList(unmatchedItems), this.instance.getStackingConstraints(),
                this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
        );
        pairs = new EdmondsMaximumCardinalityMatching<>(graph);

        // items that are stored as pairs
        itemPairs = GraphUtil.parseItemPairFromMCM(pairs);

        // items that are stored in their own stack
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(triples, itemPairs, this.instance.getItems());

        // unable to assign the items feasibly to the given number of stacks
        if (triples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) { return new Solution(); }

        BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(triples, itemPairs, unmatchedItems);

        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
            )
        ;

        GraphUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

        Solution sol = new Solution(0, this.timeLimit, this.instance);
        sol.transformStackAssignmentsIntoValidSolutionIfPossible();
        return sol;
    }

    /**
     * Solves the given instance of the SP. The objective is to minimize the transport costs
     * which is achieved by computing a min-cost-perfect-matching in the end.
     *
     * The heuristic is based on the following major steps:
     *      - compute item triples from unmatched items
     *      - compute item pairs from still unmatched items
     *      - compute still unmatched items
     *      - create bipartite graph between items and stacks
     *      - compute min-cost-perfect matching
     *      - assign items according to edges of MCPM
     *
     * @return the solution generated by the heuristic
     */
    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = GraphUtil.generateStackingConstraintGraph(
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                Integer.MAX_VALUE / this.instance.getItems().length,
                this.instance.getStacks()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);
            sol = generateSolution(itemMatching);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return sol;
    }
}
