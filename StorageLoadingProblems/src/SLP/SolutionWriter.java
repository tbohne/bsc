package SLP;

import java.io.*;

import static SLP.SolverComparison.*;

public class SolutionWriter {

    public static String getNameOfMipFormulation(Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return "BinPacking Formulation";
            case MIP_THREEINDEX:
                return "ThreeIndex Formulation";
            case CONSTRUCTIVE_HEURISTIC:
                return "Constructive Heuristic";
            default:
                return "";
        }
    }

    public static String getNameOfMipFormulationCSV(Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return "BinP";
            case MIP_THREEINDEX:
                return "3Idx";
            case CONSTRUCTIVE_HEURISTIC:
                return "ConstHeu";
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

            String mip = getNameOfMipFormulationCSV(solver);
            if (!sol.isEmpty()) {
                bw.write(sol.getNameOfSolvedInstance().replace("instances/slp_instance_", "") + "," + mip + "," + sol.getTimeToSolve() + "," + sol.getObjectiveValue() + "\n");
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

            String mip = getNameOfMipFormulation(solver);

            if (appendNewLines) {
                bw.newLine();
                bw.write("#####################################################\n");
                bw.newLine();
            }

            bw.write("solved with: " + mip + "\n");
            bw.write(sol.toString());

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
