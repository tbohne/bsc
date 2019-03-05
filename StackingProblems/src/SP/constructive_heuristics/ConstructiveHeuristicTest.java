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

    public static void main (String[] args) {

        Instance instance = InstanceReader.readInstance("res/instances/b=3_m/slp_instance_300_120_3_03.txt");
        System.out.println("working on: " + instance.getName());

        ThreeCapHeuristic solver = new ThreeCapHeuristic(instance, TIME_LIMIT);

        Solution sol = solver.solve();
        System.out.println("feasible: " + sol.isFeasible());
        System.out.println("cost: " + sol.computeCosts());
    }
}
