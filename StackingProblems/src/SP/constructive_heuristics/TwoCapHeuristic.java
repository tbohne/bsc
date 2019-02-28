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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TwoCapHeuristic {

    private Instance instance;
    private double startTime;
    private int timeLimit;

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
     * Returns the list of unmatched items based on the list of item pairs.
     *
     * @param itemPairs - the list of item pairs (matched items)
     * @return the list of unmatched items
     */
    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> itemPairs) {

        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge edge : itemPairs) {
            matchedItems.add(edge.getVertexOne());
            matchedItems.add(edge.getVertexTwo());
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition
     * and computes the minimum cost perfect matching for this graph.
     * Since the two partitions aren't equally sized and the used algorithm to compute the MinCostPerfectMatching
     * expects a complete bipartite graph, there are dummy items that are used to make the graph complete bipartite.
     * These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemPairs - the pairs of items that are assigned to a stack together
     * @param unmatchedItems - the items that are assigned to their own stack
     * @return the minimum cost perfect matching between items and stacks
     */
    public KuhnMunkresMinimalWeightBipartitePerfectMatching getMinCostPerfectMatching(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        for (MCMEdge edge : itemPairs) {
            graph.addVertex("edge" + edge);
            partitionOne.add("edge" + edge);
        }
        for (int item : unmatchedItems) {
            graph.addVertex("item" + item);
            partitionOne.add("item" + item);
        }
        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            graph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }

        int cnt = 0;
        ArrayList<Integer> dummyItems = new ArrayList<>();
        while (partitionOne.size() < partitionTwo.size()) {
            graph.addVertex("dummy" + cnt);
            partitionOne.add("dummy" + cnt);
            dummyItems.add(cnt);
            cnt++;
        }

        // item pair - stack edges
        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                if (!graph.containsEdge("edge" + itemPairs.get(i), "stack" + j)) {
                    DefaultWeightedEdge edge = graph.addEdge("edge" + itemPairs.get(i), "stack" + j);
                    int costs = this.instance.getCosts()[itemPairs.get(i).getVertexOne()][j] + this.instance.getCosts()[itemPairs.get(i).getVertexTwo()][j];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }
        // unmatched item - stack edges
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                if (!graph.containsEdge("item" + item, "stack" + stack)) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + item, "stack" + stack);
                    int costs = this.instance.getCosts()[item][stack];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }

        // dummy items can be stored in every stack with no costs
        for (int item : dummyItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                DefaultWeightedEdge edge = graph.addEdge("dummy" + item, "stack" + stack);
                int costs = 0;
                graph.setEdgeWeight(edge, costs);
            }
        }

        return new KuhnMunkresMinimalWeightBipartitePerfectMatching(graph,partitionOne, partitionTwo);
    }

    /**
     * Parses the minimum cost perfect matching and assigns the items to the specified stacks.
     * The dummy items are ignored here, because they're not relevant for the assignment.
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
     * Generates a solution to the SP using the item pairs given by the MCM.
     * The item pairs are used to determine the unmatched items.
     * A minimum cost perfect matching in the bipartite graph consisting of the set of items (and item pairs) and the set
     * of stacks is computed and interpreted as stack assignments. Finally, the order in the stacks is fixed
     * to respect the stacking  constraints.
     *
     * @param mcm - the maximum cardinality matching determining the pairs of items
     * @return the generated solution
     */
    public Solution generateSolution(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(mcm);
        ArrayList<Integer> unmatchedItems = this.getUnmatchedItems(itemPairs);
        KuhnMunkresMinimalWeightBipartitePerfectMatching minCostPerfectMatching = this.getMinCostPerfectMatching(itemPairs, unmatchedItems);
        this.parseMatchingAndAssignItems(minCostPerfectMatching);
        this.fixOrderInStacks();

        return new Solution(0, this.timeLimit, this.instance);
    }

    /**
     * Solves the SP with an approach that uses a maximum cardinality matching followed by a minimum cost perfect matching
     * to feasibly assign all items to the storage area while minimizing the costs.
     *
     * @return the generated solution
     */
    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 2) {

            this.startTime = System.currentTimeMillis();
            DefaultUndirectedGraph stackingConstraintGraph = HeuristicUtil.generateStackingConstraintGraphNewWay(
                this.instance.getItems(), this.instance.getStackingConstraints(), this.instance.getCosts(),
                    Integer.MAX_VALUE / this.instance.getItems().length, this.instance.getStacks()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);
            sol = generateSolution(itemMatching);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 2.");
        }
        return sol;
    }
}
