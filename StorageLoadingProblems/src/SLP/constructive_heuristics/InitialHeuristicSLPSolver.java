package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

public class InitialHeuristicSLPSolver {

    private Instance instance;
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;

    private ArrayList<List<Integer>> alreadyUsedShuffles;

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
        edgePermutations.add(edges);

        Permutations.of(edges).forEach(p -> {
            List<MCMEdge> edgeList = p.collect(Collectors.toList());
            edgePermutations.add(edgeList);
        });

        ArrayList<ArrayList<MCMEdge>> stackAssignments = new ArrayList<>();

        for (List<MCMEdge> edgePerm : edgePermutations) {
            ArrayList<MCMEdge> currentStackAssignment = new ArrayList<>();
            for (int i = 0; i < this.instance.getStacks().length; i++) {
                currentStackAssignment.add(edgePerm.get(i));
            }
            stackAssignments.add(currentStackAssignment);
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

    public void assignUnmatchedItemsInGivenOrder(int[][] tmpCurrentStacks, List<Integer> unmatchedItems) {

//        ArrayList<Integer> tmp = new ArrayList<>(unmatchedItems);

        for (int item : unmatchedItems) {

            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

                int levelOfCurrentTopMostItem = -1;
                for (int level = 2; level > 0; level--) {
                    if (tmpCurrentStacks[stack][level] != -1) {
                        levelOfCurrentTopMostItem = level;
                    }
                }

                if (levelOfCurrentTopMostItem == -1) {
                    // assign to ground level
                    tmpCurrentStacks[stack][2] = item;
//                    tmp.remove(tmp.indexOf(item));
                    break;
                } else {
                    if (this.instance.getStackingConstraints()[item][tmpCurrentStacks[stack][levelOfCurrentTopMostItem]] == 1) {
                        if (levelOfCurrentTopMostItem > 0) {
                            if (tmpCurrentStacks[stack][levelOfCurrentTopMostItem - 1] == -1) {
                                tmpCurrentStacks[stack][levelOfCurrentTopMostItem - 1] = item;
//                                tmp.remove(tmp.indexOf(item));
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
        if (tmpList.size() < 9) {
            Permutations.of(tmpList).forEach(p -> {
                List<Integer> edgeList = p.collect(Collectors.toList());
                unmatchedItemPermutations.add(edgeList);
            });
        } else {

            ArrayList<List<Integer>> alreadyTested = new ArrayList<>();

            for (int i = 0; i < 40000; i++) {
                List<Integer> edgeList = new ArrayList<>(tmpList);
                unmatchedItemPermutations.add(edgeList);
                Collections.shuffle(tmpList);

                int unsuccessfulShuffleAttempts = 0;
                while (isPartOfAlreadyUsedShuffles(tmpList)) {
                    Collections.shuffle(tmpList);
                    if (unsuccessfulShuffleAttempts == 10) {
                        for (List el : unmatchedItemPermutations) {
                            res.add(el);
                        }
                        return res;
                    }
                    unsuccessfulShuffleAttempts++;
                }
            }
        }

        for (List el : unmatchedItemPermutations) {
            res.add(el);
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

        int[][] tmpCurrentStacks = new int[this.instance.getStacks().length][this.instance.getStacks()[0].length];
        this.copyStackAssignment(this.instance.getStacks(), tmpCurrentStacks);
        this.assignUnmatchedItemsInGivenOrder(tmpCurrentStacks, this.getUnmatchedItems(matchedItems));

        this.copyStackAssignment(tmpCurrentStacks, this.instance.getStacks());
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
        ArrayList<Solution> solutions = new ArrayList<>();

        Solution bestSol = new Solution();

        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {

            if (solutions.size() > 2000000) { break; }

//        for (int i = 0; i < 50; i++) {
//            ArrayList<MCMEdge> matchedItems = matchingSubsets.get(i);
//            Collections.shuffle(matchingSubsets);

            for (List<Integer> unmatchedItems : this.getUnmatchedPerms(matchedItems)) {

                this.setStacks(matchedItems, unmatchedItems);
                Solution sol1 = new Solution(0, false, this.instance);
                solutions.add(sol1);

                if (sol1.isFeasible() && sol1.getCost() < bestSol.getCost()) {
                    bestSol = sol1;
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
                solutions.add(sol2);

                if (sol2.isFeasible() && sol2.getCost() < bestSol.getCost()) {
                    bestSol = sol2;
                }

                /// TODO: implement option to just look for feasible solutions
//                if (sol2.isFeasible()) {
//                    return sol2;
//                }

                this.instance.resetStacks();
            }
        }

        System.out.println("number of solutions: " + solutions.size());
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
            sol = this.capThreeApproach();
        }
        return sol;
    }
}
