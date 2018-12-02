package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;
import java.util.Collections;

public class InitialHeuristicSLPSolver {

    private Instance instance;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> extractInitialStackAssignmentsFromMCM(EdmondsMaximumCardinalityMatching mcm) {

        // You can possibly take all k element subsets of the mcm as initial stack assignment.
        // I'll start with testing two:
        //  1. from start to k
        //  2. from end to end - k

        ArrayList<ArrayList<Integer>> matchedItems = new ArrayList<>();

        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(vertexOne);
            stack.add(vertexTwo);
            matchedItems.add(stack);
        }

        ArrayList<ArrayList<Integer>> firstVersion = new ArrayList<>();

        for (int i = 0; i < this.instance.getStacks().length; i++) {
            firstVersion.add(matchedItems.get(i));
        }

        ArrayList<ArrayList<Integer>> secondVersion = new ArrayList<>();

        for (int i = matchedItems.size() - 1; i > matchedItems.size() - 1 - this.instance.getStacks().length; i--) {
            secondVersion.add(matchedItems.get(i));
        }

        ArrayList<ArrayList<ArrayList<Integer>>> sol = new ArrayList<>();
        sol.add(firstVersion);
        sol.add(secondVersion);

        return sol;
    }

    public ArrayList<Integer> getUnmatchedItems(ArrayList<ArrayList<Integer>> matchedItems) {

//        int maximumNumberOfPairs = this.instance.getStacks().length;
//        int cnt = 0;
//
//        for (Object edge : mcm.getMatching()) {
//            if (cnt < maximumNumberOfPairs) {
//                int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
//                int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
//                matchedItems.add(vertexOne);
//                matchedItems.add(vertexTwo);
//            }
//            cnt++;
//        }

//        ArrayList<Integer> matchedItems = this.extractItemsFromMatching(mcm);

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        ArrayList<Integer> matchedItemsList = new ArrayList<>();

        for (ArrayList<Integer> edge : matchedItems) {
            matchedItemsList.add(edge.get(0));
            matchedItemsList.add(edge.get(1));
        }

        for (int item : this.instance.getItems()) {
            if (!matchedItemsList.contains(item)) {
                unmatchedItems.add(item);
            }
        }

        return unmatchedItems;
    }

    public void setStacks(ArrayList<ArrayList<Integer>> matchedItems, ArrayList<Integer> unmatchedItems) {

        System.out.println("set stacks");

        int cnt = 0;
        // Sets MCM pairs to level 0 and 1 of stacks
        for (ArrayList<Integer> edge : matchedItems) {
            if (cnt < this.instance.getStacks().length) {
                int vertexOne = edge.get(0);
                int vertexTwo = edge.get(1);
                this.instance.getStacks()[cnt][0] = vertexOne;
                this.instance.getStacks()[cnt][1] = vertexTwo;
            }
            cnt++;
        }

        System.out.println("nomatch: " + unmatchedItems);

        ArrayList<Integer> tmpList = new ArrayList<>(unmatchedItems);
        int numberOfShuffles = 0;
        int[][] tmpCurrentStacks = new int[this.instance.getStacks().length][this.instance.getStacks()[0].length];

        while (!tmpList.isEmpty() && numberOfShuffles < 20) {

            for (int i = 0; i < this.instance.getStacks().length; i++) {
                for (int j = 0; j < this.instance.getStacks()[0].length; j++) {
                    tmpCurrentStacks[i][j] = this.instance.getStacks()[i][j];
                }
            }

            tmpList = new ArrayList<>(unmatchedItems);
            Collections.shuffle(tmpList);
            numberOfShuffles++;

            // Could be extended to try several possible allocations
            for (int item : unmatchedItems) {
                for (int i = 0; i < this.instance.getStacks().length; i++) {

                    int currentTopMostItem = tmpCurrentStacks[i][1];

                    if (currentTopMostItem == -1) {
                        if (tmpCurrentStacks[i][0] == -1) {
                            System.out.println("assign special: " + item);
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
                            System.out.println("assign: " + item);
                            tmpCurrentStacks[i][2] = item;
                            tmpList.remove(tmpList.indexOf(item));
                            break;
                        }
                    }
                }
            }
            System.out.println("numofs: " + numberOfShuffles);
            System.out.println(tmpList);
        }

        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[0].length; j++) {
                this.instance.getStacks()[i][j] = tmpCurrentStacks[i][j];
            }
        }
    }

    public Solution capThreeApproach() {

        // TODO:
        //   - calc MCM between for b = 2
        //   - interpret MCM edges as stack assignments
        //   - iterate over remaining items and assign to feasible stack

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

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

        // no getUnmatchedItems()
        // --> implement function that retrieves all items from the matching as int
        // --> find all items from getItems() that are not part of the list

        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        // System.out.println(mcm.getMatching());

        //////////////////////
        ArrayList<ArrayList<Integer>> matchedItemsOne = this.extractInitialStackAssignmentsFromMCM(mcm).get(0);
        ArrayList<ArrayList<Integer>> matchedItemsTwo = this.extractInitialStackAssignmentsFromMCM(mcm).get(1);

        ArrayList<Integer> unmatchedItemsOne = this.getUnmatchedItems(matchedItemsOne);
        ArrayList<Integer> unmatchedItemsTwo = this.getUnmatchedItems(matchedItemsTwo);

        System.out.println(mcm.getMatching());

        System.out.println("unmatched: " + unmatchedItemsOne);

        this.setStacks(matchedItemsOne, unmatchedItemsOne);

        Solution sol = new Solution(0, 0, this.instance.getStacks(), "test", false, this.instance.getItems().length);

        if (!sol.isFeasible()) {
            this.instance.resetStacks();
            this.setStacks(matchedItemsTwo, unmatchedItemsTwo);
            sol = new Solution(0, 0, this.instance.getStacks(), "test", false, this.instance.getItems().length);
        }

        return sol;
    }

    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            sol = this.capThreeApproach();
        }

        return sol;
    }

}
