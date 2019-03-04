package SP.util;

import SP.representations.MCMEdge;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * A collection of graph related utility methods used in the heuristics.
 *
 * @author Tim Bohne
 */
public class GraphUtil {

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

    /**
     * Adds the edges from item pairs to stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge means that the item pair is assignable
     * to the stack. Each pair is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - the bipartite graph consisting of stacks and items
     * @param itemPairs      - the pairs of items to be connected to the stacks
     * @param stacks         - the stacks the item pairs are going to be connected to
     * @param costMatrix     - the matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesForItemPairs(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            ArrayList<MCMEdge> itemPairs,
            int[][] stacks,
            int[][] costMatrix
    ) {
        for (int pair = 0; pair < itemPairs.size(); pair++) {
            for (int stack = 0; stack < stacks.length; stack++) {
                // TODO: check necessary?
                if (!bipartiteGraph.containsEdge("pair" + itemPairs.get(pair), "stack" + stack)) {
                    DefaultWeightedEdge edge = bipartiteGraph.addEdge("pair" + itemPairs.get(pair), "stack" + stack);
                    int costs = costMatrix[itemPairs.get(pair).getVertexOne()][stack] + costMatrix[itemPairs.get(pair).getVertexTwo()][stack];
                    bipartiteGraph.setEdgeWeight(edge, costs);
                }
            }
        }
    }

    /**
     * Adds the edges from item triples to stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge means that the item triple is assignable
     * to the stack. Each triple is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - the bipartite graph consisting of stacks and items
     * @param itemTriples    - the triples of items to be connected to the stacks
     * @param stacks         - the stacks the item triples are going to be connected to
     * @param costMatrix     - the matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesForItemTriples(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            ArrayList<ArrayList<Integer>> itemTriples,
            int[][] stacks,
            int[][] costMatrix
    ) {
        for (int triple = 0; triple < itemTriples.size(); triple++) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("triple" + itemTriples.get(triple), "stack" + stack);
                int costs = costMatrix[itemTriples.get(triple).get(0)][stack]
                        + costMatrix[itemTriples.get(triple).get(1)][stack]
                        + costMatrix[itemTriples.get(triple).get(2)][stack];
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Adds the edges from dummy items to stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge means that the dummy item is assignable
     * to the stack. Each dummy is assignable to every stack at costs 0.
     *
     * @param bipartiteGraph - the bipartite graph consisting of stacks and items
     * @param dummyItems     - the dummy items to be connected to the stacks
     * @param stacks         - the stacks the dummy items are going to be connected to
     */
    public static void addEdgesForDummyItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            ArrayList<Integer> dummyItems,
            int[][] stacks
    ) {
        for (int item : dummyItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + item, "stack" + stack);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    /**
     * Adds the edges from unmatched items to stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge means that the item is assignable
     * to the stack. Each item is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - the bipartite graph consisting of stacks and items
     * @param unmatchedItems - the unmatched items to be connected to the stacks
     * @param stacks         - the stacks the items are going to be connected to
     * @param costMatrix     - the matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesForUnmatchedItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            ArrayList<Integer> unmatchedItems,
            int[][] stacks,
            int[][] costMatrix
    ) {
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                // TODO: check necessary?
                if (!bipartiteGraph.containsEdge("item" + item, "stack" + stack)) {
                    DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "stack" + stack);
                    int costs = costMatrix[item][stack];
                    bipartiteGraph.setEdgeWeight(edge, costs);
                }
            }
        }
    }

    /**
     * Introduces dummy vertices to the given bipartite graph.
     *
     * @param bipartiteGraph - the given bipartite graph consisting of items and stacks
     * @param partitionOne   - the bipartite graph's first partition
     * @param partitionTwo   - the bipartite graph's second partition
     * @return the created list of dummy items corresponding to the dummy vertices
     */
    public static ArrayList<Integer> introduceDummyVertices(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            Set<String> partitionOne,
            Set<String> partitionTwo
    ) {
        int cnt = 0;
        ArrayList<Integer> dummyItems = new ArrayList<>();
        while (partitionOne.size() < partitionTwo.size()) {
            bipartiteGraph.addVertex("dummy" + cnt);
            partitionOne.add("dummy" + cnt);
            dummyItems.add(cnt);
            cnt++;
        }
        return dummyItems;
    }

    /**
     * Adds an edge to the graph if the given unmatched item can be assigned
     * to the given pair to build up a valid item triple.
     *
     * @param graph               - the graph to be extended
     * @param lowerItemOfPair     - the lower item of the pair
     * @param unmatchedItem       - the unmatched item to be assigned to the pair
     * @param upperItemOfPair     - the upper item of the pair
     * @param itemPair            - the edge that represents the pair
     * @param stackingConstraints - the stacking constraints to be respected
     */
    public static void addEdgeForCompatibleItemTriple(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            int lowerItemOfPair,
            int unmatchedItem,
            int upperItemOfPair,
            MCMEdge itemPair,
            int[][] stackingConstraints
    ) {
        if (stackingConstraints[lowerItemOfPair][unmatchedItem] == 1 || stackingConstraints[unmatchedItem][upperItemOfPair] == 1) {
            if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
                graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
            }
        }
    }

    /**
     * Adds the item-pair vertices to the given bipartite graph.
     *
     * @param itemPairs      - the item pairs to be added as vertices
     * @param bipartiteGraph - the bipartite graph to be extended
     * @param partitionOne   - the partition the vertices are added to
     */
    public static void addVerticesForItemPairs(
        ArrayList<MCMEdge> itemPairs,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
        Set<String> partitionOne
    ) {
        for (MCMEdge edge : itemPairs) {
            bipartiteGraph.addVertex("edge" + edge);
            partitionOne.add("edge" + edge);
        }
    }

    /**
     * Adds the unmatched-item vertices to the given bipartite graph.
     *
     * @param unmatchedItems - the unmatched items to be added as vertices
     * @param bipartiteGraph - the bipartite graph to be extended
     * @param partitionOne   - the partition the vertices are added to
     */
    public static void addVerticesForUnmatchedItems(
        ArrayList<Integer> unmatchedItems,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
        Set<String> partitionOne
    ) {
        for (int item : unmatchedItems) {
            bipartiteGraph.addVertex("item" + item);
            partitionOne.add("item" + item);
        }
    }

    /**
     * Adds the stacks as vertices to the given bipartite graph.
     *
     * @param stacks         - the stacks to be added as vertices
     * @param bipartiteGraph - the bipartite graph to be extended
     * @param partitionTwo   - the partition the vertices are added to
     */
    public static void addVerticesForStacks(
        int[][] stacks,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
        Set<String> partitionTwo
    ) {
        for (int stack = 0; stack < stacks.length; stack++) {
            bipartiteGraph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }
    }

    /**
     * Generates the bipartite graph containing pairs of items in
     * one partition and unmatched items in the other one.
     *
     * @param itemPairs           - the list of item pairs
     * @param unmatchedItems      - the list of unmatched items
     * @param stackingConstraints - the stacking constraints to be respected
     * @return the generated bipartite graph
     */
    public static DefaultUndirectedGraph<String, DefaultEdge> generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems, int[][] stackingConstraints
    ) {

        DefaultUndirectedGraph<String, DefaultEdge> bipartiteGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        // adding the item pairs as nodes to the graph
        for (int i = 0; i < itemPairs.size(); i++) {
            bipartiteGraph.addVertex("edge" + itemPairs.get(i));
        }
        // adding the unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            bipartiteGraph.addVertex("v" + i);
        }

        for (int itemPair = 0; itemPair < itemPairs.size(); itemPair++) {
            for (int unmatchedItem = 0; unmatchedItem < unmatchedItems.size(); unmatchedItem++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done
                if (stackingConstraints[itemPairs.get(itemPair).getVertexOne()][itemPairs.get(itemPair).getVertexTwo()] == 1) {
                    GraphUtil.addEdgeForCompatibleItemTriple(
                            bipartiteGraph,
                            itemPairs.get(itemPair).getVertexTwo(),
                            unmatchedItems.get(unmatchedItem),
                            itemPairs.get(itemPair).getVertexOne(),
                            itemPairs.get(itemPair),
                            stackingConstraints
                    );
                } else {
                    GraphUtil.addEdgeForCompatibleItemTriple(
                            bipartiteGraph,
                            itemPairs.get(itemPair).getVertexOne(),
                            unmatchedItems.get(unmatchedItem),
                            itemPairs.get(itemPair).getVertexTwo(),
                            itemPairs.get(itemPair),
                            stackingConstraints
                    );
                }
            }
        }
        return bipartiteGraph;
    }

    /**
     * Generates the stacking constraint graph for the items.
     * There exists an edge between two items if the items are stackable in
     * at least one direction and if they can be assigned to at least one stack together.
     *
     * @param items               - the list of items (nodes of the graph)
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costMatrix          - the matrix containing the costs for item-stack-assignments
     * @param invalidEdgeCosts    - the cost value used to implement the placement constraints
     * @param stacks              - the given stacks in the storage area
     * @return the generated stacking constraint graph
     */
    public static DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraph(
            int[] items, int[][] stackingConstraints, int[][] costMatrix, int invalidEdgeCosts, int[][] stacks
    ) {
        DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        ArrayList<Integer> itemList = new ArrayList<>();
        for (int item : items) {
            itemList.add(item);
            stackingConstraintGraph.addVertex("v" + item);
        }
        for (int i = 0; i < stackingConstraints.length; i++) {
            for (int j = 0; j < stackingConstraints[0].length; j++) {

                // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1
                // and if they have at least one compatible stack.
                if (i != j && stackingConstraints[i][j] == 1 ||stackingConstraints[j][i] == 1) {
                    int numberOfCompatibleStacks = 0;
                    for (int stackIdx = 0; stackIdx < stacks.length; stackIdx++) {
                        if (costMatrix[i][stackIdx] < invalidEdgeCosts && costMatrix[j][stackIdx] < invalidEdgeCosts) {
                            numberOfCompatibleStacks++;
                        }
                    }
                    if (numberOfCompatibleStacks > 0) {
                        if (itemList.contains(i) && itemList.contains(j)) {
                            if (!stackingConstraintGraph.containsEdge("v" + j, "v" + i)) {
                                stackingConstraintGraph.addEdge("v" + i, "v" + j);
                            }
                        }
                    }
                }
            }
        }
        return stackingConstraintGraph;
    }

    /**
     * Returns a copy of the given edge list.
     *
     * @param edgeList - the edge list to be copied
     * @return the copied edge list
     */
    public static ArrayList<MCMEdge> getCopyOfEdgeList(ArrayList<MCMEdge> edgeList) {
        ArrayList<MCMEdge> edgeListCopy = new ArrayList<>();
        for (MCMEdge edge : edgeList) {
            edgeListCopy.add(new MCMEdge(edge));
        }
        return edgeListCopy;
    }

    /**
     * Parses item pairs from the given maximum cardinality matching.
     *
     * @param mcm - the given maximum cardinality matching
     * @return the list of matched items (edges)
     */
    public static ArrayList<MCMEdge> parseItemPairFromMCM(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> matchedItems = new ArrayList<>();
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
        return matchedItems;
    }

    /**
     * Parses item triples from the given maximum cardinality matching.
     *
     * @param mcm - the given maximum cardinality matching
     * @return the list of parsed item triples
     */
    public static ArrayList<ArrayList<Integer>> parseItemTripleFromMCM(EdmondsMaximumCardinalityMatching mcm) {
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

    /**
     * Parses the given minimum weight perfect matching and assigns it to the given stacks.
     *
     * @param mwpm   - the minimum weight perfect matching to be parsed
     * @param stacks - the stacks the parsed items are going to be assigned to
     */
    public static void parseAndAssignMinCostPerfectMatching(
            KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm, int[][] stacks
    ) {
        for (Object edge : mwpm.getMatching().getEdges()) {
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

    /**
     * Parses the combination of item pair and compatible stack.
     *
     * @param mcm - the maximum cardinality matching to be parsed
     * @return a map containing the stack and its assigned items
     */
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
}
