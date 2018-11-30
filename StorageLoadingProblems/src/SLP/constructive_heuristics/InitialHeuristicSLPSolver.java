package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;

public class InitialHeuristicSLPSolver {

    private Instance instance;

    public InitialHeuristicSLPSolver(Instance instance) {
        this.instance = instance;
    }

    public ArrayList<Integer> getUnmatchedItems(EdmondsMaximumCardinalityMatching mcm) {

        ArrayList<Integer> unmatchedItems = new ArrayList<>();
        ArrayList<Integer> matchedItems = new ArrayList<>();

        int maximumNumberOfPairs = this.instance.getStacks().length;
        int cnt = 0;

        for (Object edge : mcm.getMatching()) {
            if (cnt < maximumNumberOfPairs) {
                int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
                int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
                matchedItems.add(vertexOne);
                matchedItems.add(vertexTwo);
            }
            cnt++;
        }

        for (int item : this.instance.getItems()) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }

        return unmatchedItems;
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

        ArrayList<Integer> unmatchedItems = this.getUnmatchedItems(mcm);

        System.out.println(mcm.getMatching());

        int cnt = 0;
        // Sets MCM pairs to level 0 and 1 of stacks
        for (DefaultEdge edge : mcm.getMatching()) {
            if (cnt < this.instance.getStacks().length) {
                int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
                int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
                this.instance.getStacks()[cnt][0] = vertexOne;
                this.instance.getStacks()[cnt][1] = vertexTwo;
            }
            cnt++;
        }

        System.out.println("nomatch: " + unmatchedItems);

        // Could be extended to try several possible allocations
        for (int item : unmatchedItems) {
            for (int i = 0; i < this.instance.getStacks().length; i++) {
                int currentTopMostItem = this.instance.getStacks()[i][1];
                if (this.instance.getStackingConstraints()[item][currentTopMostItem] == 1) {
                    this.instance.getStacks()[i][2] = item;
                    break;
                }
            }
        }

        return new Solution(0, 0, this.instance.getStacks(), "test", false);
    }

    public Solution solve() {

        Solution sol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            sol = this.capThreeApproach();
        }

        return sol;
    }

}
