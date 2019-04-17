package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.io.InstanceReader;
import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;

public class TabuSearchTest {

    public static void main(String[] args) {


        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_100_60_2_00.txt");

        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir("res/solutions/", "res/instances/");
        Solution sol = solutions.get(0);

        System.out.println("expected instance name: " + instance.getName());
        System.out.println("actual instance name  : " + sol.getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(instance, sol);

        System.out.println("COSTS BEFORE: " + sol.computeCosts());
        sol = ts.solve(instance);
        System.out.println("FINAL COSTS: " + sol.computeCosts());
    }
}
