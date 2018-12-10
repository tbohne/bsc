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
    private ArrayList<ArrayList<MCMEdge>> edgeShuffles;
    private ArrayList<ArrayList<Integer>> unmatchedItemShuffles;
    private ArrayList<Integer> unstackableItems;
    private ArrayList<Integer> additionalUnmatchedItems;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
        this.edgeShuffles = new ArrayList<>();
        this.unmatchedItemShuffles = new ArrayList<>();
        this.unstackableItems = new ArrayList<>();
        this.additionalUnmatchedItems = new ArrayList<>();
    }

    public void parseMCM(ArrayList<MCMEdge> matchedItems, EdmondsMaximumCardinalityMatching mcm) {
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());

            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
    }

    public boolean alreadyMatched(ArrayList<MCMEdge> matchedItems) {
        for (ArrayList<MCMEdge> alreadyMatched : this.edgeShuffles) {
            if (alreadyMatched.equals(matchedItems)) {
                return true;
            }
        }
        return false;
    }

    public boolean alreadyUsed(ArrayList<Integer> unmatchedItems) {
        for (ArrayList<Integer> shuffle : this.unmatchedItemShuffles) {
            if (shuffle.equals(unmatchedItems)) {
                return true;
            }
        }
        return false;
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
                for (int level = 0; level < tmpCurrentStacks[stack].length; level++) {
                    if (tmpCurrentStacks[stack][level] != -1) {
                        levelOfCurrentTopMostItem = level;
                    }
                }

                if (levelOfCurrentTopMostItem == -1) {
                    // assign to ground level
                    tmpCurrentStacks[stack][0] = item;
//                    tmp.remove(tmp.indexOf(item));
                    break;
                } else {
                    if (this.instance.getStackingConstraints()[item][tmpCurrentStacks[stack][levelOfCurrentTopMostItem]] == 1) {
                        if (levelOfCurrentTopMostItem < 2) {
                            if (tmpCurrentStacks[stack][levelOfCurrentTopMostItem + 1] == -1) {
                                tmpCurrentStacks[stack][levelOfCurrentTopMostItem + 1] = item;
//                                tmp.remove(tmp.indexOf(item));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public ArrayList<List<Integer>> getUnmatchedPerms(ArrayList<MCMEdge> matchedItems) {

        ArrayList<Integer> tmpList = new ArrayList<>(this.getUnmatchedItems(matchedItems));
        ArrayList<List<Integer>> res = new ArrayList<>();

        ArrayList<List<Integer>> unmatchedItemPermutations = new ArrayList<>();

        Permutations.of(tmpList).forEach(p -> {
            List<Integer> edgeList = p.collect(Collectors.toList());
            unmatchedItemPermutations.add(edgeList);
        });

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
                this.instance.getStacks()[cnt][0] = item;
                this.unstackableItems.add(item);
                cnt++;
            }
        }

        // Sets MCM pairs to level 0 and 1 of stacks
        for (MCMEdge edge : matchedItems) {
            if (cnt < this.instance.getStacks().length) {

                int vertexOne = edge.getVertexOne();
                int vertexTwo = edge.getVertexTwo();

                if (this.instance.getStackingConstraints()[vertexTwo][vertexOne] == 1) {
                    this.instance.getStacks()[cnt][0] = vertexOne;
                    this.instance.getStacks()[cnt][1] = vertexTwo;
                } else {
                    this.instance.getStacks()[cnt][0] = vertexTwo;
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

//        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {
        for (int i = 0; i < 50; i++) {

            ArrayList<MCMEdge> matchedItems = matchingSubsets.get(i);

            Collections.shuffle(matchingSubsets);

            for (List<Integer> unmatchedItems : this.getUnmatchedPerms(matchedItems)) {

                this.setStacks(matchedItems, unmatchedItems);
                Solution sollll = new Solution(0, 0, false, this.instance);
                solutions.add(sollll);

                if (sollll.getNumberOfAssignedItems() > bestSol.getNumberOfAssignedItems()) {
                    bestSol = sollll;
                }

                if (sollll.isFeasible()) {
                    return sollll;
                }

                this.instance.resetStacks();

                for (MCMEdge e : matchedItems) {
                    e.flipVertices();
                }
                this.setStacks(matchedItems, unmatchedItems);
                Solution sol2 = new Solution(0, 0, false, this.instance);
                solutions.add(sol2);

                if (sol2.getNumberOfAssignedItems() > bestSol.getNumberOfAssignedItems()) {
                    bestSol = sol2;
                }

                if (sol2.isFeasible()) {
                    return sol2;
                }

                this.instance.resetStacks();
            }
        }

        System.out.println("number of solutions: " + solutions.size());
        for (Solution sol : solutions) {
            // TODO: return the cheapest based on costs
            if (sol.isFeasible()) {
                return sol;
            }
        }

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
