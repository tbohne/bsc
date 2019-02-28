package SP.representations;

import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.constructive_heuristics.ThreeCapPermutationHeuristic;
import SP.constructive_heuristics.ThreeCapRecursiveMCMHeuristic;
import SP.constructive_heuristics.TwoCapHeuristic;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;

import java.io.*;

import static SP.experiments.SolverComparison3Cap.*;

public class SolutionWriter {

    public static String getNameOfSolver(Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return BinPackingFormulation.class.getName();
            case MIP_THREEINDEX:
                return ThreeIndexFormulation.class.getName();
            case CONSTRUCTIVE_TWO_CAP:
                return TwoCapHeuristic.class.getName();
            case CONSTRUCTIVE_THREE_CAP:
                return ThreeCapHeuristic.class.getName();
            case CONSTRUCTIVE_THREE_CAP_PERMUTATION:
                return ThreeCapPermutationHeuristic.class.getName();
            case CONSTRUCTIVE_THREE_CAP_RECURSION:
                return ThreeCapRecursiveMCMHeuristic.class.getName();
            default:
                return "";
        }
    }

    public static String getNameOfSolverCSV(Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return "BinP";
            case MIP_THREEINDEX:
                return "3Idx";
            case CONSTRUCTIVE_TWO_CAP:
                return "2Cap";
            case CONSTRUCTIVE_THREE_CAP:
                return "3Cap";
            case CONSTRUCTIVE_THREE_CAP_PERMUTATION:
                return "3CapPerm";
            case CONSTRUCTIVE_THREE_CAP_RECURSION:
                return "3CapRec";
            default:
                return "";
        }
    }

    public static void writeSolutionAsCSV(String filename, Solution sol, Solver solver) {
        try {

            File file = new File(filename);
            boolean newFile = false;

            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            if (newFile) {
                bw.write("instance,solver,time,val\n");
            }

            String mip = getNameOfSolverCSV(solver);
            if (sol.isFeasible()) {
                bw.write(sol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                    + "," + mip + "," + sol.getTimeToSolve() + "," + sol.getObjectiveValue() + "\n"
                );
            }
            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeSolution(String filename, Solution sol, Solver solver) {

        try {
            File file = new File(filename);

            boolean appendNewLines = true;

            if (!file.exists()) {
                file.createNewFile();
                appendNewLines = false;
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            String solverName = getNameOfSolver(solver);

            if (appendNewLines) {
                bw.newLine();
                bw.write("#####################################################\n");
                bw.newLine();
            }

            bw.write("solved with: " + solverName + "\n");
            bw.write(sol.toString());

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
