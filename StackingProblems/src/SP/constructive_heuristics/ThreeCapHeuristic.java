package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class ThreeCapHeuristic {

    private Instance instance;
    private double startTime;
    private int timeLimit;

    public ThreeCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Adds the vertices for item triples, item pairs, unmatched items and stacks to the specified graph
     * and fills the partitions that define the bipartite graph.
     *
     * @param itemTriples - the list of item triples
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @param graph - the graph to be created
     * @param partitionOne - the first partition of the bipartite graph
     * @param partitionTwo - the second partition of the bipartite graph
     */
    public void addVertices(ArrayList<ArrayList<Integer>> itemTriples, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph, Set<String> partitionOne, Set<String> partitionTwo) {

            for (ArrayList<Integer> triple : itemTriples) {
                graph.addVertex("triple" + triple);
                partitionOne.add("triple" + triple);
            }
            for (MCMEdge pair : itemPairs) {
                graph.addVertex("pair" + pair);
                partitionOne.add("pair" + pair);
            }
            for (int item : unmatchedItems) {
                graph.addVertex("item" + item);
                partitionOne.add("item" + item);
            }
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                graph.addVertex("stack" + stack);
                partitionTwo.add("stack" + stack);
            }
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition
     * and computes the minimum cost perfect matching for this graph.
     * Since the two partitions aren't equally sized and the used algorithm to compute the MinCostPerfectMatching
     * expects a complete bipartite graph, there are dummy items that are used to make the graph complete bipartite.
     * These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemTriples - the list of item triples
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return MinCostPerfectMatching between items and stacks
     */
    public KuhnMunkresMinimalWeightBipartitePerfectMatching getMinCostPerfectMatching(
            ArrayList<ArrayList<Integer>> itemTriples,
            ArrayList<MCMEdge> itemPairs,
            ArrayList<Integer> unmatchedItems
    ) {
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();
        this.addVertices(itemTriples, itemPairs, unmatchedItems, graph, partitionOne, partitionTwo);
        ArrayList<Integer> dummyItems = HeuristicUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);

        HeuristicUtil.addEdgesForItemTriples(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
        HeuristicUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

        return new KuhnMunkresMinimalWeightBipartitePerfectMatching(graph,partitionOne, partitionTwo);
    }

    /**
     * Computes item triples from the list of item pairs and the list of unmatched items.
     *
     * @param itemPairs - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return list of item compatible item triples
     */
    public ArrayList<ArrayList<Integer>> computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = HeuristicUtil.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
                itemPairs, unmatchedItems, this.instance.getStackingConstraints()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTriples = HeuristicUtil.parseItemTripleMCM(mcm);

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
        HeuristicUtil.generateCompletelyFilledStacks(itemPairs, itemPairRemovalList, completelyFilledStacks, this.instance.getStackingConstraints());
        return completelyFilledStacks;
    }

    /**
     * Generates the solution to the given instance of the SP by applying the heuristic described in solve().
     *
     * @param itemMatching - matching containing the item pairs
     * @return the generated solution
     */
    public Solution generateSolution(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching) {

        ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(itemMatching);
        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItems(itemPairs, this.instance.getItems()));

        ArrayList<ArrayList<Integer>> triples = this.computeCompatibleItemTriples(itemPairs, unmatchedItems);

        // items that are not part of a triple
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

        // build pairs again
        DefaultUndirectedGraph graph = HeuristicUtil.generateStackingConstraintGraphNewWay(
            HeuristicUtil.getArrayFromList(unmatchedItems), this.instance.getStackingConstraints(),
            this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
        );
        EdmondsMaximumCardinalityMatching pairs = new EdmondsMaximumCardinalityMatching(graph);
        itemPairs = HeuristicUtil.parseItemPairMCM(pairs);

        // The remaining unmatched items are not assignable to the pairs,
        // therefore pairs are merged together to form triples if possible.
        triples.addAll(this.mergeItemPairs(itemPairs));

        // compute finally unmatched items
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

        graph = HeuristicUtil.generateStackingConstraintGraphNewWay(
                HeuristicUtil.getArrayFromList(unmatchedItems), this.instance.getStackingConstraints(),
                this.instance.getCosts(), Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
        );
        pairs = new EdmondsMaximumCardinalityMatching(graph);

        // items that are stored as pairs
        itemPairs = HeuristicUtil.parseItemPairMCM(pairs);

        // items that are stored in their own stack
        unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(triples, itemPairs, this.instance.getItems());

        // unable to assign the items feasibly to the given number of stacks
        if (triples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) { return new Solution(); }

        KuhnMunkresMinimalWeightBipartitePerfectMatching minCostPerfectMatching = this.getMinCostPerfectMatching(triples, itemPairs, unmatchedItems);

        HeuristicUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

        Solution sol = new Solution(0, this.timeLimit, this.instance);
        sol.transformStackAssignmentIntoValidSolutionIfPossible();
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

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = HeuristicUtil.generateStackingConstraintGraphNewWay(
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