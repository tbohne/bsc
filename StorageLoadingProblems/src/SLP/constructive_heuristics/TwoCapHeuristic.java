package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.MCMEdge;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;

public class TwoCapHeuristic {

    private Instance instance;
    private double startTime;

    public TwoCapHeuristic(Instance instance) {
        this.instance = instance;
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

                            if (!graph.containsEdge("stack" + j, "edge" + itemPairs.get(i))
                                    && !graph.containsEdge("edge" + itemPairs.get(i), "stack" + j)) {
                                        graph.addEdge("edge" + itemPairs.get(i), "stack" + j);
                            }

                }
            }
        }

        EdmondsMaximumCardinalityMatching<String, DefaultEdge> finalMCM = new EdmondsMaximumCardinalityMatching<>(graph);
        System.out.println("items assigned: " +  finalMCM.getMatching().getEdges().size() * 2);
        System.out.println("free stacks: " + (this.instance.getStacks().length - finalMCM.getMatching().getEdges().size()));

        return finalMCM;
    }

    public HashMap parseItemPairStackCombination(Object edge) {
        int firstItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[0].trim());
        int secondItem = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[0].replace(")", "").trim());
        int stack = Integer.parseInt(edge.toString().replace("(edge(", "").split(",")[1].split(":")[1].replace("stack", "").replace(")", "").trim());

        HashMap<Integer, ArrayList<Integer>> itemPairStackCombination = new HashMap<>();
        ArrayList<Integer> items = new ArrayList<>();
        items.add(firstItem);
        items.add(secondItem);
        itemPairStackCombination.put(stack, items);

        return itemPairStackCombination;
    }

    public Solution firstApproach(EdmondsMaximumCardinalityMatching mcm, boolean optimizeSolution) {

        ArrayList<MCMEdge> itemPairs = new ArrayList<>();
        HeuristicUtil.parseItemPairMCM(itemPairs, mcm);
        EdmondsMaximumCardinalityMatching finalMCM = this.getMatchingBetweenItemPairsAndStacks(itemPairs);

        for (Object edge : finalMCM.getMatching().getEdges()) {
            HashMap itemPairStackCombination = this.parseItemPairStackCombination(edge);
            System.out.println(itemPairStackCombination);
        }

        Solution sol = new Solution(0, false, this.instance);

        return sol;
    }

    /**
     *
     * @param optimizeSolution - specifies whether the solution should be optimized or just valid
     * @return
     */
    public Solution solve(boolean optimizeSolution) {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 2) {

            this.startTime = System.currentTimeMillis();
            DefaultUndirectedGraph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);

            HeuristicUtil.generateStackingConstraintGraph(graph, this.instance.getItems(), this.instance.getStackingConstraints());
            EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);

            sol = firstApproach(mcm, optimizeSolution);
            sol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        } else {
            System.out.println("This heuristic is designed to solve SLP with a stack capacity of 2.");
        }
        return sol;
    }
}
