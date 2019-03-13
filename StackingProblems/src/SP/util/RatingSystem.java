package SP.util;

import SP.representations.MCMEdge;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of utility methods providing a rating system
 * for items / edges used in some of the heuristics.
 *
 * @author Tim Bohne
 */
public class RatingSystem {

    /**
     * Computes the row rating for the given unmatched item.
     * The row rating is the number of items the given item can be stacked upon.
     *
     * @param item                - the item for which the row rating gets computed
     * @param stackingConstraints - the stacking constraints the rating is based on
     * @return the item's row rating
     */
    public static int computeRowRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int entry : stackingConstraints[item]) {
            rating += entry;
        }
        return rating;
    }

    /**
     * Computes the column rating for the given unmatched item.
     * The column rating is the number of items that can be stacked upon the given item.
     *
     * @param item                - the item for which the col rating gets computed
     * @param stackingConstraints - teh stacking constraints the rating is based on
     * @return the item's col rating
     */
    public static int computeColRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int i = 0; i < stackingConstraints.length; i++) {
            rating += stackingConstraints[i][item];
        }
        return rating;
    }

    /**
     * Provides a map containing the ratings of the item pair.
     *
     * @param itemOne             - the first item of the pair
     * @param itemTwo             - the second item of the pair
     * @param stackingConstraints - the stacking constraints the ratings are based on
     * @return the map containing the ratings
     */
    public static HashMap<Integer, String> getRatingsMapForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        HashMap<Integer, String> itemRatings = new HashMap<>();
        itemRatings.put(RatingSystem.computeRowRatingForUnmatchedItem(itemOne, stackingConstraints), "itemOneRow");
        itemRatings.put(RatingSystem.computeColRatingForUnmatchedItem(itemOne, stackingConstraints), "itemOneCol");
        itemRatings.put(RatingSystem.computeRowRatingForUnmatchedItem(itemTwo, stackingConstraints), "itemTwoRow");
        itemRatings.put(RatingSystem.computeColRatingForUnmatchedItem(itemTwo, stackingConstraints), "itemTwoCol");
        return itemRatings;
    }

    /**
     * Provides a list of the ratings of the item pair.
     *
     * @param itemOne             - the first item of the pair
     * @param itemTwo             - the second item of the pair
     * @param stackingConstraints - the stacking constraints the ratings are based on
     * @return the list containing the ratings
     */
    public static ArrayList<Integer> getItemRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        HashMap<Integer, String> itemRatings = getRatingsMapForItemPair(itemOne, itemTwo, stackingConstraints);
        ArrayList<Integer> ratings = new ArrayList<>();
        for (int key : itemRatings.keySet()) {
            ratings.add(key);
        }
        return ratings;
    }

    /**
     * Returns the sum of the relevant ratings for the item pair.
     *
     * @param itemOne             - the first item of the pair
     * @param itemTwo             - the second item of the pair
     * @param stackingConstraints - the stacking constraints the ratings are based on
     * @return the sum of the relevant item ratings
     */
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

    /**
     * Returns the extreme value from the relevant ratings of the pair.
     *
     * @param itemOne             - the first item of the pair
     * @param itemTwo             - the second item of the pair
     * @param stackingConstraints - the stacking constraints the ratings are based on
     * @param min                 - specifies whether the min value should be returned (max otherwise)
     * @return the extreme rating value
     */
    public static int getExtremeOfRelevantRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints, boolean min) {
        if (HeuristicUtil.itemsStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Rates the given edges based on their items' compatibility to other edges.
     *
     * @param matchedItems        - the edges (pairs) to be rated
     * @param stackingConstraints - the stacking constraints to be respected
     */
    public static void assignCompatibilityRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int compatibility = 0;

            for (MCMEdge e : matchedItems) {
                if (e != edge) {
                    if (HeuristicUtil.itemAssignableToPair(itemOne, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)
                        || HeuristicUtil.itemAssignableToPair(itemTwo, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)) {
                            compatibility++;
                    }
                }
            }
            edge.setRating(compatibility);
        }
    }

    /**
     * Rates the given edges based on their extreme cost values.
     *
     * @param matchedItems        - the edges (pairs) to be rated
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the cost matrix
     * @param stacks              - the available stacks
     * @param max                 - determines whether the max value is used (min otherwise)
     */
    public static void assignExtremeCostRatingToEdges(
        ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints, int[][] costs, int[][] stacks, boolean max
    ) {
        for (MCMEdge edge : matchedItems) {
            ArrayList<Integer> costEntries = new ArrayList<>();
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;

            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[itemOne][stack] < forbiddenVal && costs[itemTwo][stack] < forbiddenVal) {
                    int currCost = costs[itemOne][stack] + costs[itemTwo][stack];
                    costEntries.add(currCost);
                }
            }
            if (max) {
                edge.setRating(Collections.max(costEntries));
            } else {
                edge.setRating(Collections.min(costEntries));
            }
        }
    }

    /**
     * Computes the average costs to assign the given pairs.
     * An incompatible stack is penalized with a high cost value of 1000.
     *
     * @param stacks       - the available stacks
     * @param costs        - the cost matrix
     * @param itemOne      - the first item of the pair
     * @param itemTwo      - the second item of the pair
     * @param forbiddenVal - the value indicating incompatible stacks
     * @return
     */
    public static int computeAvgCosts(int[][] stacks, int[][] costs, int itemOne, int itemTwo, int forbiddenVal) {
        int avgCosts = 0;
        for (int stack = 0; stack < stacks.length; stack++) {
            if (costs[itemOne][stack] < forbiddenVal && costs[itemTwo][stack] < forbiddenVal) {
                int currCost = costs[itemOne][stack] + costs[itemTwo][stack];
                avgCosts += currCost;
            } else {
                avgCosts += 1000;
            }
        }
        return avgCosts / stacks.length;
    }

    /**
     * Computes an item's pair compatibility which is the number of other edges it is assignable to.
     *
     * @param pairs               - the list of item pairs to be checked
     * @param item                - the item to compute the compatibility for
     * @param stackingConstraints - the stacking constraints to compatibility is based on
     * @param itemEdge            - the edge the item to be checked is part of
     * @return
     */
    public static int computePairCompatibility(ArrayList<MCMEdge> pairs, int item, int[][] stackingConstraints, MCMEdge itemEdge) {
        int pairCompatibility = 0;
        for (MCMEdge pair : pairs) {
            if (itemEdge != pair) {
                if (HeuristicUtil.itemAssignableToPair(item, pair.getVertexOne(), pair.getVertexTwo(), stackingConstraints)) {
                    pairCompatibility++;
                }
            }
        }
        return pairCompatibility;
    }

    public static int getAvg(ArrayList<Integer> values) {
        int avg = 0;
        for (int val : values) {
            avg += val;
        }
        return avg / values.size();
    }

    /**
     * A rating system developed for the 3Cap heuristic.
     *
     * @param matchedItems        - the edges (pairs) to be rated
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the cost matrix
     * @param stacks              - the available stacks
     * @param deviationThreshold  - determines whether an edge is rated higher then 0
     */
    public static void assignThreeCapPairRating(
        ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints, int[][] costs, int[][] stacks, int deviationThreshold
    ) {
        ArrayList<ArrayList<Integer>> listOfPairRatings = new ArrayList<>();
        ArrayList<Integer> compatibilities = new ArrayList<>();
        ArrayList<Integer> avgCostValues = new ArrayList<>();

        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;

            int pairCompatibilityItemOne = computePairCompatibility(matchedItems, itemOne, stackingConstraints, edge);
            int pairCompatibilityItemTwo = computePairCompatibility(matchedItems, itemTwo, stackingConstraints, edge);
            int worstCompatibility = pairCompatibilityItemOne > pairCompatibilityItemTwo ? pairCompatibilityItemTwo : pairCompatibilityItemOne;

            int avgCosts = computeAvgCosts(stacks, costs, itemOne, itemTwo, forbiddenVal);

            ArrayList<Integer> pairRatings = new ArrayList<>();
            pairRatings.add(worstCompatibility);
            pairRatings.add(avgCosts);

            listOfPairRatings.add(pairRatings);
            compatibilities.add(worstCompatibility);
            avgCostValues.add(avgCosts);
        }

        int avgCompatibility = getAvg(compatibilities);
        int avgCosts = getAvg(avgCostValues);

        for (int i = 0; i < matchedItems.size(); i++) {
            int rating = 0;
            if (listOfPairRatings.get(i).get(0) > avgCompatibility && listOfPairRatings.get(i).get(1) > avgCosts) {
                int compatibilityDeviation = HeuristicUtil.getPercentageDeviation(avgCompatibility, listOfPairRatings.get(i).get(0));
                int costDeviation = HeuristicUtil.getPercentageDeviation(avgCosts, listOfPairRatings.get(i).get(1));
                if (compatibilityDeviation + costDeviation > deviationThreshold) {
                    rating = compatibilityDeviation + costDeviation;
                }
            }
            matchedItems.get(i).setRating(rating);
        }
    }

    /**
     * Rates the pairs based on their avg costs and a bonus if the items are stackable in both directions.
     *
     * @param matchedItems        - the edges (pairs) to be rated
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the cost matrix
     * @param stacks              - the available stacks
     */
    public static void assignAvgCostsRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints, int[][] costs, int[][] stacks) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;

            int bonus = 0;
            // items that are stackable in both directions shouldn't be separated
            if (HeuristicUtil.itemsStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
                bonus = 1500;
            }
            int avgCosts = computeAvgCosts(stacks, costs, itemOne, itemTwo, forbiddenVal);
            int rating = avgCosts - bonus;
            edge.setRating(rating);
        }
    }

    /**
     * Rates the edges based on a combination of several relevant values.
     *
     * @param matchedItems        - the edges (pairs) to be rated
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the cost matrix
     * @param stacks              - the available stacks
     */
    public static void assignNewRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints, int[][] costs, int[][] stacks) {

        for (MCMEdge edge : matchedItems) {
            ArrayList<Integer> costEntries = new ArrayList<>();
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;
            int avgCosts = 0;
            int cnt = 0;

            // compatibility with other pairs
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int compatibility = 0;

            for (MCMEdge e : matchedItems) {
                if (e != edge) {
                    if (HeuristicUtil.itemAssignableToPair(itemOne, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)
                        || HeuristicUtil.itemAssignableToPair(itemTwo, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)) {
                            compatibility++;
                    }
                }
            }
            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[itemOne][stack] < forbiddenVal && costs[itemTwo][stack] < forbiddenVal) {
                    int currCost = costs[itemOne][stack] + costs[itemTwo][stack];
                    costEntries.add(currCost);
                    avgCosts += costs[itemOne][stack] + costs[itemTwo][stack];
                    cnt++;
                }
            }
            edge.setRating((compatibility + cnt) / ((avgCosts / cnt) ));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Assigns the sum of the ratings to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
    public static void assignSumRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getSumOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
        }
    }

    /**
     * Assigns the minimum rating to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
    public static void assignMinRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, true));
        }
    }

    /**
     * Assigns the maximum rating to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
    public static void assignMaxRatingToEdges(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, false));
        }
    }

    /**
     * Assigns the row rating to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
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

    /**
     * Assigns the col rating to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
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

    /**
     * Assigns the col rating to the specified edges.
     *
     * @param matchedItems        - the matched items (edges) to be rated
     * @param stackingConstraints - the stacking constraints the ratings are based on
     */
    public static void assignColRatingToEdgesNewWay(ArrayList<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {

            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();

            if (HeuristicUtil.itemsStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
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

    /**
     * Returns the list of unmatched items increasingly sorted by col rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public static ArrayList<Integer> getUnmatchedItemsSortedByColRating(ArrayList<Integer> unmatchedItems, int[][] stackingConstraints) {
        HashMap<Integer, Integer> unmatchedItemColRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            unmatchedItemColRatings.put(item, RatingSystem.computeColRatingForUnmatchedItem(item, stackingConstraints));
        }
        Map<Integer, Integer> sortedItemColRatings = HeuristicUtil.sortMapByValue(unmatchedItemColRatings);
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
            int rating = RatingSystem.computeRowRatingForUnmatchedItem(item, stackingConstraints);
            unmatchedItemRowRatings.put(item, rating);
        }
        ArrayList<Integer> unmatchedItemsSortedByRowRating = new ArrayList<>();
        Map<Integer, Integer> sortedItemRowRatings = HeuristicUtil.sortMapByValue(unmatchedItemRowRatings);
        for (int item : sortedItemRowRatings.keySet()) {
            unmatchedItemsSortedByRowRating.add(item);
        }
        return unmatchedItemsSortedByRowRating;
    }

    /**
     * Applies each edge rating system to a copy of the item pair list.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public static void applyRatingSystemsToItemPairPermutations(ArrayList<ArrayList<MCMEdge>> itemPairPermutations, int[][] stackingConstraints) {
        int idx = 0;
        RatingSystem.assignRowRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignColRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignMaxRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignMinRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
        RatingSystem.assignSumRatingToEdges(itemPairPermutations.get(idx++), stackingConstraints);
    }
}
