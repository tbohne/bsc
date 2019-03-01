package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import SP.util.MapUtil;
import SP.util.RatingSystem;
import com.google.common.collect.Collections2;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class ThreeCapPermutationHeuristic {

    private final int COMPLETE_PERMUTATION_LIMIT = 8;
    private final int ITEM_PAIR_PERMUTATIONS = 40000;
    private final int ITEM_PERMUTATIONS = 500;

    private final int NUMER_OF_USED_EDGE_RATING_SYSTEMS = 5;

    private Instance instance;
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;
    private ArrayList<List<Integer>> alreadyUsedShuffles;
    private double startTime;
    private int timeLimit;
    private int priorizationFactor;

    private Solution bestSolution;

    public ThreeCapPermutationHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        this.alreadyUsedShuffles = new ArrayList<>();
        this.priorizationFactor = (int)Math.ceil((double)(instance.getItems().length) / 50.0);
        this.bestSolution = new Solution();
    }

    /************************************ TODO: UNUSED ATM ************************************/
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

    public void addUnmatchedItemPermutations(ArrayList<Integer> initialListOfUnmatchedItems, ArrayList<List<Integer>> unmatchedItemPermutations) {
        // For up to 8 items, the computation of permutations is possible in a reasonable time frame,
        // after that 40k random shuffles are used instead.
        if (initialListOfUnmatchedItems.size() <= COMPLETE_PERMUTATION_LIMIT) {
            for (List<Integer> itemList : Collections2.permutations(initialListOfUnmatchedItems)) {
                unmatchedItemPermutations.add(new ArrayList<>(itemList));
            }
        } else {
            for (int i = 0; i < ITEM_PERMUTATIONS; i++) {
                unmatchedItemPermutations.add(new ArrayList<>(initialListOfUnmatchedItems));
                Collections.shuffle(initialListOfUnmatchedItems);

                int unsuccessfulShuffleAttempts = 0;
                while (HeuristicUtil.isAlreadyUsedShuffle(initialListOfUnmatchedItems, this.alreadyUsedShuffles)) {
                    System.out.println("already");
                    Collections.shuffle(initialListOfUnmatchedItems);
                    if (unsuccessfulShuffleAttempts == 10) {
                        return;
                    }
                    unsuccessfulShuffleAttempts++;
                }
                this.alreadyUsedShuffles.add(new ArrayList<>(initialListOfUnmatchedItems));
            }
        }
    }
    /******************************************************************************************/

    /**
     * Returns the list of unmatched items increasingly sorted by row rating.
     *
     * @param unmatchedItems - the unsorted list of unmatched items
     * @return the sorted list of unmatched items
     */
    public ArrayList<Integer> getUnmatchedItemsSortedByRowRating(ArrayList<Integer> unmatchedItems) {

        HashMap<Integer, Integer> unmatchedItemRowRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            int rating = RatingSystem.computeRowRatingForUnmatchedItem(item, this.instance.getStackingConstraints());
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
            unmatchedItemColRatings.put(item, RatingSystem.computeColRatingForUnmatchedItem(item, this.instance.getStackingConstraints()));
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

        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItemsFromPairs(matchedItems, this.instance.getItems()));
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
     * Prioritizes the assignment of edges that are inflexible in terms of the used rating system (row / col).
     *
     * @param matchedItems - the list of item pairs
     * @param prioritizedEdges - the list of edges that are prioritized
     */
    public void prioritizeInflexibleEdges(ArrayList<MCMEdge> matchedItems, ArrayList<MCMEdge> prioritizedEdges) {

        for (MCMEdge edge : matchedItems) {

            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();

//            System.out.println("edge rating: " + edge.getRating());
//            System.out.println("prio: " + this.priorizationFactor);

            if (edge.getRating() <= this.priorizationFactor) {

                for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                    if (!HeuristicUtil.stackEmpty(stack, this.instance.getStacks())
                        || !HeuristicUtil.itemPairAndStackCompatible(stack, itemOne, itemTwo, this.instance.getCosts(),
                            Integer.MAX_VALUE / this.instance.getItems().length)) { continue; }


//                    System.out.println("prioritizing: " + itemOne + ", " + itemTwo);
                    prioritizedEdges.add(new MCMEdge(itemOne, itemTwo, 0));
                    this.assignPairInReasonableOrder(stack, itemOne, itemTwo);
                    break;
                }
            }
        }
    }

    /**
     * Assigns the given item to the first position it is feasibly assignable to.
     *
     * @param item - the item that is going to be assigned
     * @return whether or not the assignment of the item was successful
     */
    public boolean assignItemToFirstPossiblePosition(int item) {

        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

            // item and stack are incompatible
            if (this.instance.getCosts()[item][stack] >= Integer.MAX_VALUE / this.instance.getItems().length) { continue; }

            // empty stack
            if (HeuristicUtil.stackEmpty(stack, this.instance.getStacks())) {
                this.instance.getStacks()[stack][2] = item;
                return true;
            // items at 2nd and 3rd level
            } else if (this.instance.getStacks()[stack][2] == -1) {
                if (this.instance.getStackingConstraints()[this.instance.getStacks()[stack][1]][item] == 1) {
                    this.instance.getStacks()[stack][2] = item;
                    return true;
                }
            // items at 1st and 2nd level
            } else if (this.instance.getStacks()[stack][2] != -1 && this.instance.getStacks()[stack][1] != -1) {
                if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][1]] == 1) {
                    this.instance.getStacks()[stack][0] = item;
                    return true;
                }
            // only one item at ground level
            } else {
                if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][2]] == 1) {
                    this.instance.getStacks()[stack][1] = item;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Prioritizes the assignment of items that are inflexible in terms of the number of items they can be placed upon.
     *
     * @param unmatchedItems - the items that aren't paired
     * @return whether or not the assignment of the inflexible items was successful
     */
    public boolean prioritizeInflexibleItems(List<Integer> unmatchedItems) {
        for (int item : unmatchedItems) {

            if (RatingSystem.computeRowRatingForUnmatchedItem(item, this.instance.getStackingConstraints()) <= this.priorizationFactor
                && RatingSystem.computeColRatingForUnmatchedItem(item, this.instance.getStackingConstraints()) <= this.priorizationFactor) {
//                    System.out.println("prioritizing: " + item);
                    this.unstackableItems.add(item);
                    if (!this.assignItemToFirstPossiblePosition(item)) {
                        return false;
                    }
            }
        }
        return true;
    }

    public boolean assignFinallyUnmatchedItemsInDifferentOrders(ArrayList<Integer> stillUnmatchedItems) {

//        System.out.println("finally unmatched: " + stillUnmatchedItems);

        ArrayList<Integer> stillUnmatchedBackup = new ArrayList<>(stillUnmatchedItems);
        ArrayList<Integer> toRemoveAgain;

        // ROW RATING
        stillUnmatchedItems = this.getUnmatchedItemsSortedByRowRating(stillUnmatchedItems);
        toRemoveAgain = new ArrayList<>(stillUnmatchedItems);
        this.assignSortedListOfUnmatchedItems(stillUnmatchedItems);
        if (HeuristicUtil.getNumberOfItemsAssignedToStacks(this.instance.getStacks()) == this.instance.getItems().length) { return true; }

        this.instance.removeItemListFromStorageArea(toRemoveAgain);

        // COL RATING
        stillUnmatchedItems = this.getUnmatchedItemsSortedByColRating(stillUnmatchedBackup);
        toRemoveAgain = new ArrayList<>(stillUnmatchedItems);
        this.assignSortedListOfUnmatchedItems(stillUnmatchedItems);

        if (HeuristicUtil.getNumberOfItemsAssignedToStacks(this.instance.getStacks()) == this.instance.getItems().length) { return true; }

        this.instance.removeItemListFromStorageArea(toRemoveAgain);

        // ROW RATING REVERSED
        stillUnmatchedItems = this.getUnmatchedItemsSortedByRowRating(stillUnmatchedBackup);
        toRemoveAgain = new ArrayList<>(stillUnmatchedItems);
        Collections.reverse(stillUnmatchedItems);
        this.assignSortedListOfUnmatchedItems(stillUnmatchedItems);

        if (HeuristicUtil.getNumberOfItemsAssignedToStacks(this.instance.getStacks()) == this.instance.getItems().length) { return true; }

        this.instance.removeItemListFromStorageArea(toRemoveAgain);

        // COL RATING REVERSED
        stillUnmatchedItems = this.getUnmatchedItemsSortedByColRating(stillUnmatchedBackup);
        Collections.reverse(stillUnmatchedItems);
        this.assignSortedListOfUnmatchedItems(stillUnmatchedItems);

        if (HeuristicUtil.getNumberOfItemsAssignedToStacks(this.instance.getStacks()) == this.instance.getItems().length) { return true; }

        return false;
    }

    /**
     * Fills the storage area with the item pairs and the remaining unmatched items.
     *
     * @param matchedItems - the items that are grouped together in pairs
     * @param unmatchedItems - the items that are't paired together
     * @return whether or not the storing procedure was successful
     */
    public boolean fillStorageArea(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {

        if (matchedItems.size() * 2 + unmatchedItems.size() != this.instance.getItems().length) {
            System.out.println("PROBLEM: number of matched items + number of unmatched items != number of items");
            System.exit(0);
        }

        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        ArrayList<MCMEdge> prioritizedEdges = new ArrayList<>();

        this.prioritizeInflexibleEdges(matchedItems, prioritizedEdges);
        if (!this.prioritizeInflexibleItems(unmatchedItems)) { return false; }
        this.processMatchedItems(matchedItems, prioritizedEdges);

        ArrayList<Integer> stillUnmatchedItems = new ArrayList<>(unmatchedItems);
        this.updateUnmatchedItems(stillUnmatchedItems);

        this.assignFinallyUnmatchedItemsInDifferentOrders(stillUnmatchedItems);
        return true;
    }

    /**
     * Generates a maximum cardinality matching for the remaining unmatched item and tries to assign
     * these items as pairs to the remaining free stack positions.
     *
     * @param unmatchedItems - the remaining unmatched items
     */
    public void tryToAssignRemainingItemsAsPairs(List<Integer> unmatchedItems) {

        EdmondsMaximumCardinalityMatching mcm = GraphUtil.getMCMForItemList((ArrayList<Integer>) unmatchedItems, this.instance.getStackingConstraints());
        ArrayList<MCMEdge> itemPairs = GraphUtil.parseItemPairFromMCM(mcm);

        ArrayList<Integer> toBeRemoved = new ArrayList<>();

        for (MCMEdge itemPair : itemPairs) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                if (this.instance.getCosts()[itemPair.getVertexOne()][stack] >= Integer.MAX_VALUE / this.instance.getItems().length
                    || this.instance.getCosts()[itemPair.getVertexTwo()][stack] >= Integer.MAX_VALUE / this.instance.getItems().length) {
                        continue;
                }

                if (this.instance.getStacks()[stack][2] == -1) {
                    if (this.instance.getStackingConstraints()[itemPair.getVertexOne()][itemPair.getVertexTwo()] == 1) {
                        this.instance.getStacks()[stack][2] = itemPair.getVertexTwo();
                        this.instance.getStacks()[stack][1] = itemPair.getVertexOne();
                    } else {
                        this.instance.getStacks()[stack][2] = itemPair.getVertexOne();
                        this.instance.getStacks()[stack][1] = itemPair.getVertexTwo();
                    }
                    toBeRemoved.add(itemPair.getVertexOne());
                    toBeRemoved.add(itemPair.getVertexTwo());
                    break;
                } else if (this.instance.getStacks()[stack][1] == -1 && this.instance.getStacks()[stack][0] == -1) {
                    if (this.instance.getStackingConstraints()[itemPair.getVertexOne()][itemPair.getVertexTwo()] == 1) {
                        if (this.instance.getStackingConstraints()[itemPair.getVertexTwo()][this.instance.getStacks()[stack][2]] == 1) {
                            this.instance.getStacks()[stack][1] = itemPair.getVertexTwo();
                            this.instance.getStacks()[stack][0] = itemPair.getVertexOne();
                            toBeRemoved.add(itemPair.getVertexOne());
                            toBeRemoved.add(itemPair.getVertexTwo());
                            break;
                        }
                    } else {
                        if (this.instance.getStackingConstraints()[itemPair.getVertexOne()][this.instance.getStacks()[stack][2]] == 1) {
                            this.instance.getStacks()[stack][1] = itemPair.getVertexOne();
                            this.instance.getStacks()[stack][0] = itemPair.getVertexTwo();
                            toBeRemoved.add(itemPair.getVertexOne());
                            toBeRemoved.add(itemPair.getVertexTwo());
                            break;
                        }
                    }
                }
            }
        }
        for (int item : toBeRemoved) {
            unmatchedItems.remove(unmatchedItems.indexOf(item));
        }
    }

    /**
     * Assigns as many of the finally unmatched items as possible to the remaining free positions in the stacks.
     *
     * @param unmatchedItems - the unmatched items that are going to be assigned
     */
    public void assignSortedListOfUnmatchedItems(List<Integer> unmatchedItems) {

        this.tryToAssignRemainingItemsAsPairs(unmatchedItems);

        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                // item and stack incompatible
                if (this.instance.getCosts()[item][stack] >= Integer.MAX_VALUE / this.instance.getItems().length) { continue; }

                // completely free
                if (this.instance.getStacks()[stack][2] == -1 && this.instance.getStacks()[stack][1] == -1 && this.instance.getStacks()[stack][0] == -1) {
                    this.instance.getStacks()[stack][2] = item;
                    break;
                // only ground free
                } else if (this.instance.getStacks()[stack][2] == -1 && this.instance.getStacks()[stack][1] != -1) {
                    if (this.instance.getStackingConstraints()[this.instance.getStacks()[stack][1]][item] == 1) {
                        this.instance.getStacks()[stack][2] = item;
                        break;
                    }
                // only top level free
                } else if (this.instance.getStacks()[stack][0] == -1 && this.instance.getStacks()[stack][1] != -1) {
                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][1]] == 1) {
                        this.instance.getStacks()[stack][0] = item;
                        break;
                    }
                // only ground level blocked
                } else if (this.instance.getStacks()[stack][2] != -1 && this.instance.getStacks()[stack][1] == -1) {
                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][2]] == 1) {
                        this.instance.getStacks()[stack][1] = item;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Updates the list of unmatched items.
     *
     * @param unmatchedItems - the list to be updated
     */
    public void updateUnmatchedItems(ArrayList<Integer> unmatchedItems) {

        unmatchedItems.addAll(this.additionalUnmatchedItems);

        ArrayList<Integer> toBeRemoved = new ArrayList<>();
        for (int item : unmatchedItems) {
            if (this.unstackableItems.contains(item)) {
                toBeRemoved.add(item);
            }
        }

        while (toBeRemoved.size() > 0) {
            int idx = unmatchedItems.indexOf(toBeRemoved.get(0));
            unmatchedItems.remove(idx);
            toBeRemoved.remove(0);
        }
    }

    /**
     * Assigns the given item pair to the specified stack.
     *
     * @param stackIdx - specifies the stack, the items are placed in
     * @param below - the item that is placed below
     * @param above - the item that is placed above
     * @return whether or not the assignment was successful
     */
    public boolean assignItemPairToStack(int stackIdx, int below, int above, boolean ground) {

        // at least one of the items is not compatible with the stack
        if (this.instance.getCosts()[below][stackIdx] >= Integer.MAX_VALUE / this.instance.getItems().length
                || this.instance.getCosts()[above][stackIdx] >= Integer.MAX_VALUE / this.instance.getItems().length) {
            return false;
        }

        if (this.instance.getStacks()[stackIdx][2] == -1 && this.instance.getStacks()[stackIdx][1] == -1 && this.instance.getStacks()[stackIdx][0] == -1) {

            if (ground) {
                // place on ground level
                this.instance.getStacks()[stackIdx][2] = below;
                this.instance.getStacks()[stackIdx][1] = above;
                return true;
            } else {
                // don't place on ground level
                this.instance.getStacks()[stackIdx][1] = below;
                this.instance.getStacks()[stackIdx][0] = above;
                return true;
            }
        }

        // assign to 1st and 2nd level
        else if (this.instance.getStacks()[stackIdx][2] == -1 && this.instance.getStacks()[stackIdx][1] == -1) {
            // TODO: this case is no longer possible
            this.instance.getStacks()[stackIdx][2] = below;
            this.instance.getStacks()[stackIdx][1] = above;
            return true;
        // assign to 2nd and 3rd level
        } else if (this.instance.getStacks()[stackIdx][1] == -1 && this.instance.getStacks()[stackIdx][0] == -1
                && this.instance.getStackingConstraints()[below][this.instance.getStacks()[stackIdx][2]] == 1) {
            this.instance.getStacks()[stackIdx][1] = below;
            this.instance.getStacks()[stackIdx][0] = above;
            return true;
        }
        // no two free positions
        return false;
    }

    /**
     * Assigns the given pair of items in a reasonable order to the specified stack.
     *
     * @param stack - the stack, the items get assigned to
     * @param itemOne - the first item of the pair
     * @param itemTwo - the second item of the pair
     * @return whether or not the assignment was successful
     */
    public boolean assignPairInReasonableOrder(int stack, int itemOne, int itemTwo) {

        if (HeuristicUtil.itemsStackableInBothDirections(itemOne, itemTwo, this.instance.getStackingConstraints())) {

            // The maximum is the most promising order.
            switch (RatingSystem.getRatingsMapForItemPair(itemOne, itemTwo, this.instance.getStackingConstraints()).get(
                RatingSystem.getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, this.instance.getStackingConstraints(), false))) {

                    case "itemOneRow":
                        // not ground - item one below - desirable: good row rating for item one
                        if (this.assignItemPairToStack(stack, itemOne, itemTwo, false)) { return true; }
                        break;
                    case "itemOneCol":
                        // ground - item one above - desirable: good col rating for item one
                        if (this.assignItemPairToStack(stack, itemTwo, itemOne, true)) { return true; }
                        break;
                    case "itemTwoRow":
                        // not ground - item one above - desirable: good good row rating item two
                        if (this.assignItemPairToStack(stack, itemTwo, itemOne, false)) { return true; }
                        break;
                    case "itemTwoCol":
                        // ground - item one below - desirable: good col rating item two
                        if (this.assignItemPairToStack(stack, itemOne, itemTwo, true)) { return true; }
            }

        } else {
            if (this.instance.getStackingConstraints()[itemOne][itemTwo] == 1) {
                if (this.assignItemPairToStack(stack, itemTwo, itemOne, true)) { return true; }
            } else {
                if (this.assignItemPairToStack(stack, itemOne, itemTwo, true)) { return true; }
            }
        }
        return false;
    }

    /**
     * Handles the assignment of the items connected by the given edge.
     *
     * @param edge - the edge connecting the item pair
     * @return whether the assignment was successful
     */
    public boolean handleItemPairAssignmentForEdge(MCMEdge edge) {

        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();

            if (this.assignPairInReasonableOrder(stack, itemOne, itemTwo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes the list of matched items.
     *
     * @param matchedItems - the edges connecting pairs of items
     * @param prioritizedEdges - the list of edges that are prioritized
     */
    public void processMatchedItems(ArrayList<MCMEdge> matchedItems, ArrayList<MCMEdge> prioritizedEdges) {
        for (MCMEdge edge : matchedItems) {

            boolean continueOuterLoop = false;
            for (MCMEdge e : prioritizedEdges) {
                if (e.getVertexOne() == edge.getVertexOne() && e.getVertexTwo() == edge.getVertexTwo()) {
                    continueOuterLoop = true;
                    break;
                }
            }
            if (continueOuterLoop) { continue; }

            if (!this.handleItemPairAssignmentForEdge(edge)) {
                this.additionalUnmatchedItems.add(edge.getVertexOne());
                this.additionalUnmatchedItems.add(edge.getVertexTwo());
            }
        }
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
     * Applies each edge rating system to a copy of the item pair list.
     *
     * @param itemPairPermutations - the list of item pair permutations
     */
    public void applyRatingSystemsToItemPairPermutations(ArrayList<ArrayList<MCMEdge>> itemPairPermutations) {
        int idx = 0;
        RatingSystem.assignRowRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        RatingSystem.assignMaxRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        RatingSystem.assignMinRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
        RatingSystem.assignSumRatingToEdges(itemPairPermutations.get(idx++), this.instance.getStackingConstraints());
    }

    /**
     * Returns a list of permutations of the item pairs.
     * The permutations are generated using different rating systems as basis for the sorting procedure.
     *
     * @param itemMatching - matching containing the item pairs
     * @return list of item pair permutations
     */
    public ArrayList<ArrayList<MCMEdge>> getItemPairPermutations(EdmondsMaximumCardinalityMatching itemMatching) {

        ArrayList<MCMEdge> itemPairs = GraphUtil.parseItemPairFromMCM(itemMatching);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        for (int i = 0; i < NUMER_OF_USED_EDGE_RATING_SYSTEMS; i++) {
            ArrayList<MCMEdge> tmpItemPairs = GraphUtil.getCopyOfEdgeList(itemPairs);
            itemPairPermutations.add(tmpItemPairs);
        }
        this.applyRatingSystemsToItemPairPermutations(itemPairPermutations);
        this.sortItemPairPermutationsBasedOnRatings(itemPairPermutations);

        // TODO: experiment
        this.addItemPairPermutations(itemPairs, itemPairPermutations);

        return itemPairPermutations;
    }

    /**
     * Updates the best solution if a better one is found.
     *
     * @param sol - the solution to be checked
     */
    public void updateBestSolution(Solution sol) {
        if (sol.isFeasible() && sol.getCost() < this.bestSolution.getCost()) {
            this.bestSolution = new Solution(sol);
        }
    }

    /**
     * Generates a solution with the current stack assignments.
     *
     * @param optimizeSolution - determines whether or not the solution is optimized
     * @return whether or not the generated solution should be returned
     */
    public boolean generateSolution(boolean optimizeSolution) {
        Solution sol = new Solution(0, this.timeLimit, this.instance);
//        sol.printStorageArea();
//        System.out.println("assigned: " + sol.getNumberOfAssignedItems());
        this.updateBestSolution(sol);
        if (!optimizeSolution && sol.isFeasible()) { return true; }
        this.instance.resetStacks();
        return false;
    }

    /**
     * Generates the solution to the given instance of the SP.
     *
     * @param itemMatching - matching containing the item pairs
     * @param optimizeSolution - indicates whether the solution should be optimized (otherwise the first feasible sol gets returned)
     * @return the generated solution
     */
    public Solution permutationApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching, boolean optimizeSolution) {

        this.bestSolution = new Solution();

        for (ArrayList<MCMEdge> itemPairPermutation : this.getItemPairPermutations(itemMatching)) {

            System.out.println(itemPairPermutation);

            if ((System.currentTimeMillis() - startTime) / 1000.0 >= this.timeLimit) { break; }
            for (List<Integer> unmatchedItems : this.getUnmatchedItemPermutations(itemPairPermutation)) {

                System.out.println("itemPairs: " + itemPairPermutation);
                System.out.println("unmatched: " + unmatchedItems);

                if (!this.fillStorageArea(itemPairPermutation, unmatchedItems)) {
                    this.instance.resetStacks();
                    continue;
                }

                if (this.generateSolution(optimizeSolution)) { return this.bestSolution; }
                if (!this.generateSolWithFlippedItemPair(itemPairPermutation, unmatchedItems)) { continue; }
                if (this.generateSolution(optimizeSolution)) { return this.bestSolution; }
            }
        }

        return this.bestSolution;
    }

    /**
     * Generates a stack assignment with a flipped version of the matched items.
     *
     * @param matchedItems - the item pairs
     * @param unmatchedItems - the items that are not paired
     * @return whether or not the assignment was successful
     */
    public boolean generateSolWithFlippedItemPair(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {
        ArrayList<MCMEdge> copyMatchedItems = new ArrayList<>();

        for (MCMEdge e : matchedItems) {
            copyMatchedItems.add(new MCMEdge(e));
        }
        for (MCMEdge e : copyMatchedItems) {
            e.flipVertices();
        }

        if (!this.fillStorageArea(copyMatchedItems, unmatchedItems)) {
            this.instance.resetStacks();
            return false;
        }
        return true;
    }

    /**
     * Solves the SP with an approach that uses maximum cardinality matchings and several permutations of items that
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

            DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = GraphUtil.generateStackingConstraintGraph(
                this.instance.getItems(),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                Integer.MAX_VALUE / this.instance.getItems().length,
                this.instance.getStacks()
            );
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(stackingConstraintGraph);

            sol = permutationApproach(itemMatching, optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return sol;
    }
}
