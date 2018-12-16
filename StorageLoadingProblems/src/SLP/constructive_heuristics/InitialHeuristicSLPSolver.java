package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.MapUtil;
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

    private double startTime;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
        this.alreadyUsedShuffles = new ArrayList<>();
    }

    public void parseMCM(ArrayList<MCMEdge> matchedItems, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());

            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
    }

    public void assignRatingToEdgesStrategyOne(ArrayList<MCMEdge> matchedItems) {
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

    public void assignRatingToEdgesStrategyTwo(ArrayList<MCMEdge> matchedItems) {

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

        int numberOfEdgesToBeReplaced = (int) (0.4 * this.instance.getStacks().length);
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

        this.assignRatingToEdgesStrategyOne(edges);
        this.assignRatingToEdgesStrategyTwo(edgesCopy);

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
        for (int cnt = 0; cnt < 15; cnt++) {
            edgePermutations.add(new ArrayList(this.edgeExchange(edges)));
        }
        for (int cnt = 0; cnt < 15; cnt++) {
            edgePermutations.add(new ArrayList(this.edgeExchange(edgesCopy)));
        }

        if (edges.size() < 9) {
            for (List<MCMEdge> edgeList : Collections2.permutations(edges)) {
                edgePermutations.add(new ArrayList(edgeList));
            }
        } else {
            for (int i = 0; i < 40000; i++) {
                Collections.shuffle(edges);
                edgePermutations.add(new ArrayList(edges));
            }
        }

        return edgePermutations;
    }

    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> edges) {

        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (MCMEdge edge : edges) {
            matchedItems.add(edge.getVertexOne());
            matchedItems.add(edge.getVertexTwo());
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();

        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item) /*&& !this.unstackableItems.contains(item)*/) {
                unmatchedItems.add(item);
            }
        }

//        unmatchedItems.addAll(this.additionalUnmatchedItems);

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

//        System.out.println("SIZE: " + unmatchedItems.size());

        for (int item : unmatchedItems) {

//            for (int i = 0; i < this.instance.getStacks().length; i++) {
//                for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
//                    System.out.print(this.instance.getStacks()[i][j] + " ");
//                }
//                System.out.println();
//            }

            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

//                System.out.println("CURRENT STACK:####################");
//                for (int i = 0; i < this.instance.getStacks()[stack].length; i++) {
//                    System.out.print(this.instance.getStacks()[stack][i] + " ");
//                }

                int levelOfCurrentTopMostItem = -99;
                for (int level = 2; level >= 0; level--) {
                    if (this.instance.getStacks()[stack][level] != -1) {
                        levelOfCurrentTopMostItem = level;
                    }
                }

//                System.out.println("current top most item: " + this.instance.getStacks()[stack][levelOfCurrentTopMostItem]);
//                System.out.println("##################################");

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

    public int computeRatingForUnmatchedItem(int item) {
        int rating = 0;

        for (int j = 0; j < this.instance.getStackingConstraints()[item].length; j++) {
            rating += this.instance.getStackingConstraints()[item][j];
        }
        return rating;
    }

    public ArrayList<List<Integer>> getUnmatchedPerms(ArrayList<MCMEdge> matchedItems) {

        ArrayList<Integer> tmpList = new ArrayList<>(this.getUnmatchedItems(matchedItems));
        ArrayList<List<Integer>> res = new ArrayList<>();
        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        ////////////////////////////////////////////////////////////

        // TODO: Order by rating (hardest cases first)
        HashMap<Integer, Integer> unmatchedItemRatings = new HashMap<>();
        for (int item : tmpList) {
            unmatchedItemRatings.put(item, this.computeRatingForUnmatchedItem(item));
        }

//        System.out.println("BEFORE: " + unmatchedItemRatings);
        Map<Integer, Integer> sorted = MapUtil.sortByValue(unmatchedItemRatings);
//        System.out.println("AFTER: " + sorted);

        ArrayList<Integer> newApproach = new ArrayList<>();

        for (int i : sorted.keySet()) {
            newApproach.add(i);
        }

        ////////////////////////////////////////////////////////////

        // For up to 8 items, the computation of permutations is possible in a reasonable time frame,
        // after that 40k random shuffles are used instead.
        if (tmpList.size() < 7) {
            for (List<Integer> edgeList : Collections2.permutations(tmpList)) {
                unmatchedItemPermutations.add(new ArrayList<>(edgeList));
            }
        } else {

            for (int i = 0; i < 500; i++) {
                unmatchedItemPermutations.add(new ArrayList<>(newApproach));
                Collections.reverse(newApproach);
                unmatchedItemPermutations.add(new ArrayList<>(newApproach));
                Collections.shuffle(tmpList);

                int unsuccessfulShuffleAttempts = 0;
                while (isPartOfAlreadyUsedShuffles(tmpList)) {
                    System.out.println("already");
                    Collections.shuffle(tmpList);
                    if (unsuccessfulShuffleAttempts == 5) {
                        for (List el : unmatchedItemPermutations) {
                            res.add(el);
                        }
                        return res;
                    }
                    unsuccessfulShuffleAttempts++;
                }
                this.alreadyUsedShuffles.add(new ArrayList<>(tmpList));
            }
        }

        for (List el : unmatchedItemPermutations) {
            res.add(new ArrayList<>(el));
        }
        return res;
    }

    public boolean assignItemToFirstPossiblePosition(int item) {

        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 2; j > 0; j--) {

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

//                if (this.instance.getStacks()[i][j] == -1 && this.instance.getStacks()[i][j - 1] == -1) {
//                    this.instance.getStacks()[i][j] = item;
//                    return true;
//                } else if (this.instance.getStacks()[i][j] != -1 && this.instance.getStacks()[i][j - 1] == -1) {
//                    if (this.instance.getStackingConstraints()[item][this.instance.getStacks()[i][j]] == 1) {
//                        this.instance.getStacks()[i][j - 1] = item;
//                        return true;
//                    }
//                }
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

    public boolean setStacks(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {

        ArrayList<Integer> test = new ArrayList<>();

        for (MCMEdge e : matchedItems) {
            test.add(e.getVertexTwo());
            test.add(e.getVertexOne());
        }
        for (int i : unmatchedItems) {
            test.add(i);
        }

        Set<Integer> set = new HashSet<Integer>(test);
        if(set.size() < test.size()){
            System.out.println("DUPLICATES");
        }

//        System.out.println("number of matched plus number of unmatched in setStacks: " + (matchedItems.size() * 2 + unmatchedItems.size()));

        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();

        ArrayList<MCMEdge> prioEdge = new ArrayList<>();

        int cnt = 0;

        for (MCMEdge edge : matchedItems) {
            int vertexOne = edge.getVertexOne();
            int vertexTwo = edge.getVertexTwo();

            if (this.computeRatingForUnmatchedItem(vertexOne) == 1 || this.computeRatingForUnmatchedItem(vertexTwo) == 1) {

                prioEdge.add(new MCMEdge(vertexOne, vertexTwo, 0));

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
            int sum = 0;
            for (int constraint : this.instance.getStackingConstraints()[item]) {
                sum += constraint;
            }

            if (sum <= 1) {
                this.unstackableItems.add(item);
                if (!this.assignItemToFirstPossiblePosition(item)) {
                    return false;
                }
            }
        }

//        cnt = 0;
//        for (int i = 0; i < this.instance.getStacks().length; i++) {
//            if (this.instance.getStacks()[i][2] == -1) {
//                cnt = i;
//            }
//        }

        // Sets MCM pairs to level 0 and 1 of stacks
        // (0 is actually the top level, so level 0 is 2 here)
        for (MCMEdge edge : matchedItems) {
            boolean cont = false;
            for (MCMEdge e : prioEdge) {
                if (e.getVertexOne() == edge.getVertexOne() && e.getVertexTwo() == edge.getVertexTwo()) {
                    cont = true;
                    break;
                }
            }

            if (cont) { continue; }

            // If the edge couldn't be assigned, the solution doesn't work.
            if (!this.assignEdgeToFirstPossiblePosition(edge)) {
                this.additionalUnmatchedItems.add(edge.getVertexOne());
                this.additionalUnmatchedItems.add(edge.getVertexTwo());
            }
        }

        ArrayList<Integer> alr = new ArrayList<>();
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
                if (this.instance.getStacks()[i][j] != -1) {
                    alr.add(this.instance.getStacks()[i][j]);
                }
            }
        }

        ArrayList<Integer> stillTodoItems = new ArrayList<>(unmatchedItems);
        stillTodoItems.addAll(this.additionalUnmatchedItems);

//        Set<Integer> set = new HashSet<Integer>(stillTodoItems);
//        if(set.size() < stillTodoItems.size()){
//            System.out.println("DUPLICATES");
//        }

//        System.out.println(unmatchedItems);

        ArrayList<Integer> toBeRemoved = new ArrayList<>();
        for (int i : stillTodoItems) {
            if (this.unstackableItems.contains(i)) {
                toBeRemoved.add(i);
            }
        }

        while (toBeRemoved.size() > 0) {
            int idx = stillTodoItems.indexOf(toBeRemoved.get(0));
            stillTodoItems.remove(idx);
            toBeRemoved.remove(0);
        }

//        System.out.println("assigned: " + itemsCurrentlyAssigned);
//        System.out.println("assigned size: " + itemsCurrentlyAssigned.size());

        for (int i : stillTodoItems) {
            // If we have a one here, we have a problem
            // Now there are also matched items with sum = 1
            if (this.computeRatingForUnmatchedItem(i) == 1) {
//                System.out.println("PROBLEM");
                return false;
            }
        }

//        System.out.println("todo: " + stillTodoItems);
//        System.out.println("todo size: " + stillTodoItems.size());

        this.assignUnmatchedItemsInGivenOrder(stillTodoItems);

//        System.out.println("############################################");
//        for (int i = 0; i < this.instance.getStacks().length; i++) {
//            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
//                System.out.print(this.instance.getStacks()[i][j] + " ");
//            }
//            System.out.println();
//        }
//        System.out.println("############################################");
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

    public Solution capThreeApproach(boolean optimizeSolution) {

        // TODO:
        //   - calc MCM between items for b = 2
        //   - interpret MCM edges as stack assignments
        //   - iterate over remaining items and assign to feasible stack

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateStackingConstraintGraph(graph);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<MCMEdge>> matchingSubsets = this.getInitialStackAssignmentsFromMCM(mcm);

        Solution bestSol = new Solution();

        int generatedSolutions = 0;

        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {

//            System.out.println(generatedSolutions);
//            System.out.println(matchedItems);

            if (generatedSolutions > 20000) { break; }

            for (List<Integer> unmatchedItems : this.getUnmatchedPerms(matchedItems)) {

                if (!this.setStacks(matchedItems, unmatchedItems)) {
                    this.instance.resetStacks();
//                    System.out.println("break");
                    break;
                }
                Solution sol1 = new Solution(0, false, this.instance);

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
//                    System.out.println("break");
                    break;
                }
                Solution sol2 = new Solution(0, false, this.instance);

                if (!optimizeSolution && sol2.isFeasible()) {
                    return sol2;
                }

                if (sol2.isFeasible() && sol2.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol2);
                }

                generatedSolutions += 2;

//                if (generatedSolutions == 2) {
//
//                    // There seems to be an assignment that conflicts the stacking constraints
//
//                    System.out.println(sol2.getNumberOfAssignedItems());
//                    System.out.println(sol2.isFeasible());
//
//                    for (int i = 0; i < this.instance.getStacks().length; i++) {
//                        for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
//                            System.out.print(this.instance.getStacks()[i][j] + " ");
//                        }
//                        System.out.println();
//                    }
//
//                    System.exit(0);
//                }

                this.instance.resetStacks();
            }
        }

        System.out.println("number of solutions: " + generatedSolutions);

        return bestSol;
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
