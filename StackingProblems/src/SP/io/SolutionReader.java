package SP.io;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.io.InstanceReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Provides functionalities to read solutions for stacking problems from the file system.
 *
 * @author Tim Bohne
 */
public class SolutionReader {

    /**
     * Reads the storage area from the file.
     *
     * @param br          - the buffered reader pointing to the specific line
     * @param storageArea - the storage area to be filled
     * @throws IOException
     */
    public static void readStorageArea(BufferedReader br, ArrayList<ArrayList<Integer>> storageArea) throws IOException {
        for (String line; (line = br.readLine()) != null; ) {
            ArrayList<Integer> stack = new ArrayList<>();
            String[] splitArr = line.split(":");
            if (splitArr.length > 1) {
                String first = splitArr[1].trim();
                if (first.length() > 0) {
                    int[] array = Arrays.stream(first.split("\\s")).mapToInt(Integer::parseInt).toArray();
                    for (int item : array) {
                        stack.add(item);
                    }
                }
                storageArea.add(new ArrayList<>(stack));
            } else {
                storageArea.add(new ArrayList<>());
            }
        }
    }

    /**
     * Copies the read storage area to the solution's filled storage area.
     * TODO: not the best way to do that
     *
     * @param storageArea - the storage area to be copied
     * @param sol         - the solution whose storage area gets filled
     */
    public static void fillStorageArea(ArrayList<ArrayList<Integer>> storageArea, Solution sol) {
        for (int i = 0; i < storageArea.size(); i++) {
            for (int j = 0; j < storageArea.get(i).size(); j++) {
                sol.getFilledStorageArea()[i][j] = storageArea.get(i).get(j);
            }
        }
    }

    /**
     * Reads the solution of a stacking-problem from the specified file.
     *
     * @param file            - the file to read from
     * @param instanceDirName - the name of the instance directory
     * @param solver          - specifies the solver for which the solution is to be read
     * @return the read solution
     */
    public static Solution readSolution(File file, String instanceDirName, String solver) {

        Solution sol = new Solution();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                // then the next lines should be used to read the solution
                if (line.contains("solved with: SP.constructive_heuristics." + solver)) {
                    break;
                }
            }

            String line = br.readLine();
            int timeLimit = Integer.parseInt(line.split(":")[1].replace("s", "").trim());
            line = br.readLine();
            double timeToSolveInSeconds = Double.parseDouble(line.split(":")[1].replace(",", ".").replace("s", "").trim());
            line = br.readLine();
            double objectiveValue = Double.parseDouble(line.split(":")[1].trim());
            br.readLine();
            br.readLine();

            ArrayList<ArrayList<Integer>> storageArea = new ArrayList<>();
            readStorageArea(br, storageArea);
            String instanceName = file.getName().replace("sol", "instance");
            Instance instance = InstanceReader.readInstance(instanceDirName + instanceName);
            sol = new Solution(timeToSolveInSeconds, objectiveValue, timeLimit, instance);
            fillStorageArea(storageArea, sol);

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        sol.lowerItemsThatAreStackedInTheAir();
        return sol;
    }

    /**
     * Reads the solutions from the files contained in the specified directory.
     *
     * @param solutionDirName - name of the directory containing the solutions
     * @param instanceDirName - name of the directory containing the solved instances
     * @param solver          - specifies the solver for which the solution is to be read
     * @return a list of the read solutions
     */
    public static ArrayList<Solution> readSolutionsFromDir(String solutionDirName, String instanceDirName, String solver) {

        File dir = new File(solutionDirName);
        ArrayList<Solution> solutions = new ArrayList<>();

        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);

        for (File file : directoryListing) {
            if (!file.isDirectory() && file.getName().contains("slp_")) {
                solutions.add(readSolution(file, instanceDirName, solver));
                break;
            }
        }
        return solutions;
    }
}
