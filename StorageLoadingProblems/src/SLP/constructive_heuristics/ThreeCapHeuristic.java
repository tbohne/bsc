package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import com.google.common.collect.Collections2;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class ThreeCapHeuristic {

    private final int COMPLETE_PERMUTATION_LIMIT = 8;
    private final int ITEM_PAIR_PERMUTATIONS = 40000;
    private final int NUMER_OF_USED_EDGE_RATING_SYSTEMS = 5;

    private Instance instance;
    private double startTime;
    private int timeLimit;

    public ThreeCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Returns a list of permutations of the unmatched items.
     * These permutations are generated according to several strategies.
     *
     * @param matchedItems - the items that are already matched
     * @return list of of permutations of the unmatched items
     */
    public ArrayList<List<Integer>> getUnmatchedItemPermutations(ArrayList<MCMEdge> matchedItems) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItems(matchedItems, this.instance.getItems()));
        HeuristicUtil.removeExceedingItemPairsFromMatchedItems(matchedItems, unmatchedItems, this.instance.getStacks());
        ArrayList<Integer> unmatchedItemsSortedByRowRating = HeuristicUtil.getUnmatchedItemsSortedByRowRating(unmatchedItems, this.instance.getStackingConstraints());
        ArrayList<Integer> unmatchedItemsSortedByColRating = HeuristicUtil.getUnmatchedItemsSortedByColRating(unmatchedItems, this.instance.getStackingConstraints());

        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRowRating));
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByColRating));

        return unmatchedItemPermutations;
    }

    /**
     * Sorts each item pair permutation based on the assigned edge ratings.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public void sortItemPairPermutationsBasedOnRatings(ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        for (int i = 0; i < NUMER_OF_USED_EDGE_RATING_SYSTEMS; i++) {
            Collections.sort(itemPairPermutations.get(i));
        }
    }

    /**
     * Adds item pair permutations to the list.
     *
     * @param itemPairs - the list of item pair
     * @param itemPairPermutations - the list of permutations of the item pairs
     */
    public void addItemPairPermutations(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        if (itemPairs.size() <= COMPLETE_PERMUTATION_LIMIT) {
            for (List<MCMEdge> edgeList : Collections2.permutations(itemPairs)) {
                itemPairPermutations.add(new ArrayList(edgeList));
            }
        } else {

            // TODO: Remove hard coded values
            for (int cnt = 0; cnt < 5000; cnt++) {
                ArrayList<MCMEdge> tmp = new ArrayList(HeuristicUtil.edgeExchange(itemPairs, this.instance.getStacks()));
                if (!itemPairPermutations.contains(tmp)) {
                    itemPairPermutations.add(tmp);
                }
            }

            for (int i = 0; i < ITEM_PAIR_PERMUTATIONS; i++) {
                Collections.shuffle(itemPairs);
                itemPairPermutations.add(new ArrayList(itemPairs));
            }
        }
    }

    /**
     * Returns a list of permutations of the item pairs.
     * The permutations are generated using different rating systems as basis for the sorting procedure.
     *
     * @param itemMatching - matching containing the item pairs
     * @return list of item pair permutations
     */
    public ArrayList<ArrayList<MCMEdge>> getItemPairPermutations(EdmondsMaximumCardinalityMatching itemMatching) {

        ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(itemMatching);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        for (int i = 0; i < NUMER_OF_USED_EDGE_RATING_SYSTEMS; i++) {
            ArrayList<MCMEdge> tmpItemPairs = HeuristicUtil.getCopyOfEdgeList(itemPairs);
            itemPairPermutations.add(tmpItemPairs);
        }
        HeuristicUtil.applyRatingSystemsToItemPairPermutations(itemPairPermutations, this.instance.getStackingConstraints());
        this.sortItemPairPermutationsBasedOnRatings(itemPairPermutations);

        // TODO: experiment
        this.addItemPairPermutations(itemPairs, itemPairPermutations);

        return itemPairPermutations;
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

        int cnt = 0;
        ArrayList<Integer> dummyItems = new ArrayList<>();
        while (partitionOne.size() < partitionTwo.size()) {
            graph.addVertex("dummy" + cnt);
            partitionOne.add("dummy" + cnt);
            dummyItems.add(cnt);
            cnt++;
        }

        // edges from item triples to stacks
        for (int i = 0; i < itemTriples.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                DefaultWeightedEdge edge = graph.addEdge("triple" + itemTriples.get(i), "stack" + j);
                int costs = this.instance.getCosts()[itemTriples.get(i).get(0)][j]
                        + this.instance.getCosts()[itemTriples.get(i).get(1)][j]
                        + this.instance.getCosts()[itemTriples.get(i).get(2)][j];
                graph.setEdgeWeight(edge, costs);
            }
        }

        // edges from item pairs to stacks
        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                if (!graph.containsEdge("pair" + itemPairs.get(i), "stack" + j)) {
                    DefaultWeightedEdge edge = graph.addEdge("pair" + itemPairs.get(i), "stack" + j);
                    int costs = this.instance.getCosts()[itemPairs.get(i).getVertexOne()][j] + this.instance.getCosts()[itemPairs.get(i).getVertexTwo()][j];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }
        // edges from unmatched items to stacks
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                if (!graph.containsEdge("item" + item, "stack" + stack)) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + item, "stack" + stack);
                    int costs = this.instance.getCosts()[item][stack];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }

        // edges from dummy items to stacks
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
        ArrayList<ArrayList<Integer>> itemTriples = new ArrayList<>();
        HeuristicUtil.parseItemTripleMCM(itemTriples, mcm);
        return itemTriples;
    }

    public ArrayList<Integer> getMatchedItemsFromPairs(ArrayList<MCMEdge> pairs) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge pair : pairs) {
            matchedItems.add(pair.getVertexOne());
            matchedItems.add(pair.getVertexTwo());
        }
        return matchedItems;
    }

    public ArrayList<Integer> getMatchedItemsFromTriples(ArrayList<ArrayList<Integer>> triples) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (ArrayList<Integer> triple : triples) {
            for (int item : triple) {
                matchedItems.add(item);
            }
        }
        return matchedItems;
    }

    public ArrayList<Integer> getUnmatchedItemsFromMatchedItems(ArrayList<Integer> matchedItems) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    public ArrayList<Integer> getUnmatchedItemsFromTriplesAndPairs(ArrayList<ArrayList<Integer>> triples, ArrayList<MCMEdge> pairs) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        matchedItems.addAll(this.getMatchedItemsFromTriples(triples));
        matchedItems.addAll(this.getMatchedItemsFromPairs(pairs));
        return this.getUnmatchedItemsFromMatchedItems(matchedItems);
    }

    /**
     * Returns a list of unmatched items based on the list of matched triples.
     *
     * @param triples - the list of matched triples
     * @return list of unmatched items
     */
    public ArrayList<Integer> getUnmatchedItemsFromTriples(ArrayList<ArrayList<Integer>> triples) {
        return this.getUnmatchedItemsFromMatchedItems(this.getMatchedItemsFromTriples(triples));
    }

    public void parseAndAssign(KuhnMunkresMinimalWeightBipartitePerfectMatching matching) {

        for (Object edge : matching.getMatching().getEdges()) {
            System.out.println(edge);

            if (edge.toString().contains("triple")) {
                int itemOne = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[0].replace("[", "".trim()));
                int itemTwo = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[1].trim());
                int itemThree = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[2].replace("]", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());

                this.instance.getStacks()[stack][0] = itemOne;
                this.instance.getStacks()[stack][1] = itemTwo;
                this.instance.getStacks()[stack][2] = itemThree;

            } else if (edge.toString().contains("pair")) {
                int itemOne = Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[0].replace("(", "").trim());
                int itemTwo = Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[1].replace(")", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());

                this.instance.getStacks()[stack][0] = itemOne;
                this.instance.getStacks()[stack][1] = itemTwo;
            } else if (edge.toString().contains("item")) {
                // TODO: Check whether possible at all
            }
        }
    }

    /**
     * Generates the solution to the given instance of the SLP.
     *
     * @param itemMatching - matching containing the item pairs
     * @return the generated solution
     */
    public Solution permutationApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching) {

        Solution sol = new Solution();

        for (ArrayList<MCMEdge> itemPairPermutation : this.getItemPairPermutations(itemMatching)) {

            System.out.println(itemPairPermutation);

            if ((System.currentTimeMillis() - startTime) / 1000.0 >= this.timeLimit) { break; }
            for (List<Integer> unmatchedItems : this.getUnmatchedItemPermutations(itemPairPermutation)) {

                ArrayList<ArrayList<Integer>> triples = this.computeCompatibleItemTriples(itemPairPermutation, (ArrayList<Integer>) unmatchedItems);
                System.out.println(triples);
                unmatchedItems = this.getUnmatchedItemsFromTriples(triples);
                System.out.println(unmatchedItems);

                int[] items = new int[unmatchedItems.size()];
                for (int i = 0; i < unmatchedItems.size(); i++) {
                    items[i] = unmatchedItems.get(i);
                }

                DefaultUndirectedGraph graph = new DefaultUndirectedGraph(DefaultEdge.class);
                HeuristicUtil.generateStackingConstraintGraphNewWay(graph, items, this.instance.getStackingConstraints());

                EdmondsMaximumCardinalityMatching pairs = new EdmondsMaximumCardinalityMatching(graph);
                ArrayList<MCMEdge> itemPairs = HeuristicUtil.parseItemPairMCM(pairs);
                System.out.println(pairs.getMatching().getEdges());

                unmatchedItems = this.getUnmatchedItemsFromTriplesAndPairs(triples, itemPairs);
                System.out.println(unmatchedItems);

                if (triples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) {
                    continue;
                }

                KuhnMunkresMinimalWeightBipartitePerfectMatching matching = this.getMinCostPerfectMatching(
                        triples, itemPairs, (ArrayList<Integer>) unmatchedItems
                );
                System.out.println(matching.getMatching().getEdges());

                this.parseAndAssign(matching);


                sol = new Solution(0, this.timeLimit, this.instance);
                sol.transformStackAssignmentIntoValidSolutionIfPossible();
                if (sol.isFeasible()) {
                    return sol;
                }
            }
        }
        return sol;
    }

    /**
     * Solves the SLP with an approach that uses maximum cardinality matchings and several permutations of items that
     * are generated according to a number of different strategies.
     *
     * @return the solution generated by the heuristic
     */
    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            HeuristicUtil.generateStackingConstraintGraphNewWay(
                    stackingConstraintGraph,
                    this.instance.getItems(),
                    this.instance.getStackingConstraints()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);

            sol = permutationApproach(itemMatching);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 3.");
        }
        return sol;
    }
}
