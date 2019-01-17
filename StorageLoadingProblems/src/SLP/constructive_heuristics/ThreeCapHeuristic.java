package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import SLP.util.MapUtil;
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
     * Returns the list of unmatched items increasingly sorted by row rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public ArrayList<Integer> getUnmatchedItemsSortedByRowRating(ArrayList<Integer> unmatchedItems) {

        HashMap<Integer, Integer> unmatchedItemRowRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            int rating = HeuristicUtil.computeRowRatingForUnmatchedItem(item, this.instance.getStackingConstraints());
            unmatchedItemRowRatings.put(item, rating);
        }
        ArrayList<Integer> unmatchedItemsSortedByRowRating = new ArrayList<>();
        Map<Integer, Integer> sortedItemRowRatings = MapUtil.sortByValue(unmatchedItemRowRatings);
        for (int item : sortedItemRowRatings.keySet()) {
            unmatchedItemsSortedByRowRating.add(item);
        }

        return unmatchedItemsSortedByRowRating;
    }

    /**
     * Returns the list of unmatched items increasingly sorted by col rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public ArrayList<Integer> getUnmatchedItemsSortedByColRating(ArrayList<Integer> unmatchedItems) {
        HashMap<Integer, Integer> unmatchedItemColRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            unmatchedItemColRatings.put(item, HeuristicUtil.computeColRatingForUnmatchedItem(item, this.instance.getStackingConstraints()));
        }
        Map<Integer, Integer> sortedItemColRatings = MapUtil.sortByValue(unmatchedItemColRatings);
        ArrayList<Integer> unmatchedItemsSortedByColRating = new ArrayList<>();
        for (int item : sortedItemColRatings.keySet()) {
            unmatchedItemsSortedByColRating.add(item);
        }
        return unmatchedItemsSortedByColRating;
    }

    /**
     * Adds the item pairs that exceed the number of stacks to the unmatched items.
     *
     * @param matchedItems - the list of matched item pairs
     * @param unmatchedItems - the list of unmatched items
     */
    public void removeExceedingItemPairsFromMatchedItems(ArrayList<MCMEdge> matchedItems, ArrayList<Integer> unmatchedItems) {
        ArrayList<MCMEdge> toBeRemoved = new ArrayList<>();
        for (int i = this.instance.getStacks().length; i < matchedItems.size(); i++) {
            int itemOne = matchedItems.get(i).getVertexOne();
            int itemTwo = matchedItems.get(i).getVertexTwo();
            unmatchedItems.add(itemOne);
            unmatchedItems.add(itemTwo);
            toBeRemoved.add(matchedItems.get(i));
        }

        for (MCMEdge e : toBeRemoved) {
            matchedItems.remove(matchedItems.indexOf(e));
        }
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
        this.removeExceedingItemPairsFromMatchedItems(matchedItems, unmatchedItems);

        // lowest rating first --> inflexible item first
        ArrayList<Integer> unmatchedItemsSortedByRowRating = this.getUnmatchedItemsSortedByRowRating(unmatchedItems);
        ArrayList<Integer> unmatchedItemsSortedByColRating = this.getUnmatchedItemsSortedByColRating(unmatchedItems);

        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRowRating));
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByColRating));

        // TODO: probably not really useful
        Collections.reverse(unmatchedItemsSortedByRowRating);
        Collections.reverse(unmatchedItemsSortedByColRating);
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

    // TODO: exchange certain elements of this sequence with other (unused) ones (EXPERIMENTAL APPROACH)
    // IDEA:
    // - choose a number n (20%) of random elements to be replaced
    // - choose the next n unused elements from the ordered list
    // - exchange the elements
    public ArrayList<MCMEdge> edgeExchange(List<MCMEdge> edges) {

        ArrayList tmpEdges = new ArrayList(edges);

        int numberOfEdgesToBeReplaced = (int) (0.3 * this.instance.getStacks().length);
        if (numberOfEdgesToBeReplaced > (edges.size() - this.instance.getStacks().length)) {
            numberOfEdgesToBeReplaced = edges.size() - this.instance.getStacks().length;
        }

        ArrayList<Integer> toBeReplaced = new ArrayList<>();

        for (int i = 0; i < numberOfEdgesToBeReplaced; i++) {
            toBeReplaced.add(HeuristicUtil.getRandomValueInBetween(0, this.instance.getStacks().length - 1));
        }
        for (int i = 0; i < toBeReplaced.size(); i++) {
            Collections.swap(tmpEdges, toBeReplaced.get(i), i + this.instance.getStacks().length);
        }

        return new ArrayList(tmpEdges);
    }

    /**
     * Applies each edge rating system to a copy of the item pair list.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public void applyRatingSystemsToItemPairPermutations(ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        int idx = 0;
        HeuristicUtil.assignRowRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        HeuristicUtil.assignColRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        HeuristicUtil.assignMaxRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        HeuristicUtil.assignMinRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        HeuristicUtil.assignSumRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
    }

    public void addItemPairPermutations(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        if (itemPairs.size() <= COMPLETE_PERMUTATION_LIMIT) {
            for (List<MCMEdge> edgeList : Collections2.permutations(itemPairs)) {
                itemPairPermutations.add(new ArrayList(edgeList));
            }
        } else {

            // TODO: Remove hard coded values
            for (int cnt = 0; cnt < 5000; cnt++) {
                ArrayList<MCMEdge> tmp = new ArrayList(this.edgeExchange(itemPairs));
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
        this.applyRatingSystemsToItemPairPermutations(itemPairPermutations);
        this.sortItemPairPermutationsBasedOnRatings(itemPairPermutations);

        // TODO: experiment
        this.addItemPairPermutations(itemPairs, itemPairPermutations);

        return itemPairPermutations;
    }

    // TODO: HERE
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

        System.out.println(partitionOne.size());
        System.out.println(partitionTwo.size());

        // item triple - stack edges
        for (int i = 0; i < itemTriples.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                DefaultWeightedEdge edge = graph.addEdge("triple" + itemTriples.get(i), "stack" + j);
                int costs = this.instance.getCosts()[itemTriples.get(i).get(0)][j]
                        + this.instance.getCosts()[itemTriples.get(i).get(1)][j]
                        + this.instance.getCosts()[itemTriples.get(i).get(2)][j];
                graph.setEdgeWeight(edge, costs);
            }
        }

        // item pair - stack edges
        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                if (!graph.containsEdge("pair" + itemPairs.get(i), "stack" + j)) {
                    DefaultWeightedEdge edge = graph.addEdge("pair" + itemPairs.get(i), "stack" + j);
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

    public ArrayList<ArrayList<Integer>> computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(graph, itemPairs, unmatchedItems);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTripleStackAssignments = new ArrayList<>();
        HeuristicUtil.parseItemTripleMCM(itemTripleStackAssignments, itemTriples);
        return itemTripleStackAssignments;
    }

    public void generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            DefaultUndirectedGraph<String, DefaultEdge> graph, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems
    ) {

        // adding the item pairs as nodes to the graph
        for (int i = 0; i < itemPairs.size(); i++) {
            graph.addVertex("edge" + itemPairs.get(i));
        }

        // adding the unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < unmatchedItems.size(); j++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done
                if (this.instance.getStackingConstraints()[itemPairs.get(i).getVertexOne()][itemPairs.get(i).getVertexTwo()] == 1) {
                    this.addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexTwo(), unmatchedItems.get(j),
                            itemPairs.get(i).getVertexOne(), itemPairs.get(i)
                    );
                } else {
                    this.addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexOne(), unmatchedItems.get(j),
                            itemPairs.get(i).getVertexTwo(), itemPairs.get(i)
                    );
                }
            }
        }
    }

    public void addEdgeForCompatibleItemTriple(DefaultUndirectedGraph<String, DefaultEdge> graph, int pairItemOne, int unmatchedItem, int pairItemTwo, MCMEdge itemPair) {

        if (this.instance.getStackingConstraints()[pairItemOne][unmatchedItem] == 1
                || this.instance.getStackingConstraints()[unmatchedItem][pairItemTwo] == 1) {

            if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
                graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
            }
        }
    }

    public ArrayList<Integer> unmatchedHere(ArrayList<ArrayList<Integer>> triples) {

        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (ArrayList<Integer> triple : triples) {
            for (int i : triple) {
                matchedItems.add(i);
            }
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();

        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    public ArrayList<Integer> getFinallyUnmatched(ArrayList<ArrayList<Integer>> triples, ArrayList<MCMEdge> pairs) {
        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (ArrayList<Integer> triple : triples) {
            for (int item : triple) {
                matchedItems.add(item);
            }
        }

        for (MCMEdge edge : pairs) {
            matchedItems.add(edge.getVertexOne());
            matchedItems.add(edge.getVertexTwo());
        }

        ArrayList<Integer> unmatched = new ArrayList<>();

        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatched.add(item);
            }
        }
        return unmatched;
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
     * @param optimizeSolution - indicates whether the solution should be optimized (otherwise the first feasible sol gets returned)
     * @return the generated solution
     */
    public Solution permutationApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching, boolean optimizeSolution) {

        Solution sol = new Solution();

        for (ArrayList<MCMEdge> itemPairPermutation : this.getItemPairPermutations(itemMatching)) {

            System.out.println(itemPairPermutation);

            if ((System.currentTimeMillis() - startTime) / 1000.0 >= this.timeLimit) { break; }
            for (List<Integer> unmatchedItems : this.getUnmatchedItemPermutations(itemPairPermutation)) {

                ArrayList<ArrayList<Integer>> triples = this.computeCompatibleItemTriples(itemPairPermutation, (ArrayList<Integer>) unmatchedItems);
                System.out.println(triples);
                unmatchedItems = this.unmatchedHere(triples);
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

                unmatchedItems = this.getFinallyUnmatched(triples, itemPairs);
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
     * @param optimizeSolution - specifies whether the solution should be optimized (otherwise the first feasible solution gets returned)
     * @return the solution generated by the heuristic
     */
    public Solution solve(boolean optimizeSolution) {

        // TODO: not yet optimizing

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

            sol = permutationApproach(itemMatching, optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 3.");
        }
        return sol;
    }
}
