package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.util.MapUtil;
import SLP.Solution;
import com.google.common.collect.Collections2;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class InitialHeuristicSLPSolver {

    private Instance instance;
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;
    private ArrayList<List<Integer>> alreadyUsedShuffles;
    private ArrayList<ArrayList<Integer>> stackAssignments;
    private int previousNumberOfRemainingItems;

    private double startTime;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        this.alreadyUsedShuffles = new ArrayList<>();
        this.stackAssignments = new ArrayList<>();
        this.previousNumberOfRemainingItems = this.instance.getItems().length;
    }

    public void parseItemPairMCM(ArrayList<MCMEdge> matchedItems, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());

            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
    }

    public void parseItemTripleMCM(ArrayList<ArrayList<Integer>> stackAssignmentOne, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching().getEdges()) {
            String parsedEdge = edge.toString().replace("(edge(", "").replace(") : v", ", ").replace(")", "").trim();
            int first = Integer.parseInt(parsedEdge.split(",")[0].trim());
            int second = Integer.parseInt(parsedEdge.split(",")[1].trim());
            int third = Integer.parseInt(parsedEdge.split(",")[2].trim());

            ArrayList<Integer> currAssignment = new ArrayList<>();
            currAssignment.add(first);
            currAssignment.add(second);
            currAssignment.add(third);
            stackAssignmentOne.add(new ArrayList<>(currAssignment));
        }
    }

    public void assignRowRatingToEdges(ArrayList<MCMEdge> matchedItems) {
        for (MCMEdge edge : matchedItems) {
            int rating = 0;

            for (int entry : this.instance.getStackingConstraints()[edge.getVertexOne()]) {
                rating += entry;
            }
            for (int entry : this.instance.getStackingConstraints()[edge.getVertexTwo()]) {
                rating += entry;
            }
            edge.setRating(rating);
        }
    }

    public void assignColRatingToEdges(ArrayList<MCMEdge> matchedItems) {
        for (MCMEdge edge : matchedItems) {
            int rating = 0;

            // The rating is determined by the number of rows that have a one at position vertexOne or vertexTwo.
            // A high rating means that many items can be placed on top of the initial assignment.
            for (int i = 0; i < this.instance.getStackingConstraints().length; i++) {
                rating += (this.instance.getStackingConstraints()[i][edge.getVertexOne()] + this.instance.getStackingConstraints()[i][edge.getVertexTwo()]);
            }
            edge.setRating(rating);
        }
    }

    public ArrayList<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        ArrayList<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }

    // TODO: exchange certain elements of this sequence with other (unused) ones
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
            Random r = new Random();
            int low = 0;
            int high = this.instance.getStacks().length - 1;
            int result = r.nextInt(high - low) + low;
            toBeReplaced.add(result);
        }

        for (int i = 0; i < toBeReplaced.size(); i++) {
            Collections.swap(tmpEdges, toBeReplaced.get(i), i + this.instance.getStacks().length);
        }

        return new ArrayList(tmpEdges);
    }

    public ArrayList<ArrayList<MCMEdge>> getInitialStackAssignmentsFromMCM(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<MCMEdge> edges = new ArrayList<>();
        this.parseItemPairMCM(edges, mcm);

        ArrayList<MCMEdge> edgesCopy = new ArrayList<>();
        for (MCMEdge e : edges) {
            edgesCopy.add(new MCMEdge(e));
        }

        this.assignRowRatingToEdges(edges);
        this.assignColRatingToEdges(edgesCopy);

        // The edges are sorted based on their ratings.
        Collections.sort(edges);
        Collections.sort(edgesCopy);

        ArrayList<ArrayList<MCMEdge>> edgePermutations = new ArrayList<>();
        // The first permutation that is added is the one based on the sorting
        // which should be the most promising stack assignment.
        edgePermutations.add(new ArrayList(edges));
        edgePermutations.add(new ArrayList(edgesCopy));

        edgePermutations.add(this.getReversedCopyOfEdgeList(edges));
        edgePermutations.add(this.getReversedCopyOfEdgeList(edgesCopy));

        // TODO: Remove hard coded values
        for (int cnt = 0; cnt < 5000; cnt++) {
            ArrayList<MCMEdge> tmp = new ArrayList(this.edgeExchange(edges));
            if (!edgePermutations.contains(tmp)) {
                edgePermutations.add(tmp);
            }
        }
        for (int cnt = 0; cnt < 5000; cnt++) {
            ArrayList<MCMEdge> tmp = new ArrayList(this.edgeExchange(edgesCopy));
            if (!edgePermutations.contains(tmp)) {
                edgePermutations.add(tmp);
            }
        }

        if (edges.size() < 9) {
            for (List<MCMEdge> edgeList : Collections2.permutations(edges)) {
                edgePermutations.add(new ArrayList(edgeList));
            }
        } else {
            for (int i = 0; i < 40000; i++) {
                Collections.shuffle(edges);
//                if (!edgePermutations.contains(edges)) {
                    edgePermutations.add(new ArrayList(edges));
//                }
            }
        }

        return edgePermutations;
    }

    public ArrayList<Integer> getInitiallyUnmatchedItems(ArrayList<MCMEdge> edges) {

        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (MCMEdge edge : edges) {
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

    public void copyStackAssignment(int[][] init, int[][] copy) {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[0].length; j++) {
                copy[i][j] = init[i][j];
            }
        }
    }

    public void assignUnmatchedItemsInGivenOrder(List<Integer> unmatchedItems) {

        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                int levelOfCurrentTopMostItem = -99;
                for (int level = 2; level >= 0; level--) {
                    if (this.instance.getStacks()[stack][level] != -1) {
                        levelOfCurrentTopMostItem = level;
                    }
                }

                if (levelOfCurrentTopMostItem == -99) {
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

    public boolean isPartOfAlreadyUsedShuffles(ArrayList<Integer> currentShuffle) {
        for (List<Integer> shuffle : this.alreadyUsedShuffles) {
            if (shuffle.equals(currentShuffle)) {
                return true;
            }
        }
        return false;
    }

    public int computeRowRatingForUnmatchedItem(int item) {
        int rating = 0;
        for (int entry : this.instance.getStackingConstraints()[item]) {
            rating += entry;
        }
        return rating;
    }

    public int computeColRatingForUnmatchedItem(int item) {
        int rating = 0;
        for (int i = 0; i < this.instance.getStackingConstraints().length; i++) {
            rating += this.instance.getStackingConstraints()[i][item];
        }
        return rating;
    }

    public ArrayList<List<Integer>> getUnmatchedPermutations(ArrayList<MCMEdge> matchedItems) {

        ArrayList<Integer> initiallyUnmatchedItems = new ArrayList<>(this.getInitiallyUnmatchedItems(matchedItems));
        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        HashMap<Integer, Integer> unmatchedItemRatings = new HashMap<>();
        for (int item : initiallyUnmatchedItems) {
            unmatchedItemRatings.put(item, this.computeRowRatingForUnmatchedItem(item));
        }

        // ordered by rating - hardest cases first
        Map<Integer, Integer> sortedItemRatings = MapUtil.sortByValue(unmatchedItemRatings);
        ArrayList<Integer> unmatchedItemsSortedByRating = new ArrayList<>();

        for (int item : sortedItemRatings.keySet()) {
            unmatchedItemsSortedByRating.add(item);
        }

        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRating));
        Collections.reverse(unmatchedItemsSortedByRating);
        unmatchedItemPermutations.add(new ArrayList<>(unmatchedItemsSortedByRating));

        ///////////////////////// TODO: Complete idea
//        // restore
//        Collections.reverse(unmatchedItemsSortedByRating);
//        for (int i = 0; i < 10000; i++) {
//            Random r = new Random();
//            int low = 0;
//            int high = unmatchedItemsSortedByRating.size() - 1;
//            int res1 = r.nextInt(high - low) + low;
//            int res2 = r.nextInt(high - low) + low;
//            ArrayList<Integer> tmp = new ArrayList<>(unmatchedItemsSortedByRating);
//            Collections.swap(tmp, res1, res2);
//            unmatchedItemPermutations.add(new ArrayList<>(tmp));
//        }
        //////////////////////////////////

        // For up to 8 items, the computation of permutations is possible in a reasonable time frame,
        // after that 40k random shuffles are used instead.
//        if (initiallyUnmatchedItems.size() < 9) {
//            for (List<Integer> itemList : Collections2.permutations(initiallyUnmatchedItems)) {
//                unmatchedItemPermutations.add(new ArrayList<>(itemList));
//            }
//        } else {
            for (int i = 0; i < 500; i++) {
                unmatchedItemPermutations.add(new ArrayList<>(initiallyUnmatchedItems));
                Collections.shuffle(initiallyUnmatchedItems);

                int unsuccessfulShuffleAttempts = 0;
                while (isPartOfAlreadyUsedShuffles(initiallyUnmatchedItems)) {
                    System.out.println("already");
                    Collections.shuffle(initiallyUnmatchedItems);
                    if (unsuccessfulShuffleAttempts == 10) {
                        return unmatchedItemPermutations;
                    }
                    unsuccessfulShuffleAttempts++;
                }
                this.alreadyUsedShuffles.add(new ArrayList<>(initiallyUnmatchedItems));
            }
//        }

        return unmatchedItemPermutations;
    }

    public boolean assignItemToFirstPossiblePosition(int item) {

        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 2; j > 0; j--) {

                // TODO: generalize steps

                // GROUND LEVEL CASE
                if (j == 2 && this.instance.getStacks()[i][j] == -1) {
                    this.instance.getStacks()[i][j] = item;
                    return true;
                } else if (j == 2 && this.instance.getStacks()[i][j] != -1) {
                    if (this.instance.getStacks()[i][j - 1] == -1 && this.instance.getStackingConstraints()[item][this.instance.getStacks()[i][j]] == 1) {
                        this.instance.getStacks()[i][j - 1] = item;
                        return true;
                    }
                } else if (j == 1 && this.instance.getStacks()[i][j] == -1) {
                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[i][j + 1]] == 1) {
                        this.instance.getStacks()[i][j] = item;
                        return true;
                    }
                } else if (j == 1 && this.instance.getStacks()[i][j] != -1) {
                    if (this.instance.getStacks()[i][j - 1] == -1 && this.instance.getStackingConstraints()[item][this.instance.getStacks()[i][j]] == 1) {
                        this.instance.getStacks()[i][j - 1] = item;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean assignItemsInGivenOrder(int idx, int below, int above) {
        // assign to 1st and 2nd level
        if (this.instance.getStacks()[idx][2] == -1) {
            this.instance.getStacks()[idx][2] = below;
            this.instance.getStacks()[idx][1] = above;
            return true;
        // assign to 2nd and 3rd level
        } else if (this.instance.getStacks()[idx][1] == -1
                && this.instance.getStackingConstraints()[below][this.instance.getStacks()[idx][2]] == 1) {
            this.instance.getStacks()[idx][1] = below;
            this.instance.getStacks()[idx][0] = above;
            return true;
        }
        // no two free positions
        return false;
    }

    public boolean assignEdgeToFirstPossiblePosition(MCMEdge edge) {

        for (int i = 0; i < this.instance.getStacks().length; i++) {
            int vertexOne = edge.getVertexOne();
            int vertexTwo = edge.getVertexTwo();

            if (this.instance.getStackingConstraints()[vertexTwo][vertexOne] == 1) {
                if (this.assignItemsInGivenOrder(i, vertexOne, vertexTwo)) {
                    return true;
                }
            } else {
                if (this.assignItemsInGivenOrder(i, vertexTwo, vertexOne)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean listContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        if(set.size() < items.size()){
            return true;
        }
        return false;
    }

    public void prioritizeInflexibleEdges(ArrayList<MCMEdge> matchedItems, ArrayList<MCMEdge> prioritizedEdges) {

        int cnt = 0;

        for (MCMEdge edge : matchedItems) {
            int vertexOne = edge.getVertexOne();
            int vertexTwo = edge.getVertexTwo();

            if (this.computeRowRatingForUnmatchedItem(vertexOne) <= 10 || this.computeRowRatingForUnmatchedItem(vertexTwo) <= 10) {

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

    public boolean prioritizeInflexibleItem(List<Integer> unmatchedItems) {
        for (int item : unmatchedItems) {
            if (this.computeRowRatingForUnmatchedItem(item) <= 10) {
                this.unstackableItems.add(item);
                if (!this.assignItemToFirstPossiblePosition(item)) {
                    return false;
                }
            }
        }
        return true;
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

            if (!this.assignEdgeToFirstPossiblePosition(edge)) {
                this.additionalUnmatchedItems.add(edge.getVertexOne());
                this.additionalUnmatchedItems.add(edge.getVertexTwo());
            }
        }
    }

    public boolean getUnmatchedItems(ArrayList<Integer> unmatchedItems) {

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

        // TODO: Check - could cause problems
        for (int item : unmatchedItems) {
            // If we still have a one here, we have a problem.
            if (this.computeRowRatingForUnmatchedItem(item) == 1) {
                return false;
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
        if (!this.getUnmatchedItems(stillUnmatchedItems)) { return false; }
        this.assignUnmatchedItemsInGivenOrder(stillUnmatchedItems);
        return true;
    }

    public void generateStackingConstraintGraph(DefaultUndirectedGraph<String, DefaultEdge> graph) {
        for (int item : this.instance.getItems()) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < this.instance.getStackingConstraints().length; i++) {
            for (int j = 0; j < this.instance.getStackingConstraints()[0].length; j++) {
                if (i != j && this.instance.getStackingConstraints()[i][j] == 1 || this.instance.getStackingConstraints()[j][i] == 1) {
                    if (!graph.containsEdge("v" + j, "v" + i)) {
                        graph.addEdge("v" + i, "v" + j);
                    }
                }
            }
        }
    }

    public EdmondsMaximumCardinalityMatching<String, DefaultEdge> getMCMForUnassignedItems(ArrayList<Integer> unassignedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int item : unassignedItems) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < unassignedItems.size(); i++) {
            for (int j = 0; j < unassignedItems.size(); j++) {
                if (unassignedItems.get(i) != unassignedItems.get(j) && this.instance.getStackingConstraints()[unassignedItems.get(i)][unassignedItems.get(j)] == 1
                        || this.instance.getStackingConstraints()[unassignedItems.get(j)][unassignedItems.get(i)] == 1) {

                            if (!graph.containsEdge("v" + unassignedItems.get(j), "v" + unassignedItems.get(i))) {
                                graph.addEdge("v" + unassignedItems.get(i), "v" + unassignedItems.get(j));
                            }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        return mcm;
    }

    public void generateBipartiteGraph(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            ArrayList<MCMEdge> edges,
            ArrayList<Integer> unmatchedItems,
            int numberOfUsedItemPairs
    ) {

        // adding the specified number of item pairs as nodes to the graph
        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            graph.addVertex("edge" + edges.get(i));
        }

        // adding all unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < numberOfUsedItemPairs; i++) {
            for (int j = 0; j < unmatchedItems.size(); j++) {

                // if it is possible to complete the stack assignment with the unmatched item, it is done

                if (this.instance.getStackingConstraints()[edges.get(i).getVertexOne()][edges.get(i).getVertexTwo()] == 1) {
                    if (this.instance.getStackingConstraints()[edges.get(i).getVertexTwo()][unmatchedItems.get(j)] == 1
                            || this.instance.getStackingConstraints()[unmatchedItems.get(j)][edges.get(i).getVertexOne()] == 1) {

                                if (!graph.containsEdge("v" + unmatchedItems.get(j), "edge" + edges.get(i))) {
                                    graph.addEdge("edge" + edges.get(i), "v" + unmatchedItems.get(j));
                                }
                    }
                } else {
                    if (this.instance.getStackingConstraints()[edges.get(i).getVertexOne()][unmatchedItems.get(j)] == 1
                            || this.instance.getStackingConstraints()[unmatchedItems.get(j)][edges.get(i).getVertexTwo()] == 1) {

                                if (!graph.containsEdge("v" + unmatchedItems.get(j), "edge" + edges.get(i))) {
                                    graph.addEdge("edge" + edges.get(i), "v" + unmatchedItems.get(j));
                                }
                    }
                }
            }
        }
    }

    public ArrayList<Integer> getCurrentListOfUnmatchedItems(int length, ArrayList<MCMEdge> edges, ArrayList<Integer> unassignedItems) {
        ArrayList<Integer> MCMItems = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            MCMItems.add(edges.get(i).getVertexOne());
            MCMItems.add(edges.get(i).getVertexTwo());
        }
        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : unassignedItems) {
            if (!MCMItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    public ArrayList<Integer> getUnassignedItemsFromStorageAreaSnapshot(ArrayList<ArrayList<Integer>> storageArea) {
        ArrayList<Integer> alreadyAssignedItems = new ArrayList<>();
        for (ArrayList<Integer> stack : storageArea) {
            for (int item : stack) {
                alreadyAssignedItems.add(item);
            }
        }
        for (ArrayList<Integer> stack : this.stackAssignments) {
            for (int item : stack) {
                alreadyAssignedItems.add(item);
            }
        }
        ArrayList<Integer> unassignedItems = new ArrayList<>();
        for (int i : this.instance.getItems()) {
            if (!alreadyAssignedItems.contains(i)) {
                unassignedItems.add(i);
            }
        }
        return unassignedItems;
    }

    public void recursiveMCMApproach(EdmondsMaximumCardinalityMatching mcm, int numberOfEdgesToBeUsed, ArrayList<Integer> remainingItems) {

        System.out.println("remaining items: " + remainingItems.size());

        if (remainingItems.size() == 0 || this.previousNumberOfRemainingItems == remainingItems.size()) {
            if (this.previousNumberOfRemainingItems != this.instance.getItems().length) { return; }
        }
        this.previousNumberOfRemainingItems = remainingItems.size();
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        this.parseItemPairMCM(itemPairs, mcm);
        this.assignColRatingToEdges(itemPairs);
        Collections.sort(itemPairs);

        // COMPUTING COMPATIBLE ITEM TRIPLES FROM ITEM PAIRS AND REMAINING ITEMS
        ArrayList<Integer> unmatchedItems = this.getCurrentListOfUnmatchedItems(numberOfEdgesToBeUsed, itemPairs, remainingItems);
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateBipartiteGraph(graph, itemPairs, unmatchedItems, numberOfEdgesToBeUsed);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<ArrayList<Integer>> currentStackAssignment = new ArrayList<>();
        this.parseItemTripleMCM(currentStackAssignment, itemTriples);
        this.stackAssignments.addAll(currentStackAssignment);

        // COMPUTING COMPATIBLE ITEM PAIRS FROM REMAINING ITEMS
        unmatchedItems = this.getUnassignedItemsFromStorageAreaSnapshot(currentStackAssignment);
        EdmondsMaximumCardinalityMatching remainingItemPairs = this.getMCMForUnassignedItems(unmatchedItems);
        itemPairs = new ArrayList<>();
        this.parseItemPairMCM(itemPairs, remainingItemPairs);
        numberOfEdgesToBeUsed = (int)Math.ceil(itemPairs.size() / 3);

        this.recursiveMCMApproach(remainingItemPairs, numberOfEdgesToBeUsed, unmatchedItems);
    }

    public void generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            ArrayList<MCMEdge> edges,
            int numberOfUsedEdges,
            ArrayList<Integer> remainingItems,
            ArrayList<ArrayList<MCMEdge>> itemPairPermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems
    ) {

        for (int i = 0; i < 10000; i++) {
            ArrayList<Integer> tmpRemainingItems = new ArrayList<>(remainingItems);
            ArrayList<MCMEdge> tmpItemPairs = new ArrayList<>();

            for (int j = 0; j < edges.size(); j++) {
                if (j < numberOfUsedEdges) {
                    tmpItemPairs.add(edges.get(j));
                } else {
                    tmpRemainingItems.add(edges.get(j).getVertexOne());
                    tmpRemainingItems.add(edges.get(j).getVertexTwo());
                }
            }
            // TODO: add check for already used shuffles
            Collections.shuffle(edges);
            itemPairPermutations.add(new ArrayList<>(tmpItemPairs));
            listsOfRemainingItems.add(new ArrayList<>(tmpRemainingItems));
        }
    }

    public int generateCurrentStackAssignments(
            ArrayList<MCMEdge> currentEdges,
            ArrayList<Integer> currentRemainingItems,
            ArrayList<ArrayList<Integer>> currentStackAssignments
    ) {

        int numberOfAssignments = 0;

        for (MCMEdge e : currentEdges) {
            ArrayList<Integer> currentStack = new ArrayList<>();
            currentStack.add(e.getVertexOne());
            currentStack.add(e.getVertexTwo());

            for (int item : currentRemainingItems) {
                if (this.instance.getStackingConstraints()[e.getVertexOne()][e.getVertexTwo()] == 1) {
                    if (this.instance.getStackingConstraints()[e.getVertexTwo()][item] == 1
                            || this.instance.getStackingConstraints()[item][e.getVertexOne()] == 1)
                    {
                        numberOfAssignments++;
                        currentStack.add(item);
                        currentRemainingItems.remove(currentRemainingItems.indexOf(item));
                        break;
                    }
                } else {
                    if (this.instance.getStackingConstraints()[e.getVertexOne()][item] == 1
                            || this.instance.getStackingConstraints()[item][e.getVertexTwo()] == 1)
                    {
                        numberOfAssignments++;
                        currentStack.add(item);
                        currentRemainingItems.remove(currentRemainingItems.indexOf(item));
                        break;
                    }
                }
            }
            currentStackAssignments.add(currentStack);
        }
        return numberOfAssignments;
    }

    public void findBestRemainingStackAssignments(
            ArrayList<ArrayList<Integer>> bestStackAssignments,
            ArrayList<ArrayList<MCMEdge>> edgePermutations,
            ArrayList<ArrayList<Integer>> listsOfRemainingItems,
            ArrayList<Integer> remainingItems
    ) {

        int maxNumberOfAssignments = 0;

        for (ArrayList<MCMEdge> currentEdges : edgePermutations) {
            ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
            ArrayList<Integer> currentRemainingItems = new ArrayList<>(listsOfRemainingItems.get(edgePermutations.indexOf(currentEdges)));

            int numberOfAssignments = this.generateCurrentStackAssignments(currentEdges, currentRemainingItems, currentStackAssignments);

            if (numberOfAssignments > maxNumberOfAssignments) {
                maxNumberOfAssignments = numberOfAssignments;
                bestStackAssignments.clear();
                bestStackAssignments.addAll(currentStackAssignments);
                remainingItems.clear();
                remainingItems.addAll(currentRemainingItems);
            }
        }
    }

    public void updateRemainingItems(ArrayList<MCMEdge> edges, ArrayList<Integer> remainingItems) {
        for (MCMEdge e : edges) {
            if (remainingItems.contains(e.getVertexOne())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexOne()));
            }
            if (remainingItems.contains(e.getVertexTwo())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexTwo()));
            }
        }
    }

    public void assignUnstackableItemsToOwnStack(ArrayList<Integer> remainingItems) {
        for (int item : remainingItems) {
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(item);
            this.stackAssignments.add(stack);
        }
    }

    public void fillStorageAreaWithGeneratedStackAssignments() {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
                if (i < this.stackAssignments.size()) {
                    ArrayList<Integer> stack = this.stackAssignments.get(i);
                    if (j < stack.size()) {
                        this.instance.getStacks()[i][j] = stack.get(j);
                    }
                }
            }
        }
    }

    public void checkItemAssignments() {
        ArrayList<Integer> remainingItems = this.getUnassignedItemsFromStorageAreaSnapshot(this.stackAssignments);
        if (remainingItems.size() > 0) {
            System.out.println("Problem: Not all items have been assigned, even though the process is complete.");
        }
    }

    public void completeStackAssignmentsForRecursiveApproach() {
        ArrayList<Integer> remainingItems = this.getUnassignedItemsFromStorageAreaSnapshot(this.stackAssignments);
        EdmondsMaximumCardinalityMatching mcm = this.getMCMForUnassignedItems(remainingItems);
        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        this.parseItemPairMCM(itemPairs, mcm);
        this.updateRemainingItems(itemPairs, remainingItems);

        this.assignColRatingToEdges(itemPairs);
        Collections.sort(itemPairs);
        // TODO: search for reasonable way to compute the number of used item pairs
        int numberOfUsedItemPairs = itemPairs.size() - (int)Math.ceil(itemPairs.size() / 2) + 10;

        ArrayList<ArrayList<MCMEdge>> itemPairPermutations = new ArrayList<>();
        ArrayList<ArrayList<Integer>> listsOfRemainingItems = new ArrayList<>();
        this.generateItemPairPermutationsAndCorrespondingListsOfRemainingItems(
            itemPairs, numberOfUsedItemPairs, remainingItems, itemPairPermutations, listsOfRemainingItems
        );

        ArrayList<ArrayList<Integer>> bestStackAssignments = new ArrayList<>();
        this.findBestRemainingStackAssignments(bestStackAssignments, itemPairPermutations, listsOfRemainingItems, remainingItems);
        this.stackAssignments.addAll(bestStackAssignments);
        this.assignUnstackableItemsToOwnStack(remainingItems);

        System.out.println("stacks used: " + this.stackAssignments.size());
        this.checkItemAssignments();
        this.fillStorageAreaWithGeneratedStackAssignments();
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

    public Solution permutationApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm, boolean optimizeSolution) {

        ArrayList<ArrayList<MCMEdge>> itemPairSubsets = this.getInitialStackAssignmentsFromMCM(mcm);
        Solution bestSol = new Solution();
        int generatedSolutions = 0;

        for (ArrayList<MCMEdge> matchedItems : itemPairSubsets) {
            // time limit of 5 minutes
            if ((System.currentTimeMillis() - startTime) / 1000.0 >= 300) { break; }
            // limits the number of generated solutions to ~1 mio.
            if (generatedSolutions > 1000000) { break; }

            for (List<Integer> unmatchedItems : this.getUnmatchedPermutations(matchedItems)) {
                if (!this.setStacks(matchedItems, unmatchedItems)) {
                    this.instance.resetStacks();
                    break;
                }
                Solution sol = new Solution(0, false, this.instance);
                if (!optimizeSolution && sol.isFeasible()) { return sol; }
                if (sol.isFeasible() && sol.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol);
                }
                this.instance.resetStacks();

                if (!this.generateSolWithFlippedItemPair(matchedItems, unmatchedItems)) { break; }
                Solution flippedPairsSol = new Solution(0, false, this.instance);

                if (!optimizeSolution && flippedPairsSol.isFeasible()) {
                    return flippedPairsSol;
                }

                if (flippedPairsSol.isFeasible() && flippedPairsSol.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(flippedPairsSol);
                }

                generatedSolutions += 2;
                this.instance.resetStacks();
            }
        }

        System.out.println("number of generated solutions: " + generatedSolutions);
        return bestSol;
    }

    public Solution capThreeApproach(boolean optimizeSolution) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateStackingConstraintGraph(graph);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<Integer> items = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            items.add(item);
        }
        this.recursiveMCMApproach(mcm, this.instance.getStacks().length, items);
        this.completeStackAssignmentsForRecursiveApproach();

        Solution sol = new Solution(0, false, this.instance);
        sol.transformStackAssignmentIntoValidSolutionIfPossible();
//        System.out.println("sol feasible: " + sol.isFeasible());
//        System.out.println(sol.getNumberOfAssignedItems());

//        return sol;
        return permutationApproach(mcm, optimizeSolution);
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
            sol = this.capThreeApproach(optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        }
        return sol;
    }
}
