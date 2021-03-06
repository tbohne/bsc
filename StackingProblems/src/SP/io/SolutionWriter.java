package SP.io;

import SP.representations.OptimizableSolution;
import SP.representations.Solution;

import java.io.*;

/**
 * Provides functionalities to write solutions of stacking problems to the file system.
 *
 * @author Tim Bohne
 */
public class SolutionWriter {

    /**
     * Writes the specified solution as CSV to the specified file.
     *
     * @param filename       - name of the file to write to
     * @param lowerBound     - LB to be written to the file
     * @param nameOfInstance - name of the instance the LB was computed for
     */
    public static void writeLowerBoundAsCSV(String filename, double lowerBound, String nameOfInstance) {
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
            bw.write(nameOfInstance.replace("instances/slp_instance_", "")
                + "," + "LB" + "," + "-" + "," + lowerBound + "\n");

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the optimal solution and the improved solution generated by the
     * post optimization method as CSV to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - original solution
     * @param impSol   - improved solution (post optimization result)
     */
    public static void writeOptAndImpAsCSV(String filename, OptimizableSolution sol, Solution impSol) {
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

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + "OPT" + "," + sol.getRuntimeForOptimalSolution() + "," + sol.getOptimalObjectiveValue() + "\n");

            String solver = sol.getSol().getFilledStacks()[0].length == 2 ? "2Cap + TS" : "3Cap + TS";
            double totalRuntime = (double)Math.round((sol.getSol().getTimeToSolveAsDouble() + impSol.getTimeToSolveAsDouble()) * 100) / 100;

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + solver + "," + totalRuntime + "," + impSol.computeCosts() + "\n");

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified solution as CSV to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - solution to be written to the file
     * @param solver   - solver used to create the solution
     */
    public static void writeSolutionAsCSV(String filename, Solution sol, String solver) {
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
            if (sol.isFeasible()) {
                bw.write(
                    sol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                    + "," + solver + "," + sol.getTimeToSolve() + "," + sol.computeCosts() + "\n"
                );
            }

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified LB to the specified file.
     *
     * @param filename   - name of the file to write to
     * @param lowerBound - lower bound to be written to the file
     */
    public static void writeLowerBound(String filename, double lowerBound) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("lower bound for relaxed s_ij: " + lowerBound + "\n");

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified solution to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - solution to be written to the file
     * @param solver   - solver used to create the solution
     */
    public static void writeSolution(String filename, Solution sol, String solver) {
        try {
            File file = new File(filename);
            boolean appendNewLines = true;
            if (!file.exists()) {
                file.createNewFile();
                appendNewLines = false;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            if (appendNewLines) {
                bw.newLine();
                bw.write("#####################################################\n");
                bw.newLine();
            }
            bw.write("solved with: " + solver + "\n");
            bw.write(sol.toString());

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
