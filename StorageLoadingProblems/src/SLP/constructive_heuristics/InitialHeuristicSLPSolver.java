package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class InitialHeuristicSLPSolver {

    private final int NUMBER_OF_SHUFFLES_FOR_UNMATCHED_ITEMS = 20;
    private final int NUMBER_OF_SHUFFLES_FOR_MCM_EDGES = 20;

    private Instance instance;
    private ArrayList<ArrayList<MCMEdge>> edgeShuffles;
    private ArrayList<ArrayList<Integer>> unmatchedItemShuffles;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
        this.edgeShuffles = new ArrayList<>();
        this.unmatchedItemShuffles = new ArrayList<>();
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

    public ArrayList<ArrayList<MCMEdge>> extractInitialStackAssignmentsFromMCM(EdmondsMaximumCardinalityMatching mcm) {

        // You can possibly take all k element subsets of the mcm as initial stack assignment.

        // I'll start with testing two:
        //      - from start to k
        //      - from end to end - k

        // Or even better:
        //  - take a certain amount of random shuffles of the edges in the mcm

        ArrayList<MCMEdge> matchedItems = new ArrayList<>();
        this.parseMCM(matchedItems, mcm);
        assignRatingToEdges(matchedItems);

        // sort decreasing
        Collections.sort(matchedItems);
        Collections.reverse(matchedItems);

        System.out.println("#######################################");
        for (MCMEdge e : matchedItems) {
            System.out.println(e.getVertexOne() + " - " + e.getVertexTwo() + " --> " + e.getRating());
        }
        System.out.println(matchedItems);
        System.out.println("#######################################");

        this.edgeShuffles = new ArrayList<>();
        int numberOfShuffles = 0;

        ArrayList<ArrayList<MCMEdge>> stackAssignments = new ArrayList<>();

        while (numberOfShuffles < this.NUMBER_OF_SHUFFLES_FOR_MCM_EDGES) {

            int unsuccessfulShuffleAttempts = 0;

            while (this.alreadyMatched(matchedItems) && unsuccessfulShuffleAttempts < 5) {
                System.out.println("unsuccessful shuffle attempt no. " + unsuccessfulShuffleAttempts);
                unsuccessfulShuffleAttempts++;
                Collections.shuffle(matchedItems);
            }

            if (unsuccessfulShuffleAttempts == 5) { break; }

            this.edgeShuffles.add(new ArrayList<>(matchedItems));
            ArrayList<MCMEdge> currentStackAssignment = new ArrayList<>();
            for (int i = 0; i < this.instance.getStacks().length; i++) {
                currentStackAssignment.add(matchedItems.get(i));
            }
            System.out.println("used stack assignment: " + currentStackAssignment);
            stackAssignments.add(currentStackAssignment);
            Collections.shuffle(matchedItems);
            numberOfShuffles++;
        }

        return stackAssignments;
    }

    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> matchedItems) {

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        ArrayList<Integer> listOfMatchedItems = new ArrayList<>();

        for (MCMEdge edge : matchedItems) {
            listOfMatchedItems.add(edge.getVertexOne());
            listOfMatchedItems.add(edge.getVertexTwo());
        }

        for (int item : this.instance.getItems()) {
            if (!listOfMatchedItems.contains(item)) {
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

    public void assignUnmatchedItemsInGivenOrder(ArrayList<Integer> unmatchedItems, int[][] tmpCurrentStacks, ArrayList<Integer> tmpList) {
        // Could be extended to try several possible allocations
        for (int item : unmatchedItems) {
            for (int i = 0; i < this.instance.getStacks().length; i++) {

                int currentTopMostItem = tmpCurrentStacks[i][1];

                if (currentTopMostItem == -1) {
                    if (tmpCurrentStacks[i][0] == -1) {
                        tmpCurrentStacks[i][0] = item;
                        tmpList.remove(tmpList.indexOf(item));
                        break;
                    } else {
                        if (this.instance.getStackingConstraints()[item][tmpCurrentStacks[i][0]] == 1) {
                            tmpCurrentStacks[i][1] = item;
                            tmpList.remove(tmpList.indexOf(item));
                            break;
                        }
                    }
                }
                if (this.instance.getStackingConstraints()[item][currentTopMostItem] == 1) {
                    if (tmpCurrentStacks[i][2] == -1) {
                        tmpCurrentStacks[i][2] = item;
                        tmpList.remove(tmpList.indexOf(item));
                        break;
                    }
                }
            }
        }
    }

    public void setStacks(ArrayList<MCMEdge> matchedItems, ArrayList<Integer> unmatchedItems) {

        int cnt = 0;
        // Sets MCM pairs to level 0 and 1 of stacks
        for (MCMEdge edge : matchedItems) {
            if (cnt < this.instance.getStacks().length) {
                int vertexOne = edge.getVertexOne();
                int vertexTwo = edge.getVertexTwo();
                this.instance.getStacks()[cnt][0] = vertexOne;
                this.instance.getStacks()[cnt][1] = vertexTwo;
            }
            cnt++;
        }

//        System.out.println("nomatch: " + unmatchedItems);

        ArrayList<Integer> tmpList = new ArrayList<>(unmatchedItems);
        int[][] tmpCurrentStacks = new int[this.instance.getStacks().length][this.instance.getStacks()[0].length];
        int numberOfShuffles = 0;

        while (!tmpList.isEmpty() && numberOfShuffles < this.NUMBER_OF_SHUFFLES_FOR_UNMATCHED_ITEMS) {
            this.copyStackAssignment(this.instance.getStacks(), tmpCurrentStacks);
            tmpList = new ArrayList<>(unmatchedItems);

            Collections.shuffle(tmpList);

            int unsuccessfulShuffleAttempts = 0;
            while (this.alreadyUsed(tmpList) && unsuccessfulShuffleAttempts < 5) {
                System.out.println("unsuccessful shuffle attempt no. " + unsuccessfulShuffleAttempts);
                Collections.shuffle(tmpList);
                unsuccessfulShuffleAttempts++;
            }

            if (unsuccessfulShuffleAttempts == 5) { break; }

            System.out.println(this.unmatchedItemShuffles);

            numberOfShuffles++;
            this.unmatchedItemShuffles.add(new ArrayList<>(tmpList));
            this.assignUnmatchedItemsInGivenOrder(unmatchedItems, tmpCurrentStacks, tmpList);
        }
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

        ArrayList<ArrayList<MCMEdge>> matchingSubsets = this.extractInitialStackAssignmentsFromMCM(mcm);

        ArrayList<Solution> solutions = new ArrayList<>();

        for (ArrayList<MCMEdge> matchedItems : matchingSubsets) {
            this.setStacks(matchedItems, this.getUnmatchedItems(matchedItems));
            Solution sol = new Solution(0, 0, this.instance.getStacks(), "test", false, this.instance.getItems().length);
            solutions.add(sol);
            this.instance.resetStacks();
        }

        for (Solution sol : solutions) {
            // TODO: return the cheapest based on costs
            if (sol.isFeasible()) {
                return sol;
            }
        }

        System.out.println("___________________" + matchingSubsets.get(0));
        System.out.println(getUnmatchedItems(matchingSubsets.get(0)));
        return solutions.get(0);
    }

    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            sol = this.capThreeApproach();
        }
        return sol;
    }
}
