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

        Instance instance = InstanceReader.readInstance("res/instances/b=3_l/slp_instance_500_200_3_02.txt");
        System.out.println("working on: " + instance.getName());

        ThreeCapHeuristic solver = new ThreeCapHeuristic(instance, TIME_LIMIT);

        for (int i = 0; i < 5; i++) {
            Solution sol = solver.solve(true);
        }

//        System.out.println("feasible: " + sol.isFeasible());
//        System.out.println("cost: " + sol.computeCosts());
//        System.out.println("time: " + sol.getTimeToSolve());
    }
}
