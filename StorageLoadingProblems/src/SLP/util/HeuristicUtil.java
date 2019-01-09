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

    public static void generateStackingConstraintGraph(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            int[] items,
            int[][] stackingConstraints
    ) {

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

    public static int computeRowRatingForEdgesNewWay(int itemOne, int itemTwo, int[][] stackingConstraints) {
        int rating = 0;

        if (stackingConstraints[itemOne][itemTwo] == 1 && stackingConstraints[itemTwo][itemOne] == 1) {
            int ratingOne = 0;
            for (int entry : stackingConstraints[itemOne]) {
                ratingOne += entry;
            }
            int ratingTwo = 0;
            for (int entry : stackingConstraints[itemOne]) {
                ratingTwo += entry;
            }
            rating = ratingOne > ratingTwo ? ratingOne : ratingTwo;
        } else if (stackingConstraints[itemOne][itemTwo] == 1) {
            for (int entry : stackingConstraints[itemTwo]) {
                rating += entry;
            }
        } else if (stackingConstraints[itemTwo][itemOne] == 1) {
            for (int entry : stackingConstraints[itemOne]) {
                rating += entry;
            }
        }

        return rating;
    }

    public static void assignRowRatingToEdgesNewWay(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        // determine the item that is below the other one
        // if both ways are possible, the item that is more flexible is chosen
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(HeuristicUtil.computeRowRatingForEdgesNewWay(itemOne, itemTwo, stackingConstraints));
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

    public static int computeColRatingForEdgesNewWay(int itemOne, int itemTwo, int[][] stackingConstraints) {
        int rating = 0;

        if (stackingConstraints[itemOne][itemTwo] == 1 && stackingConstraints[itemTwo][itemOne] == 1) {

            int ratingOne = 0;
            for (int i = 0; i < stackingConstraints.length; i++) {
                ratingOne += (stackingConstraints[i][itemOne]);
            }
            int ratingTwo = 0;
            for (int i = 0; i < stackingConstraints.length; i++) {
                ratingTwo += (stackingConstraints[i][itemTwo]);
            }

            rating = ratingOne > ratingTwo ? ratingOne : ratingTwo;

        } else if (stackingConstraints[itemOne][itemTwo] == 1) {

            for (int i = 0; i < stackingConstraints.length; i++) {
                rating += (stackingConstraints[i][itemOne]);
            }

        } else if (stackingConstraints[itemTwo][itemOne] == 1) {
            for (int i = 0; i < stackingConstraints.length; i++) {
                rating += (stackingConstraints[i][itemTwo]);
            }
        }

        return rating;
    }

    public static void assignColRatingToEdgesNewWay(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(HeuristicUtil.computeColRatingForEdgesNewWay(itemOne, itemTwo, stackingConstraints));
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

    public static void parseItemPairMCM(ArrayList<MCMEdge> matchedItems, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
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

    public static boolean listContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){
            return true;
        }
        return false;
    }
}
