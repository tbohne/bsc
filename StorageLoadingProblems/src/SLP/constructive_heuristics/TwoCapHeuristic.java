package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.MCMEdge;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

    public ArrayList<Integer> getUnmatchedItems(ArrayList<MCMEdge> itemPairs) {

        ArrayList<Integer> matchedItems = new ArrayList<>();

        for (MCMEdge edge : itemPairs) {
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

//    public ArrayList<Integer> getUnmatchedItems() {
//
//        ArrayList<Integer> matchedItems = new ArrayList<>();
//        for (int[] stack : this.instance.getStacks()) {
//            for (int item : stack) {
//                if (item != -1) {
//                    matchedItems.add(item);
//                }
//            }
//        }
//
//        ArrayList<Integer> unmatchedItems = new ArrayList<>();
//        for (int item : this.instance.getItems()) {
//            if (!matchedItems.contains(item)) {
//                unmatchedItems.add(item);
//            }
//        }
//
//        return unmatchedItems;
//    }

    public void generateGraph(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        for (MCMEdge e : itemPairs) {
            graph.addVertex("edge" + e);
            partitionOne.add("edge" + e);
        }

        for (int item : unmatchedItems) {
            graph.addVertex("item" + item);
            partitionOne.add("item" + item);
        }

        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            graph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }

        System.out.println("partOne: " + partitionOne.size());
        System.out.println("partTwo: " + partitionTwo.size());

        // item pair - stack edges
        for (int i = 0; i < itemPairs.size(); i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                if (this.instance.getStackConstraints()[itemPairs.get(i).getVertexOne()][j] == 1
                        && this.instance.getStackConstraints()[itemPairs.get(i).getVertexTwo()][j] == 1) {

                    if (!graph.containsEdge("edge" + itemPairs.get(i), "stack" + j)) {

                        DefaultWeightedEdge edge = graph.addEdge("edge" + itemPairs.get(i), "stack" + j);
                        int costs = this.instance.getCosts()[itemPairs.get(i).getVertexOne()][j] + this.instance.getCosts()[itemPairs.get(i).getVertexTwo()][j];
                        graph.setEdgeWeight(edge, costs);


                    }
                }
            }
        }

        // unmatched item - stack edges
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                if (this.instance.getStackConstraints()[item][stack] == 1) {
                    if (!graph.containsEdge("item" + item, "stack" + stack)) {
                        graph.addEdge("item" + item, "stack" + stack);
                        graph.setEdgeWeight(graph.getEdge("item" + item, "stack" + stack), this.instance.getCosts()[item][stack]);
                    }

                }
            }
        }

        System.out.println(graph);

//        KuhnMunkresMinimalWeightBipartitePerfectMatching mwbpm = new KuhnMunkresMinimalWeightBipartitePerfectMatching(graph,partitionOne, partitionTwo);
        MaximumWeightBipartiteMatching mwbm = new MaximumWeightBipartiteMatching(graph, partitionOne, partitionTwo);

        for (Object edge : mwbm.getMatching().getEdges()) {
            System.out.println(edge);
        }

    }

    public Solution firstApproach(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ArrayList<Integer> unmatchedItems = this.getUnmatchedItems(itemPairs);
        this.generateGraph(itemPairs, unmatchedItems);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////////

//        EdmondsMaximumCardinalityMatching matchingBetweenItemPairsAndStacks = this.getMatchingBetweenItemPairsAndStacks(itemPairs);
//        HashMap itemPairStackCombinations = HeuristicUtil.parseItemPairStackCombination(matchingBetweenItemPairsAndStacks);
//
//        for (Object key : itemPairStackCombinations.keySet()) {
//            this.instance.getStacks()[(int)key][0] = ((ArrayList<Integer>)itemPairStackCombinations.get(key)).get(0);
//            this.instance.getStacks()[(int)key][1] = ((ArrayList<Integer>)itemPairStackCombinations.get(key)).get(1);
//        }
//
//        // TODO: improve cost minimization
//        // First naive approach:
//        // Assign each item to the cheapest remaining stack.
//        for (int item : this.getUnmatchedItems()) {
//
//            int idxOfCheapest = -1;
//            int minCosts = Integer.MAX_VALUE;
//
//            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
//                if (this.instance.getStacks()[stack][0] == -1 && this.instance.getStackConstraints()[item][stack] == 1) {
//                    int costs = this.instance.getCosts()[item][stack];
//                    if (costs < minCosts) {
//                        minCosts = costs;
//                        idxOfCheapest = stack;
//                    }
//                }
//            }
//
//            if (idxOfCheapest != -1) {
//                this.instance.getStacks()[idxOfCheapest][0] = item;
//            } else {
//                return new Solution();
//            }
//        }

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
