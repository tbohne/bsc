package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;

public class TwoCapHeuristic {

    private Instance instance;
    private double startTime;
    private int timeLimit;

    public TwoCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    public EdmondsMaximumCardinalityMatching getMatchingBetweenItemPairsAndStacks(ArrayList<MCMEdge> itemPairs) {

        DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        for (MCMEdge e : itemPairs) {
            graph.addVertex("edge" + e);
        }
        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            graph.addVertex("stack" + stack);
        }

        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                if (this.instance.getStackConstraints()[itemPairs.get(i).getVertexOne()][j] == 1
                    && this.instance.getStackConstraints()[itemPairs.get(i).getVertexTwo()][j] == 1) {

                        if (!graph.containsEdge("edge" + itemPairs.get(i), "stack" + j)) {
                            graph.addEdge("edge" + itemPairs.get(i), "stack" + j);
                        }
                }
            }
        }
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemPairStackMatching = new EdmondsMaximumCardinalityMatching<>(graph);
        System.out.println("items assigned: " +  itemPairStackMatching.getMatching().getEdges().size() * 2);
        System.out.println("free stacks: " + (this.instance.getStacks().length - itemPairStackMatching.getMatching().getEdges().size()));

        return itemPairStackMatching;
    }

    public void fixOrderInStacks() {
        for (int[] stack : this.instance.getStacks()) {
            if (stack[0] != -1 && stack[1] != -1) {
                if (this.instance.getStackingConstraints()[stack[0]][stack[1]] != 1) {
                    int tmp = stack[0];
                    stack[0] = stack[1];
                    stack[1] = tmp;
                }
            }
        }
    }

    public ArrayList<Integer> getUnmatchedItems() {

        ArrayList<Integer> matchedItems = new ArrayList<>();
        for (int[] stack : this.instance.getStacks()) {
            for (int item : stack) {
                if (item != -1) {
                    matchedItems.add(item);
                }
            }
        }

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }

        return unmatchedItems;
    }

    public Solution firstApproach(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        EdmondsMaximumCardinalityMatching matchingBetweenItemPairsAndStacks = this.getMatchingBetweenItemPairsAndStacks(itemPairs);
        HashMap itemPairStackCombinations = HeuristicUtil.parseItemPairStackCombination(matchingBetweenItemPairsAndStacks);

        for (Object key : itemPairStackCombinations.keySet()) {
            this.instance.getStacks()[(int)key][0] = ((ArrayList<Integer>)itemPairStackCombinations.get(key)).get(0);
            this.instance.getStacks()[(int)key][1] = ((ArrayList<Integer>)itemPairStackCombinations.get(key)).get(1);
        }

        // TODO: improve cost minimization
        // First naive approach:
        // Assign each item to the cheapest remaining stack.
        for (int item : this.getUnmatchedItems()) {

            int idxOfCheapest = -1;
            int minCosts = Integer.MAX_VALUE;

            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                if (this.instance.getStacks()[stack][0] == -1 && this.instance.getStackConstraints()[item][stack] == 1) {
                    int costs = this.instance.getCosts()[item][stack];
                    if (costs < minCosts) {
                        minCosts = costs;
                        idxOfCheapest = stack;
                    }
                }
            }

            if (idxOfCheapest != -1) {
                this.instance.getStacks()[idxOfCheapest][0] = item;
            } else {
                return new Solution();
            }
        }

        this.fixOrderInStacks();
        Solution sol = new Solution(0, this.timeLimit, this.instance);

        return sol;
    }

    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 2) {

            this.startTime = System.currentTimeMillis();
            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            HeuristicUtil.generateStackingConstraintGraph(graph, this.instance.getItems(), this.instance.getStackingConstraints());
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(graph);
            sol = firstApproach(itemMatching);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 2.");
        }
        return sol;
    }
}
