package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.io.InstanceReader;
import SP.representations.Solution;

/**
 * Test class for constructive heuristics.
 *
 * @author Tim Bohne
 */
public class ConstructiveHeuristicTest {

    public static final int TIME_LIMIT = 300;
    public static final boolean POST_PROCESSING = true;

    public static void main (String[] args) {

        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_500_300_2_17.txt");
        System.out.println("working on: " + instance.getName());

        TwoCapHeuristic solver = new TwoCapHeuristic(instance, TIME_LIMIT);
        Solution sol = solver.solve(POST_PROCESSING);

        System.out.println("feasible: " + sol.isFeasible());
        System.out.println("cost: " + sol.computeCosts());
        System.out.println("time: " + sol.getTimeToSolve());
    }
}
