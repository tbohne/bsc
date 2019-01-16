package SLP.util;

import SLP.representations.MCMEdge;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class HeuristicUtil {

    public static EdmondsMaximumCardinalityMatching<String, DefaultEdge> getMCMForUnassignedItems(ArrayList<Integer> unassignedItems, int[][] stackingConstraints) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int item : unassignedItems) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < unassignedItems.size(); i++) {
            for (int j = 0; j < unassignedItems.size(); j++) {
                if (unassignedItems.get(i) != unassignedItems.get(j) && stackingConstraints[unassignedItems.get(i)][unassignedItems.get(j)] == 1
                        || stackingConstraints[unassignedItems.get(j)][unassignedItems.get(i)] == 1) {

                    if (!graph.containsEdge("v" + unassignedItems.get(j), "v" + unassignedItems.get(i))) {
                        graph.addEdge("v" + unassignedItems.get(i), "v" + unassignedItems.get(j));
                    }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        return mcm;
    }

    public static void generateStackingConstraintGraphNewWay(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            int[] items,
            int[][] stackingConstraints,
            int[][] stacks,
            int[][] stackConstraints
    ) {

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
                        if (stackConstraints[i][stackIdx] == 1 && stackConstraints[j][stackIdx] == 1) {
                            numberOfCompatibleStacks++;
                        }
                    }

                    // TODO: find reasonable way to calculate the value
                    if (numberOfCompatibleStacks > 0) {
                        if (!graph.containsEdge("v" + j, "v" + i)) {
                            graph.addEdge("v" + i, "v" + j);
                        }
                    }
                }
            }
        }
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

    public static boolean itemPairAndStackCompatible(int stackIdx, int itemOne, int itemTwo, int[][] stackConstraints) {
        return stackConstraints[itemOne][stackIdx] == 1 && stackConstraints[itemTwo][stackIdx] == 1;
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

    public static void parseItemTripleMCM(ArrayList<ArrayList<Integer>> currentStackAssignments, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching().getEdges()) {
            String parsedEdge = edge.toString().replace("(edge(", "").replace(") : v", ", ").replace(")", "").trim();
            int first = Integer.parseInt(parsedEdge.split(",")[0].trim());
            int second = Integer.parseInt(parsedEdge.split(",")[1].trim());
            int third = Integer.parseInt(parsedEdge.split(",")[2].trim());

            ArrayList<Integer> currAssignment = new ArrayList<>();
            currAssignment.add(first);
            currAssignment.add(second);
            currAssignment.add(third);
            currentStackAssignments.add(new ArrayList<>(currAssignment));
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
