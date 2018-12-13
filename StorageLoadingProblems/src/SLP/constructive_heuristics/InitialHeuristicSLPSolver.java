package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
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

    public void assignRatingToEdges(ArrayList<MCMEdge> matchedItems) {

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

    public ArrayList<ArrayList<MCMEdge>> getInitialStackAssignmentsFromMCM(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<MCMEdge> edges = new ArrayList<>();
        this.parseMCM(edges, mcm);
        this.assignRatingToEdges(edges);
        // The edges are sorted based on their ratings.
        Collections.sort(edges);

        ArrayList<List> edgePermutations = new ArrayList<>();
        // The first permutation that is added is the one based on the sorting
        // which should be the most promising stack assignment.
        edgePermutations.add(new ArrayList(edges));

        if (edges.size() < 9) {
            for (List<MCMEdge> edgeList : Collections2.permutations(edges)) {
                edgePermutations.add(new ArrayList(edgeList));
            }
        } else {
            for (int i = 0; i < 40000; i++) {
                edgePermutations.add(new ArrayList(edges));
                Collections.shuffle(edges);
            }
        }

        ArrayList<ArrayList<MCMEdge>> stackAssignments = new ArrayList<>();

        for (List<MCMEdge> edgePerm : edgePermutations) {
            ArrayList<MCMEdge> currentStackAssignment = new ArrayList<>();
            for (int i = 0; i < this.instance.getStacks().length; i++) {
                currentStackAssignment.add(edgePerm.get(i));
            }
            stackAssignments.add(new ArrayList<>(currentStackAssignment));
        }

        return stackAssignments;
    }

    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> edges) {

        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (MCMEdge edge : edges) {
            matchedItems.add(edge.getVertexOne());
            matchedItems.add(edge.getVertexTwo());
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();

        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item) && !unstackableItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }

        unmatchedItems.addAll(this.additionalUnmatchedItems);

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

    public boolean isPartOfAlreadyUsedShuffles(ArrayList<Integer> currentShuffle) {
        for (List<Integer> shuffle : this.alreadyUsedShuffles) {
            if (shuffle.equals(currentShuffle)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<List<Integer>> getUnmatchedPerms(ArrayList<MCMEdge> matchedItems) {

        ArrayList<Integer> tmpList = new ArrayList<>(this.getUnmatchedItems(matchedItems));
        ArrayList<List<Integer>> res = new ArrayList<>();
        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        // For up to 8 items, the computation of permutations is possible in a reasonable time frame,
        // after that 40k random shuffles are used instead.
        if (tmpList.size() < 8) {
            for (List<Integer> edgeList : Collections2.permutations(tmpList)) {
                unmatchedItemPermutations.add(new ArrayList<>(edgeList));
            }
        } else {

            for (int i = 0; i < 40000; i++) {
                unmatchedItemPermutations.add(new ArrayList<>(tmpList));
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

    public void setStacks(ArrayList<MCMEdge> matchedItems, List<Integer> unmatchedItems) {

        int cnt = 0;
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();

        for (int item : unmatchedItems) {
            int sum = 0;
            for (int constraint : this.instance.getStackingConstraints()[item]) {
                sum += constraint;
            }
            if (sum <= 1) {
                this.instance.getStacks()[cnt][2] = item;
                this.unstackableItems.add(item);
                cnt++;
            }
        }

        // Sets MCM pairs to level 0 and 1 of stacks
        // (0 is actually the top level, so level 0 is 2 here)
        for (MCMEdge edge : matchedItems) {
            if (cnt < this.instance.getStacks().length) {

                int vertexOne = edge.getVertexOne();
                int vertexTwo = edge.getVertexTwo();

                if (this.instance.getStackingConstraints()[vertexTwo][vertexOne] == 1) {
                    this.instance.getStacks()[cnt][2] = vertexOne;
                    this.instance.getStacks()[cnt][1] = vertexTwo;
                } else {
                    this.instance.getStacks()[cnt][2] = vertexTwo;
                    this.instance.getStacks()[cnt][1] = vertexOne;
                }

            } else {
                this.additionalUnmatchedItems.add(edge.getVertexOne());
                this.additionalUnmatchedItems.add(edge.getVertexTwo());
            }
            cnt++;
        }

//        int[][] tmpCurrentStacks = new int[this.instance.getStacks().length][this.instance.getStacks()[0].length];
//        this.copyStackAssignment(this.instance.getStacks(), tmpCurrentStacks);
        this.assignUnmatchedItemsInGivenOrder(this.getUnmatchedItems(matchedItems));
//        this.copyStackAssignment(tmpCurrentStacks, this.instance.getStacks());

//        System.out.println("############################################");
//        for (int i = 0; i < this.instance.getStacks().length; i++) {
//            for (int j = 0; j < this.instance.getStacks()[i].length; j++) {
//                System.out.print(this.instance.getStacks()[i][j] + " ");
//            }
//            System.out.println();
//        }
//        System.out.println("############################################");
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

    public Solution capThreeApproach() {

        // TODO:
        //   - calc MCM between items for b = 2
        //   - interpret MCM edges as stack assignments
        //   - iterate over remaining items and assign to feasible stack

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        this.generateStackingConstraintGraph(graph);

        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

        ArrayList<ArrayList<MCMEdge>> matchingSubsets = this.getInitialStackAssignmentsFromMCM(mcm);

//        ArrayList<Solution> solutions = new ArrayList<>();

        Solution bestSol = new Solution();

        int generatedSolutions = 0;

        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {

//            if (solutions.size() > 20000) { break; }
            if (generatedSolutions > 200000) { break; }

//        for (int i = 0; i < 50; i++) {
//            ArrayList<MCMEdge> matchedItems = matchingSubsets.get(i);
//            Collections.shuffle(matchingSubsets);

            for (List<Integer> unmatchedItems : this.getUnmatchedPerms(matchedItems)) {

                System.out.println(unmatchedItems);

                this.setStacks(matchedItems, unmatchedItems);
                Solution sol1 = new Solution(0, false, this.instance);
//                solutions.add(new Solution(sol1));

                if (sol1.isFeasible() && sol1.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol1);
                }

                // TODO: implement option to just look for feasible solutions
//                if (sol1.isFeasible()) {
//                    return sol1;
//                }

                this.instance.resetStacks();

                for (MCMEdge e : matchedItems) {
                    e.flipVertices();
                }

                this.setStacks(matchedItems, unmatchedItems);
                Solution sol2 = new Solution(0, false, this.instance);
//                solutions.add(new Solution(sol2));

                if (sol2.isFeasible() && sol2.getCost() < bestSol.getCost()) {
                    bestSol = new Solution(sol2);
                }

                generatedSolutions += 2;

                /// TODO: implement option to just look for feasible solutions
//                if (sol2.isFeasible()) {
//                    return sol2;
//                }

                this.instance.resetStacks();
            }
        }

//        System.out.println("number of solutions: " + solutions.size());
        System.out.println("number of solutions: " + generatedSolutions);
//        for (Solution sol : solutions) {
//            // TODO: return the cheapest based on costs
//            if (sol.isFeasible()) {
//                return sol;
//            }
//        }

        return bestSol;
    }

    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();
            sol = this.capThreeApproach();
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);
        }
        return sol;
    }
}
