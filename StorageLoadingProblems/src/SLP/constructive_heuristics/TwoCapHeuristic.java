package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.Solution;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class TwoCapHeuristic {

    private Instance instance;
    private double startTime;

    public TwoCapHeuristic(Instance instance) {
        this.instance = instance;
    }

    public Solution firstApproach(EdmondsMaximumCardinalityMatching mcm, boolean optimizeSolution) {

        return new Solution();

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
