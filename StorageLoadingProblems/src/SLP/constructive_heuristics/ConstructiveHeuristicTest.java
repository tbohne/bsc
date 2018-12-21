package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.InstanceReader;
import SLP.Solution;

public class ConstructiveHeuristicTest {

    // TODO: Should be removed later (for now used as test class)

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results

        Instance instance = InstanceReader.readInstance("res/instances/v6/slp_instance_300_120_3_90.txt");
        // System.out.println(instance);

        ThreeCapPermutationHeuristic solver1 = new ThreeCapPermutationHeuristic(instance);
        ThreeCapRecursiveMCMHeuristic solver2 = new ThreeCapRecursiveMCMHeuristic(instance);

//        Solution sol1 = solver1.solve(false);
//        instance.resetStacks();
        Solution sol2 = solver2.solve(false);

//        System.out.println("feasible: " + sol1.isFeasible());
//        System.out.println("cost: " + sol1.getCost());
        System.out.println("cost: " + sol2.getCost());
    }
}
