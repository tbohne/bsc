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
    public static final boolean PRIORITZIE_RUNTIME = false;

    public static void main (String[] args) {

        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_7_5_2_00.txt");
        System.out.println("working on: " + instance.getName());

//        ThreeCapHeuristic solver = new ThreeCapHeuristic(instance, TIME_LIMIT);
//        Solution sol = solver.solve(PRIORITZIE_RUNTIME,  POST_PROCESSING);

        TwoCapHeuristic solver = new TwoCapHeuristic(instance, TIME_LIMIT);
        Solution sol = solver.solve(POST_PROCESSING);

//        System.out.println("feasible: " + sol.isFeasible());
//        System.out.println("cost: " + sol.computeCosts());
//        System.out.println("time: " + sol.getTimeToSolve());
    }
}
