package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import SLP.util.MapUtil;
import com.google.common.collect.Collections2;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class ThreeCapPermutationHeuristic {

    private final int COMPLETE_PERMUTATION_LIMIT = 8;
    private final int ITEM_PAIR_PERMUTATIONS = 40000;
    private final int ITEM_PERMUTATIONS = 500;

    private Instance instance;
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;
    private ArrayList<List<Integer>> alreadyUsedShuffles;
    private double startTime;
    private int timeLimit;

    public ThreeCapPermutationHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        this.alreadyUsedShuffles = new ArrayList<>();
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

    public void addItemPairPermutations(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<MCMEdge>> itemPairPermutations, ArrayList<MCMEdge> itemPairsCopy) {
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
            for (int cnt = 0; cnt < 5000; cnt++) {
                ArrayList<MCMEdge> tmp = new ArrayList(this.edgeExchange(itemPairsCopy));
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

    public ArrayList<Integer> getUnmatchedItemsSortedByRowRating(ArrayList<Integer> initialListOfUnmatchedItems) {
        HashMap<Integer, Integer> unmatchedItemRowRatings = new HashMap<>();
        for (int item : initialListOfUnmatchedItems) {
            unmatchedItemRowRatings.put(item, HeuristicUtil.computeRowRatingForUnmatchedItem(item, this.instance.getStackingConstraints()));
        }
        ArrayList<Integer> unmatchedItemsSortedByRowRating = new ArrayList<>();
        Map<Integer, Integer> sortedItemRowRatings = MapUtil.sortByValue(unmatchedItemRowRatings);
        for (int item : sortedItemRowRatings.keySet()) {
            unmatchedItemsSortedByRowRating.add(item);
        }
        return unmatchedItemsSortedByRowRating;
    }

    public ArrayList<Integer> getUnmatchedItemsSortedByColRating(ArrayList<Integer> initialListOfUnmatchedItems) {
        HashMap<Integer, Integer> unmatchedItemColRatings = new HashMap<>();
        for (int item : initialListOfUnmatchedItems) {
            unmatchedItemColRatings.put(item, HeuristicUtil.computeColRatingForUnmatchedItem(item, this.instance.getStackingConstraints()));
        }
        Map<Integer, Integer> sortedItemColRatings = MapUtil.sortByValue(unmatchedItemColRatings);
        ArrayList<Integer> unmatchedItemsSortedByColRating = new ArrayList<>();
        for (int item : sortedItemColRatings.keySet()) {
            unmatchedItemsSortedByColRating.add(item);
        }
        return unmatchedItemsSortedByColRating;
    }

    public ArrayList<List<Integer>> getUnmatchedItemPermutations(ArrayList<MCMEdge> matchedItems) {

        // TODO: question to ask: is it going to be placed above or below the pair?
        //                        --> compute rating accordingly
        //                        --> dynamic decision for each stack completion

        ArrayList<Integer> initialListOfUnmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItems(matchedItems, this.instance.getItems()));
        ArrayList<Integer> unmatchedItemsSortedByRowRating = this.getUnmatchedItemsSortedByRowRating(initialListOfUnmatchedItems);
        ArrayList<Integer> unmatchedItemsSortedByColRating = this.getUnmatchedItemsSortedByColRating(initialListOfUnmatchedItems);

        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRowRating));
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByColRating));
        Collections.reverse(unmatchedItemsSortedByRowRating);
        Collections.reverse(unmatchedItemsSortedByColRating);
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRowRating));
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByColRating));

        return unmatchedItemPermutations;
    }

    public void prioritizeInflexibleEdges(ArrayList<MCMEdge> matchedItems, ArrayList<MCMEdge> prioritizedEdges) {

        int cnt = 0;

        for (MCMEdge edge : matchedItems) {

            if (cnt >= this.instance.getStacks().length) { break; }

            int vertexOne = edge.getVertexOne();
            int vertexTwo = edge.getVertexTwo();

            if (HeuristicUtil.computeRowRatingForUnmatchedItem(vertexOne, this.instance.getStackingConstraints()) <= this.instance.getItems().length / 50
                || HeuristicUtil.computeRowRatingForUnmatchedItem(vertexTwo, this.instance.getStackingConstraints()) <= this.instance.getItems().length / 50) {

                while (cnt < this.instance.getStacks().length
                    && (this.instance.getStackConstraints()[vertexOne][cnt] != 1 || this.instance.getStackConstraints()[vertexTwo][cnt] != 1)) {
                        cnt++;
                }
                if (cnt >= this.instance.getStacks().length) { break; }

                prioritizedEdges.add(new MCMEdge(vertexOne, vertexTwo, 0));

                if (this.instance.getStackingConstraints()[vertexTwo][vertexOne] == 1) {
                    this.instance.getStacks()[cnt][2] = vertexOne;
                    this.instance.getStacks()[cnt][1] = vertexTwo;
                } else {
                    this.instance.getStacks()[cnt][2] = vertexTwo;
                    this.instance.getStacks()[cnt][1] = vertexOne;
                }
                cnt++;
            }
        }
    }

    public boolean assignItemToFirstPossiblePosition(int item) {

        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

            // item and stack are incompatible
            if (this.instance.getStackConstraints()[item][stack] != 1) {
                continue;
            }

            for (int level = 2; level > 0; level--) {

                // TODO: generalize steps

                // GROUND LEVEL CASE
                if (level == 2 && this.instance.getStacks()[stack][level] == -1) {
                    this.instance.getStacks()[stack][level] = item;
                    return true;
                } else if (level == 2 && this.instance.getStacks()[stack][level] != -1) {
                    if (this.instance.getStacks()[stack][level - 1] == -1 && this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][level]] == 1) {
                        this.instance.getStacks()[stack][level - 1] = item;
                        return true;
                    }
                } else if (level == 1 && this.instance.getStacks()[stack][level] == -1) {
                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][level + 1]] == 1) {
                        this.instance.getStacks()[stack][level] = item;
                        return true;
                    }
                } else if (level == 1 && this.instance.getStacks()[stack][level] != -1) {
                    if (this.instance.getStacks()[stack][level - 1] == -1 && this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][level]] == 1) {
                        this.instance.getStacks()[stack][level - 1] = item;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean prioritizeInflexibleItem(List<Integer> unmatchedItems) {
        for (int item : unmatchedItems) {
            if (HeuristicUtil.computeRowRatingForUnmatchedItem(item, this.instance.getStackingConstraints()) <= 10) {
                this.unstackableItems.add(item);
                if (!this.assignItemToFirstPossiblePosition(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean setStacks(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {

        if (matchedItems.size() * 2 + unmatchedItems.size() != this.instance.getItems().length) {
            System.out.println("PROBLEM: number of matched items + number of unmatched items != number of items");
            System.exit(0);
        }

        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        ArrayList<MCMEdge> prioritizedEdges = new ArrayList<>();

        this.prioritizeInflexibleEdges(matchedItems, prioritizedEdges);
        if (!this.prioritizeInflexibleItem(unmatchedItems)) { return false; }
        this.processMatchedItems(matchedItems, prioritizedEdges);

        ArrayList<Integer> stillUnmatchedItems = new ArrayList<>(unmatchedItems);
        this.updateUnmatchedItems(stillUnmatchedItems);
        this.assignUnmatchedItemsInGivenOrder(stillUnmatchedItems);
        return true;
    }

    public void tryToAssignRemainingItemsAsPairs(List<Integer> unmatchedItems) {

        EdmondsMaximumCardinalityMatching mcm = HeuristicUtil.getMCMForUnassignedItems((ArrayList<Integer>) unmatchedItems, this.instance.getStackingConstraints());
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);

        ArrayList<Integer> toBeRemoved = new ArrayList<>();

        for (MCMEdge itemPair : itemPairs) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
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
     * @param unmatchedItems
     */
    public void assignUnmatchedItemsInGivenOrder(List<Integer> unmatchedItems) {

        this.tryToAssignRemainingItemsAsPairs(unmatchedItems);

        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                // item and stack incompatible
                if (this.instance.getStackConstraints()[item][stack] != 1) { continue; }

                int levelOfCurrentTopMostItem = -1;
                for (int level = 2; level >= 0; level--) {
                    if (this.instance.getStacks()[stack][level] != -1) {
                        levelOfCurrentTopMostItem = level;
                    }
                }

                if (levelOfCurrentTopMostItem == -1) {
                    // assign to ground level
                    this.instance.getStacks()[stack][2] = item;
                    break;
                } else {
                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[stack][levelOfCurrentTopMostItem]] == 1) {
                        if (levelOfCurrentTopMostItem > 0) {
                            if (this.instance.getStacks()[stack][levelOfCurrentTopMostItem - 1] == -1) {
                                this.instance.getStacks()[stack][levelOfCurrentTopMostItem - 1] = item;
                                break;
                            }
                        }
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
    public boolean assignItemPairToStack(int stackIdx, int below, int above) {

        // at least one of the items is not compatible with the stack
        if (this.instance.getStackConstraints()[below][stackIdx] != 1 || this.instance.getStackConstraints()[above][stackIdx] != 1) {
            return false;
        }

        // assign to 1st and 2nd level
        if (this.instance.getStacks()[stackIdx][2] == -1 && this.instance.getStacks()[stackIdx][1] == -1) {
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
     * Handles the assignment of the items connected by the given edge.
     *
     * @param edge - the edge connecting the item pair
     * @return whether the assignment was successful
     */
    public boolean handleItemPairAssignmentForEdge(MCMEdge edge) {

        for (int i = 0; i < this.instance.getStacks().length; i++) {

            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();

            if (this.instance.getStackingConstraints()[itemTwo][itemOne] == 1) {
                if (this.assignItemPairToStack(i, itemOne, itemTwo)) { return true; }
            } else {
                if (this.assignItemPairToStack(i, itemTwo, itemOne)) { return true; }
            }
        }
        return false;
    }

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

    public ArrayList<ArrayList<MCMEdge>> getItemPairPermutations(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        ArrayList<MCMEdge> itemPairsCopy = new ArrayList<>();
        for (MCMEdge e : itemPairs) {
            itemPairsCopy.add(new MCMEdge(e));
        }

        HeuristicUtil.assignRowRatingToEdges(itemPairs, this.instance.getStackingConstraints());
        HeuristicUtil.assignColRatingToEdges(itemPairsCopy, this.instance.getStackingConstraints());

        // The edges get sorted based on their ratings.
        Collections.sort(itemPairs);
        Collections.sort(itemPairsCopy);

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        // The first permutation that is added is the one based on the sorting
        // which should be the most promising stack assignment.
        itemPairPermutations.add(new ArrayList(itemPairs));
        itemPairPermutations.add(new ArrayList(itemPairsCopy));

        // TODO: The reversed sequence shouldn't be reasonable.
        itemPairPermutations.add(HeuristicUtil.getReversedCopyOfEdgeList(itemPairs));
        itemPairPermutations.add(HeuristicUtil.getReversedCopyOfEdgeList(itemPairsCopy));

        return itemPairPermutations;
    }

    public Solution permutationApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm, boolean optimizeSolution) {

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = this.getItemPairPermutations(mcm);
        Solution bestSol = new Solution();

        for (ArrayList<MCMEdge> itemPairPermutation : itemPairPermutations) {

            // TODO: change back to actual time limit
            // if ((System.currentTimeMillis() - startTime) / 1000.0 >= this.timeLimit) { break; }
            if ((System.currentTimeMillis() - startTime) / 1000.0 >= 20) { break; }

            for (List<Integer> unmatchedItems : this.getUnmatchedItemPermutations(itemPairPermutation)) {

                if (!this.setStacks(itemPairPermutation, unmatchedItems)) {
                    this.instance.resetStacks();
                    break;
                }

                Solution sol = new Solution(0, this.timeLimit, this.instance);
                if (!optimizeSolution && sol.isFeasible()) { return sol; }
                if (sol.isFeasible() && sol.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol);
                }

                this.instance.resetStacks();

                if (!this.generateSolWithFlippedItemPair(itemPairPermutation, unmatchedItems)) { break; }
                Solution flippedPairsSol = new Solution(0, this.timeLimit, this.instance);

                if (!optimizeSolution && flippedPairsSol.isFeasible()) {
                    return flippedPairsSol;
                }

                if (flippedPairsSol.isFeasible() && flippedPairsSol.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(flippedPairsSol);
                }

                this.instance.resetStacks();
            }
        }

        return bestSol;
    }

    public boolean generateSolWithFlippedItemPair(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {
        ArrayList<MCMEdge> copyMatchedItems = new ArrayList<>();

        for (MCMEdge e : matchedItems) {
            copyMatchedItems.add(new MCMEdge(e));
        }
        for (MCMEdge e : copyMatchedItems) {
            e.flipVertices();
        }

        if (!this.setStacks(copyMatchedItems, unmatchedItems)) {
            this.instance.resetStacks();
            return false;
        }
        return true;
    }

    /**
     *
     * @param optimizeSolution - specifies whether the solution should be optimized or just valid
     * @return
     */
    public Solution solve(boolean optimizeSolution) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();
            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

            HeuristicUtil.generateStackingConstraintGraph(graph, this.instance.getItems(), this.instance.getStackingConstraints());
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

            sol = permutationApproach(mcm, optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 3.");
        }
        return sol;
    }
}
