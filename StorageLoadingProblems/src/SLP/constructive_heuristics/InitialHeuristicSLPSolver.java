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

    public void parseMCM(ArrayList<MCMEdge> matchedItems, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());

            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
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
        this.parseMCM(edges, mcm);

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

    public int numberOfAlreadyAssignedItems() {
        int items = 0;
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
                if (this.instance.getStacks()[i][j] != -1) {
                    items++;
                }
            }
        }
        return items;
    }

    public boolean assignEdgeToFirstPossiblePosition(MCMEdge edge) {

        int cnt = 0;

        while (cnt < this.instance.getStacks().length) {

            int vertexOne = edge.getVertexOne();
            int vertexTwo = edge.getVertexTwo();

            // ORDER ONE
            if (this.instance.getStackingConstraints()[vertexTwo][vertexOne] == 1) {
                // assign to ground and 2nd level
                if (this.instance.getStacks()[cnt][2] == -1) {
                    this.instance.getStacks()[cnt][2] = vertexOne;
                    this.instance.getStacks()[cnt][1] = vertexTwo;
                    return true;
                // assign to 2nd and 3rd level
                } else if (this.instance.getStacks()[cnt][1] == -1
                        && this.instance.getStackingConstraints()[vertexOne][this.instance.getStacks()[cnt][2]] == 1) {
                    this.instance.getStacks()[cnt][1] = vertexOne;
                    this.instance.getStacks()[cnt][0] = vertexTwo;
                    return true;
                // no two free positions
                } else {
                    cnt++;
                }

            // ORDER TWO
            } else {
                if (this.instance.getStacks()[cnt][2] == -1) {
                    this.instance.getStacks()[cnt][2] = vertexTwo;
                    this.instance.getStacks()[cnt][1] = vertexOne;
                    return true;

                } else if (this.instance.getStacks()[cnt][1] == -1
                        && this.instance.getStackingConstraints()[vertexTwo][this.instance.getStacks()[cnt][2]] == 1) {
                    this.instance.getStacks()[cnt][1] = vertexTwo;
                    this.instance.getStacks()[cnt][0] = vertexOne;
                    return true;
                } else {
                    cnt++;
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

    public boolean setStacks(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {

        if (matchedItems.size() * 2 + unmatchedItems.size() != this.instance.getItems().length) {
            System.out.println("PROBLEM: number of matched items + number of unmatched items != number of items");
            System.exit(1);
        }

        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();

        // Edges that contain inflexible items are prioritized.
        ArrayList<MCMEdge> prioritizedEdges = new ArrayList<>();

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

        for (int item : unmatchedItems) {
            if (this.computeRowRatingForUnmatchedItem(item) <= 10) {
                this.unstackableItems.add(item);
                if (!this.assignItemToFirstPossiblePosition(item)) {
                    return false;
                }
            }
        }

//        System.out.println("unexpected: " + (this.unstackableItems.size() + prioritizedEdges.size()));
//        System.out.println("stacks: " + this.instance.getStacks().length);

//        int unexpected = this.unstackableItems.size() + prioritizedEdges.size();
//        if (Math.abs(unexpected - this.instance.getStacks().length) < 30) {
//            return false;
//        }

        // Sets MCM pairs to level 0 and 1 of stacks
        // (0 is actually the top level, so level 0 is 2 here)
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

        ArrayList<Integer> stillTodoItems = new ArrayList<>(unmatchedItems);
        stillTodoItems.addAll(this.additionalUnmatchedItems);

        ArrayList<Integer> toBeRemoved = new ArrayList<>();
        for (int item : stillTodoItems) {
            if (this.unstackableItems.contains(item)) {
                toBeRemoved.add(item);
            }
        }

        while (toBeRemoved.size() > 0) {
            int idx = stillTodoItems.indexOf(toBeRemoved.get(0));
            stillTodoItems.remove(idx);
            toBeRemoved.remove(0);
        }

        // TODO: Check - could cause problems
        for (int item : stillTodoItems) {
            // If we still have a one here, we have a problem.
            if (this.computeRowRatingForUnmatchedItem(item) == 1) {
                return false;
            }
        }

        this.assignUnmatchedItemsInGivenOrder(stillTodoItems);
        return true;
    }

    public void generateStackingConstraintGraph(DefaultUndirectedGraph<String, DefaultEdge> graph) {
        for (int i : this.instance.getItems()) {
            graph.addVertex("v" + i);
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

    public void parseNewMCM(ArrayList<ArrayList<Integer>> stackAssignmentOne, EdmondsMaximumCardinalityMatching newMCM) {
        for (Object edge : newMCM.getMatching().getEdges()) {
            String parsed = edge.toString().replace("(edge(", "").replace(") : v", ", ").replace(")", "").trim();
            int first = Integer.parseInt(parsed.split(",")[0].trim());
            int second = Integer.parseInt(parsed.split(",")[1].trim());
            int third = Integer.parseInt(parsed.split(",")[2].trim());

            ArrayList<Integer> currAssignment = new ArrayList<>();
            currAssignment.add(first);
            currAssignment.add(second);
            currAssignment.add(third);
            stackAssignmentOne.add(new ArrayList<>(currAssignment));
        }
    }

    public EdmondsMaximumCardinalityMatching<String, DefaultEdge> getMCMForUnmatchedItems(ArrayList<Integer> toDo) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        for (int item : toDo) {
            graph.addVertex("v" + item);
        }

        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < toDo.size(); i++) {
            for (int j = 0; j < toDo.size(); j++) {

                if (toDo.get(i) != toDo.get(j) && this.instance.getStackingConstraints()[toDo.get(i)][toDo.get(j)] == 1
                        || this.instance.getStackingConstraints()[toDo.get(j)][toDo.get(i)] == 1) {

                    if (!graph.containsEdge("v" + toDo.get(j), "v" + toDo.get(i))) {
                        graph.addEdge("v" + toDo.get(i), "v" + toDo.get(j));
                    }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        return mcm;
    }

    public void generateSpecialGraph(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            ArrayList<MCMEdge> edges,
            ArrayList<Integer> unmatchedItems,
            int stacksNeeded) {

        // adding the first m edges from the mcm as nodes to the graph
        for (int i = 0; i < stacksNeeded; i++) {
            graph.addVertex("edge" + edges.get(i));
        }

        // adding all unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            graph.addVertex("v" + i);
        }

        for (int i = 0; i < stacksNeeded; i++) {
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

    public ArrayList<Integer> getCurrentListOfUnmatchedItems(int length, ArrayList<MCMEdge> edges, ArrayList<Integer> todoItems) {
        int cnt = 0;
        ArrayList<Integer> MCMItems = new ArrayList<>();
        for (MCMEdge e : edges) {
            if (cnt < length) {
                MCMItems.add(e.getVertexOne());
                MCMItems.add(e.getVertexTwo());
            } else {
                break;
            }
            cnt++;
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int i : todoItems) {
            if (!MCMItems.contains(i)) {
                unmatchedItems.add(i);
            }
        }
        return unmatchedItems;
    }

    public ArrayList<Integer> getUnmatchedItemsFromStorageAreaSnapshot(ArrayList<ArrayList<Integer>> storageArea) {
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

        ArrayList<Integer> toDo = new ArrayList<>();
        for (int i : this.instance.getItems()) {
            if (!alreadyAssignedItems.contains(i)) {
                toDo.add(i);
            }
        }
        return toDo;
    }

    public void iterativeMCMApproach(EdmondsMaximumCardinalityMatching itemPairs, int numberOfEdgesToBeUsed, ArrayList<Integer> remainingItems) {

        System.out.println("remaining items: " + remainingItems.size());

        if (remainingItems.size() == 0 || this.previousNumberOfRemainingItems == remainingItems.size()) {
            if (this.previousNumberOfRemainingItems != this.instance.getItems().length) { return; }
        }
        this.previousNumberOfRemainingItems = remainingItems.size();
        ArrayList<MCMEdge> edges = new ArrayList<>();
        this.parseMCM(edges, itemPairs);
        this.assignColRatingToEdges(edges);
        Collections.sort(edges);

        // COMPUTING COMPATIBLE ITEM-TRIPLES FROM ITEM-PAIRS AND REMAINING ITEMS
        ArrayList<Integer> unmatchedItems = this.getCurrentListOfUnmatchedItems(numberOfEdgesToBeUsed, edges, remainingItems);
        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateSpecialGraph(graph, edges, unmatchedItems, numberOfEdgesToBeUsed);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemTriples = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<ArrayList<Integer>> currentStackAssignment = new ArrayList<>();
        this.parseNewMCM(currentStackAssignment, itemTriples);
        this.stackAssignments.addAll(currentStackAssignment);

        // COMPUTING COMPATIBLE ITEM-PAIRS FROM REMAINING ITEMS
        unmatchedItems = this.getUnmatchedItemsFromStorageAreaSnapshot(currentStackAssignment);
        EdmondsMaximumCardinalityMatching remainingItemPairs = this.getMCMForUnmatchedItems(unmatchedItems);
        edges = new ArrayList<>();
        this.parseMCM(edges, remainingItemPairs);
        numberOfEdgesToBeUsed = (int)Math.ceil(edges.size() / 3);

        this.iterativeMCMApproach(remainingItemPairs, numberOfEdgesToBeUsed, unmatchedItems);
    }

    public void completeStackAssignments() {
        ArrayList<Integer> remainingItems = this.getUnmatchedItemsFromStorageAreaSnapshot(this.stackAssignments);

        EdmondsMaximumCardinalityMatching finalMCM = this.getMCMForUnmatchedItems(remainingItems);
        ArrayList<MCMEdge> edges = new ArrayList<>();
        this.parseMCM(edges, finalMCM);

        for (MCMEdge e : edges) {
            if (remainingItems.contains(e.getVertexOne())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexOne()));
            }
            if (remainingItems.contains(e.getVertexTwo())) {
                remainingItems.remove(remainingItems.indexOf(e.getVertexTwo()));
            }
        }

        this.assignColRatingToEdges(edges);
        Collections.sort(edges);
        // TODO: search for reasonable way to compute the number of used edges
        int numberOfUsedEdges = edges.size() - (int)Math.ceil(edges.size() / 2) + 10;

        ArrayList<ArrayList<MCMEdge>> edgePermutations = new ArrayList<>();
        ArrayList<ArrayList<Integer>> listsOfRemainingItems = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            ArrayList<Integer> tmpRemainingItems = new ArrayList<>(remainingItems);
            ArrayList<MCMEdge> tmpEdges = new ArrayList<>();

            for (int j = 0; j < edges.size(); j++) {
                if (j < numberOfUsedEdges) {
                    tmpEdges.add(edges.get(j));
                } else {
                    tmpRemainingItems.add(edges.get(j).getVertexOne());
                    tmpRemainingItems.add(edges.get(j).getVertexTwo());
                }
            }

            // TODO: add check for already used shuffles

            Collections.shuffle(edges);
            edgePermutations.add(new ArrayList<>(tmpEdges));
            listsOfRemainingItems.add(new ArrayList<>(tmpRemainingItems));
        }

        int maxNumberOfAssignments = 0;
        ArrayList<ArrayList<Integer>> bestStackAssignments = new ArrayList<>();

        for (ArrayList<MCMEdge> currentEdges : edgePermutations) {

            int numberOfAssignments = 0;
            ArrayList<ArrayList<Integer>> currentStackAssignments = new ArrayList<>();
            ArrayList<Integer> currentRemainingItems = new ArrayList<>(listsOfRemainingItems.get(edgePermutations.indexOf(currentEdges)));

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

            if (numberOfAssignments > maxNumberOfAssignments) {
                maxNumberOfAssignments = numberOfAssignments;
                bestStackAssignments = new ArrayList<>(currentStackAssignments);
                remainingItems = new ArrayList<>(currentRemainingItems);
            }
        }

        this.stackAssignments.addAll(bestStackAssignments);

        for (int item : remainingItems) {
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(item);
            this.stackAssignments.add(tmp);
        }

        remainingItems = this.getUnmatchedItemsFromStorageAreaSnapshot(this.stackAssignments);
        System.out.println("stacks used: " + this.stackAssignments.size());
        System.out.println("remaining items: " + remainingItems);

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

    public Solution stillToBeNamedApproach(EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm, boolean optimizeSolution) {

        ArrayList<ArrayList<MCMEdge>> matchingSubsets = this.getInitialStackAssignmentsFromMCM(mcm);
        Solution bestSol = new Solution();

        int generatedSolutions = 0;

        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {

            // Time limit of 5 minutes
            if ((System.currentTimeMillis() - startTime) / 1000.0 >= 15) {
                break;
            }

            if (generatedSolutions > 1000000) { break; }

            for (List<Integer> unmatchedItems : this.getUnmatchedPermutations(matchedItems)) {

                if (!this.setStacks(matchedItems, unmatchedItems)) {
                    this.instance.resetStacks();
                    break;
                }
                Solution sol1 = new Solution(0, false, this.instance);

//                System.out.println(sol1.getNumberOfAssignedItems());

                if (!optimizeSolution && sol1.isFeasible()) {
                    return sol1;
                }

                if (sol1.isFeasible() && sol1.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol1);
                }

                this.instance.resetStacks();

                ArrayList<MCMEdge> copyMatchedItems = new ArrayList<>();
                for (MCMEdge e : matchedItems) {
                    copyMatchedItems.add(new MCMEdge(e));
                }

                for (MCMEdge e : copyMatchedItems) {
                    e.flipVertices();
                }

                if (!this.setStacks(copyMatchedItems, unmatchedItems)) {
                    this.instance.resetStacks();
                    break;
                }
                Solution sol2 = new Solution(0, false, this.instance);
                sol2.getNumberOfAssignedItems();

                if (!optimizeSolution && sol2.isFeasible()) {
                    return sol2;
                }

                if (sol2.isFeasible() && sol2.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol2);
                }

                generatedSolutions += 2;
                this.instance.resetStacks();
            }
        }

        System.out.println("number of solutions: " + generatedSolutions);
        return bestSol;
    }

    public Solution capThreeApproach(boolean optimizeSolution, double startTime) {

        // TODO:
        //   - calc MCM between items for b = 2
        //   - interpret MCM edges as stack assignments
        //   - iterate over remaining items and assign to feasible stack

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateStackingConstraintGraph(graph);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<Integer> items = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            items.add(item);
        }
        this.iterativeMCMApproach(mcm, this.instance.getStacks().length, items);
        this.completeStackAssignments();

        Solution sol = new Solution(0, false, this.instance);
        sol.transformStackAssignmentIntoValidSolutionIfPossible();
        System.out.println("sol feasible: " + sol.isFeasible());
        System.out.println(sol.getNumberOfAssignedItems());

        return sol;
//        return stillToBeNamedApproach(mcm, optimizeSolution);
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
            sol = this.capThreeApproach(optimizeSolution, startTime);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        }
        return sol;
    }
}
