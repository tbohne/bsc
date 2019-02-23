package SLP.representations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SolutionReader {

    public static ArrayList<Solution> readSolutionsFromDir(String solutionDirName, String instanceDirName) {

        File dir = new File(solutionDirName);
        ArrayList<Solution> solutions = new ArrayList<>();

        int cnt = 0;

        for (File file : dir.listFiles()) {

            if (cnt > 0) { break; }

            if (!file.isDirectory()) {

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    for (String line; (line = br.readLine()) != null; ) {
                        // then the next lines should be used to read the solution
                        if (line.contains("solved with: SLP.constructive_heuristics.TwoCapHeuristic")) {
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

                    for (String l; (l = br.readLine()) != null; ) {
                        ArrayList<Integer> stack = new ArrayList<>();
                        String first = l.split(":")[1].trim();
                        if (first.length() > 0) {
                            int[] array = Arrays.stream(first.split("\\s")).mapToInt(Integer::parseInt).toArray();
                            for (int item : array) {
                                stack.add(item);
                            }
                        }
                        storageArea.add(new ArrayList<>(stack));
                    }

                    System.out.println(file.getName());
                    String instanceName = file.getName().replace("sol", "instance");

                    Instance instance = InstanceReader.readInstance(instanceDirName + instanceName);
                    Solution sol = new Solution(timeToSolveInSeconds, objectiveValue, timeLimit, instance);

                    for (int i = 0; i < storageArea.size(); i++) {
                        for (int j = 0; j < storageArea.get(i).size(); j++) {
                            sol.getFilledStorageArea()[i][j] = storageArea.get(i).get(j);
                        }
                    }

                    solutions.add(sol);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            cnt++;
        }
        return solutions;
    }

}
