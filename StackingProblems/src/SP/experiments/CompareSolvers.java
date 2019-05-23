package SP.experiments;

import java.util.ArrayList;

/**
 * Used to start configure / start the solver comparison.
 *
 * @author Tim Bohne
 */
public class CompareSolvers {

    public static void main(String[] args) {

        ArrayList<SolverComparison.Solver> solversToBeUsed = new ArrayList<>();
        solversToBeUsed.add(SolverComparison.Solver.MIP_BINPACKING);
        solversToBeUsed.add(SolverComparison.Solver.MIP_THREEINDEX);
//        solversToBeUsed.add(SolverComparison.Solver.CONSTRUCTIVE_TWO_CAP);
        solversToBeUsed.add(SolverComparison.Solver.CONSTRUCTIVE_THREE_CAP);

//         SolverComparison2Cap twoCap = new SolverComparison2Cap();
//         twoCap.compareSolvers(solversToBeUsed);

        SolverComparison3Cap threeCap = new SolverComparison3Cap();
       threeCap.compareSolvers(solversToBeUsed);
    }
}
