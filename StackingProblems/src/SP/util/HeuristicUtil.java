package SP.util;

import SP.representations.MCMEdge;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

/**
 * A collection of utility methods used in the heuristics.
 *
 * @author Tim Bohne
 */
public class HeuristicUtil {

    /**
     * Generates a stacking constraint graph containing the given items as nodes.
     * There exists an edge between two items if the items are stackable in at least one direction
     * based on the given stacking constraints.
     *
     * @param items               - the items to be matched
     * @param stackingConstraints - the matrix representing the stacking constraints
     * @return the computed maximum cardinality matching
     */
    public static EdmondsMaximumCardinalityMatching<String, DefaultEdge> getMCMForItemList(ArrayList<Integer> items, int[][] stackingConstraints) {

        DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int item : items) {
            stackingConstraintGraph.addVertex("v" + item);
        }

        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < items.size(); i++) {
            for (int j = 0; j < items.size(); j++) {
                if (items.get(i) != items.get(j) && stackingConstraints[items.get(i)][items.get(j)] == 1
                    || stackingConstraints[items.get(j)][items.get(i)] == 1) {

                        if (!stackingConstraintGraph.containsEdge("v" + items.get(j), "v" + items.get(i))) {
                            stackingConstraintGraph.addEdge("v" + items.get(i), "v" + items.get(j));
                        }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);
        return mcm;
    }

    public static void addEdgesForItemPairs(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph,
            ArrayList<MCMEdge> itemPairs,
            int[][] stacks,
            int[][] costsArr
    ) {

        // edges from item pairs to stacks
        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < stacks.length; j++) {
                if (!graph.containsEdge("pair" + itemPairs.get(i), "stack" + j)) {
                    DefaultWeightedEdge edge = graph.addEdge("pair" + itemPairs.get(i), "stack" + j);
                    int costs = costsArr[itemPairs.get(i).getVertexOne()][j] + costsArr[itemPairs.get(i).getVertexTwo()][j];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }
    }

    public static void addEdgesForItemTriples(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph,
            ArrayList<ArrayList<Integer>> itemTriples,
            int[][] stacks,
            int[][] costsArr
    ) {
        // edges from item triples to stacks
        for (int i = 0; i < itemTriples.size(); i++) {
            for (int j = 0; j < stacks.length; j++) {
                DefaultWeightedEdge edge = graph.addEdge("triple" + itemTriples.get(i), "stack" + j);
                int costs = costsArr[itemTriples.get(i).get(0)][j]
                        + costsArr[itemTriples.get(i).get(1)][j]
                        + costsArr[itemTriples.get(i).get(2)][j];
                graph.setEdgeWeight(edge, costs);
            }
        }
    }

    public static void addEdgesForDummyItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph,
            ArrayList<Integer> dummyItems,
            int[][] stacks
    ) {
        // edges from dummy items to stacks
        // dummy items can be stored in every stack with no costs
        for (int item : dummyItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = graph.addEdge("dummy" + item, "stack" + stack);
                int costs = 0;
                graph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     *
     *
     * @param graph
     * @param unmatchedItems
     */
    public static void addEdgesForUnmatchedItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph,
            ArrayList<Integer> unmatchedItems,
            int[][] stacks,
            int[][] costsArr
    ) {
        // edges from unmatched items to stacks
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                if (!graph.containsEdge("item" + item, "stack" + stack)) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + item, "stack" + stack);
                    int costs = costsArr[item][stack];
                    graph.setEdgeWeight(edge, costs);
                }
            }
        }
    }

    /**
     *
     * @param graph
     * @param partitionOne
     * @param partitionTwo
     * @return
     */
    public static ArrayList<Integer> introduceDummyVertices(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph, Set<String> partitionOne, Set<String> partitionTwo) {

        int cnt = 0;
        ArrayList<Integer> dummyItems = new ArrayList<>();
        while (partitionOne.size() < partitionTwo.size()) {
            graph.addVertex("dummy" + cnt);
            partitionOne.add("dummy" + cnt);
            dummyItems.add(cnt);
            cnt++;
        }
        return dummyItems;
    }

    /**
     * Applies each edge rating system to a copy of the item pair list.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public static void applyRatingSystemsToItemPairPermutations(ArrayList<ArrayList<MCMEdge>> itemPairPermutations, int[][] stackingConstraints) {
        int idx = 0;
        HeuristicUtil.assignRowRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        HeuristicUtil.assignColRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        HeuristicUtil.assignMaxRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        HeuristicUtil.assignMinRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        HeuristicUtil.assignSumRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
    }

    // TODO: exchange certain elements of this sequence with other (unused) ones (EXPERIMENTAL APPROACH)
    // IDEA:
    // - choose a number n (20%) of random elements to be replaced
    // - choose the next n unused elements from the ordered list
    // - exchange the elements
    public static ArrayList<MCMEdge> edgeExchange(List<MCMEdge> edges, int[][] stacks) {

        ArrayList tmpEdges = new ArrayList(edges);

        int numberOfEdgesToBeReplaced = (int) (0.3 * stacks.length);
        if (numberOfEdgesToBeReplaced > (edges.size() - stacks.length)) {
            numberOfEdgesToBeReplaced = edges.size() - stacks.length;
        }

        ArrayList<Integer> toBeReplaced = new ArrayList<>();

        for (int i = 0; i < numberOfEdgesToBeReplaced; i++) {
            toBeReplaced.add(HeuristicUtil.getRandomValueInBetween(0, stacks.length - 1));
        }
        for (int i = 0; i < toBeReplaced.size(); i++) {
            Collections.swap(tmpEdges, toBeReplaced.get(i), i + stacks.length);
        }

        return new ArrayList(tmpEdges);
    }

    /**
     * Returns the list of unmatched items increasingly sorted by col rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsSortedByColRating(ArrayList<Integer> unmatchedItems, int[][] stackingConstraints) {
        HashMap<Integer, Integer> unmatchedItemColRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            unmatchedItemColRatings.put(item, HeuristicUtil.computeColRatingForUnmatchedItem(item, stackingConstraints));
        }
        Map<Integer, Integer> sortedItemColRatings = MapUtil.sortByValue(unmatchedItemColRatings);
        ArrayList<Integer> unmatchedItemsSortedByColRating = new ArrayList<>();
        for (int item : sortedItemColRatings.keySet()) {
            unmatchedItemsSortedByColRating.add(item);
        }
        return unmatchedItemsSortedByColRating;
    }

    /**
     * Returns the list of unmatched items increasingly sorted by row rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsSortedByRowRating(ArrayList<Integer> unmatchedItems, int[][] stackingConstraints) {

        HashMap<Integer, Integer> unmatchedItemRowRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            int rating = HeuristicUtil.computeRowRatingForUnmatchedItem(item, stackingConstraints);
            unmatchedItemRowRatings.put(item, rating);
        }
        ArrayList<Integer> unmatchedItemsSortedByRowRating = new ArrayList<>();
        Map<Integer, Integer> sortedItemRowRatings = MapUtil.sortByValue(unmatchedItemRowRatings);
        for (int item : sortedItemRowRatings.keySet()) {
            unmatchedItemsSortedByRowRating.add(item);
        }

        return unmatchedItemsSortedByRowRating;
    }

    public static void addEdgeForCompatibleItemTriple(
            DefaultUndirectedGraph<String, DefaultEdge> graph, int pairItemOne, int unmatchedItem, int pairItemTwo, MCMEdge itemPair, int[][] stackingConstraints
    ) {

        if (stackingConstraints[pairItemOne][unmatchedItem] == 1
                || stackingConstraints[unmatchedItem][pairItemTwo] == 1) {

            if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
                graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
            }
        }
    }

    public static ArrayList<Integer> getMatchedItemsFromPairs(ArrayList<MCMEdge> pairs) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge pair : pairs) {
            matchedItems.add(pair.getVertexOne());
            matchedItems.add(pair.getVertexTwo());
        }
        return matchedItems;
    }

    public static ArrayList<Integer> getMatchedItemsFromTriples(ArrayList<ArrayList<Integer>> triples) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (ArrayList<Integer> triple : triples) {
            for (int item : triple) {
                matchedItems.add(item);
            }
        }
        return matchedItems;
    }

    public static ArrayList<Integer> getUnmatchedItemsFromTriplesAndPairs(ArrayList<ArrayList<Integer>> triples, ArrayList<MCMEdge> pairs, int[] items) {
        ArrayList<Integer> matchedItems = new ArrayList<>();
        matchedItems.addAll(getMatchedItemsFromTriples(triples));
        matchedItems.addAll(getMatchedItemsFromPairs(pairs));
        return getUnmatchedItemsFromMatchedItems(matchedItems, items);
    }

    /**
     * Returns a list of unmatched items based on the list of matched triples.
     *
     * @param triples - the list of matched triples
     * @return list of unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsFromTriples(ArrayList<ArrayList<Integer>> triples, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromTriples(triples), items);
    }

    public static ArrayList<Integer> getUnmatchedItemsFromMatchedItems(ArrayList<Integer> matchedItems, int[] items) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : items) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }


    /**
     *
     *
     * @param itemPairs
     * @param unmatchedItems
     * @return
     */
    public static DefaultUndirectedGraph<String, DefaultEdge> generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems, int[][] stackingConstraints
    ) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

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
                if (stackingConstraints[itemPairs.get(i).getVertexOne()][itemPairs.get(i).getVertexTwo()] == 1) {
                    addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexTwo(), unmatchedItems.get(j),
                            itemPairs.get(i).getVertexOne(), itemPairs.get(i), stackingConstraints
                    );
                } else {
                    addEdgeForCompatibleItemTriple(graph, itemPairs.get(i).getVertexOne(), unmatchedItems.get(j),
                            itemPairs.get(i).getVertexTwo(), itemPairs.get(i), stackingConstraints
                    );
                }
            }
        }
        return graph;
    }

    public static DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraphNewWay(
            int[] items, int[][] stackingConstraints, int[][] costs, int max, int[][] stacks
    ) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        ArrayList<Integer> itemList = new ArrayList<>();
        for (int item : items) {
            itemList.add(item);
        }

        for (int item : items) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1
        // and if they have at least one compatible stack.

        for (int i = 0; i < stackingConstraints.length; i++) {
            for (int j = 0; j < stackingConstraints[0].length; j++) {
                if (i != j && stackingConstraints[i][j] == 1 ||stackingConstraints[j][i] == 1) {

                    int numberOfCompatibleStacks = 0;
                    for (int stackIdx = 0; stackIdx < stacks.length; stackIdx++) {
                        if (costs[i][stackIdx] < max && costs[j][stackIdx] < max) {
                            numberOfCompatibleStacks++;
                        }
                    }

                    // TODO: find reasonable way to calculate the value
                    if (numberOfCompatibleStacks > 0) {
                        if (itemList.contains(i) && itemList.contains(j)) {
                            if (!graph.containsEdge("v" + j, "v" + i)) {
                                graph.addEdge("v" + i, "v" + j);
                            }
                        }
                    }
                }
            }
        }
        return graph;
    }

    public static DefaultUndirectedGraph generateStackingConstraintGraph(int[] items, int[][] stackingConstraints) {

        DefaultUndirectedGraph graph = new DefaultUndirectedGraph(DefaultEdge.class);

        for (int item : items) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < stackingConstraints.length; i++) {
            for (int j = 0; j < stackingConstraints[0].length; j++) {
                if (i != j && stackingConstraints[i][j] == 1 ||stackingConstraints[j][i] == 1) {
                    if (!graph.containsEdge("v" + j, "v" + i)) {
                        graph.addEdge("v" + i, "v" + j);
                    }
                }
            }
        }
        return graph;
    }

    public static int computeRowRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int entry : stackingConstraints[item]) {
            rating += entry;
        }
        return rating;
    }

    public static int computeColRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int i = 0; i < stackingConstraints.length; i++) {
            rating += stackingConstraints[i][item];
        }
        return rating;
    }

    public static boolean stackEmpty(int stackIdx, int[][] storageArea) {
        return storageArea[stackIdx][2] == -1 && storageArea[stackIdx][1] == -1 && storageArea[stackIdx][0] == -1;
    }

    public static boolean itemPairAndStackCompatible(int stackIdx, int itemOne, int itemTwo, int[][] costs, int max) {
        return costs[itemOne][stackIdx] < max && costs[itemTwo][stackIdx] < max;
    }

    public static boolean itemsStackableInBothDirections(int itemOne, int itemTwo, int[][] stackingConstraints) {
        return stackingConstraints[itemTwo][itemOne] == 1 && stackingConstraints[itemOne][itemTwo] == 1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static HashMap<Integer, String> getRatingsMapForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        HashMap<Integer, String> itemRatings = new HashMap<>();
        itemRatings.put(HeuristicUtil.computeRowRatingForUnmatchedItem(itemOne, stackingConstraints), "itemOneRow");
        itemRatings.put(HeuristicUtil.computeColRatingForUnmatchedItem(itemOne, stackingConstraints), "itemOneCol");
        itemRatings.put(HeuristicUtil.computeRowRatingForUnmatchedItem(itemTwo, stackingConstraints), "itemTwoRow");
        itemRatings.put(HeuristicUtil.computeColRatingForUnmatchedItem(itemTwo, stackingConstraints), "itemTwoCol");
        return itemRatings;
    }

    public static ArrayList<Integer> getItemRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        HashMap<Integer, String> itemRatings = getRatingsMapForItemPair(itemOne, itemTwo, stackingConstraints);
        ArrayList<Integer> ratings = new ArrayList<>();
        for (int key : itemRatings.keySet()) {
            ratings.add(key);
        }
        return ratings;
    }

    public static int getSumOfRelevantRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {

        if (stackingConstraints[itemOne][itemTwo] == 1 && stackingConstraints[itemTwo][itemOne] == 1) {

            // stackable in both directions
            return computeRowRatingForUnmatchedItem(itemOne, stackingConstraints)
                + computeColRatingForUnmatchedItem(itemOne, stackingConstraints)
                + computeRowRatingForUnmatchedItem(itemTwo, stackingConstraints)
                + computeColRatingForUnmatchedItem(itemTwo, stackingConstraints);

        } else if (stackingConstraints[itemOne][itemTwo] == 1) {
            // item one upon item two
            int colItemOne = computeColRatingForUnmatchedItem(itemOne, stackingConstraints);
            int rowItemTwo = computeRowRatingForUnmatchedItem(itemTwo, stackingConstraints);
            return colItemOne + rowItemTwo;

        } else {
            // item two upon item one
            int colItemTwo = computeColRatingForUnmatchedItem(itemTwo, stackingConstraints);
            int rowItemOne = computeRowRatingForUnmatchedItem(itemOne, stackingConstraints);
            return colItemTwo + rowItemOne;
        }
    }

    public static ArrayList<MCMEdge> getCopyOfEdgeList(ArrayList<MCMEdge> edgeList) {
        ArrayList<MCMEdge> edgeListCopy = new ArrayList<>();
        for (MCMEdge edge : edgeList) {
            edgeListCopy.add(new MCMEdge(edge));
        }
        return edgeListCopy;
    }

    public static int getExtremeOfRelevantRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints, boolean min) {

        // both directions possible?
        if (itemsStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
            if (min) {
                return Collections.min(getItemRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
            } else {
                return Collections.max(getItemRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
            }
        } else if (stackingConstraints[itemOne][itemTwo] == 1) {

            // item one upon item two
            // min(col rating item one, row rating item two)
            int colItemOne = computeColRatingForUnmatchedItem(itemOne, stackingConstraints);
            int rowItemTwo = computeRowRatingForUnmatchedItem(itemTwo, stackingConstraints);
            if (min) {
                return colItemOne < rowItemTwo ? colItemOne : rowItemTwo;
            } else {
                return colItemOne > rowItemTwo ? colItemOne : rowItemTwo;
            }

        } else {

            // item two upon item one
            // min(col rating item two, row rating item one)
            int colItemTwo = computeColRatingForUnmatchedItem(itemTwo, stackingConstraints);
            int rowItemOne = computeRowRatingForUnmatchedItem(itemOne, stackingConstraints);
            if (min) {
                return colItemTwo < rowItemOne ? colItemTwo : rowItemOne;
            } else {
                return colItemTwo > rowItemOne ? colItemTwo : rowItemOne;
            }

        }
    }

    public static void assignSumRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getSumOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
        }
    }

    public static void assignMinRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, true));
        }
    }

    public static void assignMaxRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, false));
        }
    }

    public static void assignRowRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int rating = 0;

            for (int entry : stackingConstraints[edge.getVertexOne()]) {
                rating += entry;
            }
            for (int entry : stackingConstraints[edge.getVertexTwo()]) {
                rating += entry;
            }
            edge.setRating(rating);
        }
    }

    public static void assignColRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int rating = 0;

            // The rating is determined by the number of rows that have a one at position vertexOne or vertexTwo.
            // A high rating means that many items can be placed on top of the initial assignment.
            for (int i = 0; i < stackingConstraints.length; i++) {
                rating += (stackingConstraints[i][edge.getVertexOne()] + stackingConstraints[i][edge.getVertexTwo()]);
            }
            edge.setRating(rating);
        }
    }

    public static void assignColRatingToEdgesNewWay(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {

            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();

            if (itemsStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
                // return the max col rating of both items
                int colRatingOne = computeColRatingForUnmatchedItem(itemOne, stackingConstraints);
                int colRatingTwo = computeColRatingForUnmatchedItem(itemTwo, stackingConstraints);
                edge.setRating(colRatingOne > colRatingTwo ? colRatingOne : colRatingTwo);
            } else if (stackingConstraints[itemOne][itemTwo] == 1) {
                edge.setRating(computeColRatingForUnmatchedItem(itemOne, stackingConstraints));
            } else {
                edge.setRating(computeColRatingForUnmatchedItem(itemTwo, stackingConstraints));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> itemPairs, int[] items) {

        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge edge : itemPairs) {
            matchedItems.add(edge.getVertexOne());
            matchedItems.add(edge.getVertexTwo());
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : items) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    public static boolean isAlreadyUsedShuffle(ArrayList<Integer> currentShuffle, List<List<Integer>> alreadyUsedShuffles) {
        for (List<Integer> shuffle : alreadyUsedShuffles) {
            if (shuffle.equals(currentShuffle)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<MCMEdge> parseItemPairMCM(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> matchedItems = new ArrayList<>();
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
        return matchedItems;
    }

    public static ArrayList<ArrayList<Integer>> parseItemTripleMCM(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<ArrayList<Integer>> itemTriples = new ArrayList<>();

        for (Object edge : mcm.getMatching().getEdges()) {
            String parsedEdge = edge.toString().replace("(edge(", "").replace(") : v", ", ").replace(")", "").trim();
            int first = Integer.parseInt(parsedEdge.split(",")[0].trim());
            int second = Integer.parseInt(parsedEdge.split(",")[1].trim());
            int third = Integer.parseInt(parsedEdge.split(",")[2].trim());

            ArrayList<Integer> currAssignment = new ArrayList<>();
            currAssignment.add(first);
            currAssignment.add(second);
            currAssignment.add(third);
            itemTriples.add(new ArrayList<>(currAssignment));
        }

        return itemTriples;
    }

    public static int[] getArrayFromList(ArrayList<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * Updates the list of completely filled stacks and prepares the corresponding items to be removed from the remaining item pairs.
     *
     * @param itemPairRemovalList - the list to keep track of the items that should be removed form the remaining item pairs
     * @param itemOneEdge - the edge (item pair), the first item is assigned to
     * @param itemTwoEdge - the edge (item pair), the second item is assigned to
     * @param startingPair - the pair that is going to be assigned
     * @param itemOne - the first item to be assigned
     * @param itemTwo - the second item to be assigned
     * @param completelyFilledStacks - the list of completely filled stacks
     */
    public static void updateCompletelyFilledStacks(
            ArrayList<MCMEdge> itemPairRemovalList,
            MCMEdge itemOneEdge,
            MCMEdge itemTwoEdge,
            MCMEdge startingPair,
            int itemOne,
            int itemTwo,
            ArrayList<ArrayList<Integer>> completelyFilledStacks
    ) {

        itemPairRemovalList.add(itemOneEdge);
        itemPairRemovalList.add(itemTwoEdge);
        itemPairRemovalList.add(startingPair);

        ArrayList<Integer> itemOneStack = new ArrayList<>();
        itemOneStack.add(itemOne);
        itemOneStack.add(itemOneEdge.getVertexOne());
        itemOneStack.add(itemOneEdge.getVertexTwo());

        ArrayList<Integer> itemTwoStack = new ArrayList<>();
        itemTwoStack.add(itemTwo);
        itemTwoStack.add(itemTwoEdge.getVertexOne());
        itemTwoStack.add(itemTwoEdge.getVertexTwo());

        completelyFilledStacks.add(itemOneStack);
        completelyFilledStacks.add(itemTwoStack);
    }


    /**
     * Generates completely filled stacks from the list of item pairs.
     * (Breaks up a pair and tries to assign both items two new pairs to build up completely filled stacks).
     *
     * @param itemPairs - the list of item pairs
     * @param itemPairRemovalList - the list of edges (item pairs) to be removed
     * @param completelyFilledStacks - list to store the completely filled stacks
     */
    public static void generateCompletelyFilledStacks(
        ArrayList<MCMEdge> itemPairs, ArrayList<MCMEdge> itemPairRemovalList, ArrayList<ArrayList<Integer>> completelyFilledStacks, int[][] stackingConstraints
    ) {
        for (MCMEdge startingPair : itemPairs) {

            if (itemPairRemovalList.contains(startingPair)) { continue; }

            int itemOne = startingPair.getVertexOne();
            int itemTwo = startingPair.getVertexTwo();
            boolean itemOneAssigned = false;
            boolean itemTwoAssigned = false;
            MCMEdge itemOneEdge = new MCMEdge(0, 0, 0);
            MCMEdge itemTwoEdge = new MCMEdge(0, 0, 0);

            for (MCMEdge potentialTargetPair : itemPairs) {
                if (itemPairRemovalList.contains(potentialTargetPair)) { continue; }
                if (itemOneAssigned && itemTwoAssigned) { break; }

                if (startingPair != potentialTargetPair) {
                    int potentialTargetPairItemOne = potentialTargetPair.getVertexOne();
                    int potentialTargetPairItemTwo = potentialTargetPair.getVertexTwo();

                    if (!itemOneAssigned && HeuristicUtil.itemAssignableToPair(
                            itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)) {
                        itemOneAssigned = true;
                        itemOneEdge = potentialTargetPair;
                        continue;
                    }
                    if (!itemTwoAssigned && HeuristicUtil.itemAssignableToPair(
                            itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)) {
                        itemTwoAssigned = true;
                        itemTwoEdge = potentialTargetPair;
                        continue;
                    }
                }
            }

            if (itemOneAssigned && itemTwoAssigned) {
                updateCompletelyFilledStacks(itemPairRemovalList, itemOneEdge, itemTwoEdge, startingPair, itemOne, itemTwo, completelyFilledStacks);
            }
        }
    }

    /**
     * Returns whether the given item is validly assignable to the given pair.
     *
     * @param item - the item to be checked
     * @param pairItemOne - the first item of the pair
     * @param pairItemTwo - the second item of the pair
     * @return whether or not the item is assignable to the pair
     */
    public static boolean itemAssignableToPair(int item, int pairItemOne, int pairItemTwo, int[][] stackingConstraints) {

        // pair stackable in both directions
        if (HeuristicUtil.itemsStackableInBothDirections(pairItemOne, pairItemTwo, stackingConstraints)) {
            if (stackingConstraints[item][pairItemOne] == 1) {
                // itemOne above itemOneNew above itemTwoNew --> itemOne assigned
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                // itemTwoNew above itemOneNew above itemOne --> itemOne assigned
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                // itemOneNew above itemTwoNew above itemOne --> itemOne assigned
                return true;
            } else if (stackingConstraints[item][pairItemTwo] == 1) {
                // itemOne above itemTwoNew above itemOneNew --> itemOne assigned
                return true;
            }
            // pairItemOne above pairItemTwo
        } else if (stackingConstraints[pairItemOne][pairItemTwo] == 1) {
            if (stackingConstraints[item][pairItemOne] == 1) {
                return true;
            } else if (stackingConstraints[pairItemTwo][item] == 1) {
                return true;
            }
            // pairItemTwo above pairItemOne
        } else {
            if (stackingConstraints[item][pairItemTwo] == 1) {
                return true;
            } else if (stackingConstraints[pairItemOne][item] == 1) {
                return true;
            }
        }
        return false;
    }

    public static void parseAndAssignMinCostPerfectMatching(KuhnMunkresMinimalWeightBipartitePerfectMatching matching, int[][] stacks) {

        for (Object edge : matching.getMatching().getEdges()) {

            if (edge.toString().contains("triple")) {
                int itemOne = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[0].replace("[", "".trim()));
                int itemTwo = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[1].trim());
                int itemThree = Integer.parseInt(edge.toString().split(":")[0].replace("(triple", "").split(",")[2].replace("]", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());

                stacks[stack][0] = itemOne;
                stacks[stack][1] = itemTwo;
                stacks[stack][2] = itemThree;

            } else if (edge.toString().contains("pair")) {
                int itemOne = Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[0].replace("(", "").trim());
                int itemTwo = Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[1].replace(")", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());

                stacks[stack][0] = itemOne;
                stacks[stack][1] = itemTwo;
            } else if (edge.toString().contains("item")) {
                int item = Integer.parseInt(edge.toString().split(":")[0].replace("(item", "").trim());
                int stack = Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());

                stacks[stack][0] = item;
            }
        }
    }

    public static HashMap parseItemPairStackCombination(EdmondsMaximumCardinalityMatching mcm) {

        HashMap<Integer, ArrayList<Integer>> itemPairStackCombination = new HashMap<>();

        for (Object edge : mcm.getMatching().getEdges()) {
            int firstItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[0].trim());
            int secondItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[0].replace(")", "").trim());
            int stack = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[1].replace("stack", "").replace(")", "").trim());

            ArrayList<Integer> items = new ArrayList<>();
            items.add(firstItem);
            items.add(secondItem);
            itemPairStackCombination.put(stack, items);
        }

        return itemPairStackCombination;
    }

    public static ArrayList<Integer> getArrayListOfItems(int[] itemsArr) {
        ArrayList<Integer> items = new ArrayList<>();
        for (int item : itemsArr) {
            items.add(item);
        }
        return items;
    }

    public static ArrayList<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }

    public static int getRandomValueInBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    public static void copyStackAssignment(int[][] init, int[][] original, int[][] copy) {
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                copy[i][j] = init[i][j];
            }
        }
    }

    public static int getNumberOfItemsInStacks(int[][] stacks) {
        int numberOfItems = 0;
        for (int i = 0; i < stacks.length; i++) {
            for (int j = 0; j < stacks[i].length; j++) {
                if (stacks[i][j] != -1) {
                    numberOfItems++;
                }
            }
        }
        return numberOfItems;
    }

    public static boolean listContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){
            return true;
        }
        return false;
    }
}
