package SLP;

import java.io.*;

import static SLP.SolverComparison.*;

public class SolutionWriter {

    public static String getNameOfMipFormulation(Formulation mipFormulation) {
        switch (mipFormulation) {
            case BINPACKING:
                return "BinPacking Formulation";
            case THREEINDEX:
                return "ThreeIndex Formulation";
            default:
                return "";
        }
    }

    public static String getNameOfMipFormulationCSV(Formulation mipFormulation) {
        switch (mipFormulation) {
            case BINPACKING:
                return "BinP";
            case THREEINDEX:
                return "3Idx";
            default:
                return "";
        }
    }

    public static void writeSolutionAsCSV(String filename, Solution sol, Formulation mipFormulation) {
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
                bw.write("instance,mip,time,val\n");
            }

            String mip = getNameOfMipFormulationCSV(mipFormulation);
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

    public static void writeSolution(String filename, Solution sol, Formulation mipFormulation) {

        try {

            File file = new File(filename);

            boolean appendNewLines = true;

            if (!file.exists()) {
                file.createNewFile();
                appendNewLines = false;
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            String mip = getNameOfMipFormulation(mipFormulation);

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
