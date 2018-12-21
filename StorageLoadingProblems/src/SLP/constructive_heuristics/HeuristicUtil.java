package SLP.constructive_heuristics;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class HeuristicUtil {

    public static void generateStackingConstraintGraph(
            DefaultUndirectedGraph<String, DefaultEdge> graph,
            int[] items,
            int[][] stackingConstraints
    ) {

        for (int item : items) {
            graph.addVertex("v" + item);
        }
        // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1.
        for (int i = 0; i < stackingConstraints.length; i++) {
            for (int j = 0; j < stackingConstraints[0].length; j++) {
                if (i != j && stackingConstraints[i][j] == 1 ||stackingConstraints[j][i] == 1) {
                    if (!graph.containsEdge("v" + j, "v" + i)) {
                        graph.addEdge("v" + i, "v" + j);
                    }
                }
            }
        }
    }

    public static int computeRowRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int entry : stackingConstraints[item]) {
            rating += entry;
        }
        return rating;
    }

    public int computeColRatingForUnmatchedItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int i = 0; i < stackingConstraints.length; i++) {
            rating += stackingConstraints[i][item];
        }
        return rating;
    }
}
