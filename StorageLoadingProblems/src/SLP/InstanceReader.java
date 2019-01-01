package SLP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class InstanceReader {

    private static int[][] readMatrix(BufferedReader reader, int numberOfItems) {

        int[][] matrix = new int[numberOfItems][];

        try {
            String line = reader.readLine().trim();

            int idx = 0;

            while (!line.equals("")) {

                String[] integerStrings = line.split(" ");
                matrix[idx] = new int[integerStrings.length];

                for (int i = 0; i < integerStrings.length; i++) {
                    matrix[idx][i] = Integer.parseInt(integerStrings[i]);
                }

                line = reader.readLine();
                line = line == null ? "" : line.trim();
                idx++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matrix;
    }

    public static Instance readInstance(String filename) {

        int numberOfItems = 0;
        int numberOfStacks = 0;
        int stackCapacity = 0;
        int[][] stackingConstraints = new int[numberOfItems][];
        int[][] stackConstraints = new int[numberOfItems][];
        int[][] costs = new int [numberOfItems][];

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            numberOfItems = Integer.parseInt(reader.readLine().trim());
            numberOfStacks = Integer.parseInt(reader.readLine().trim());
            stackCapacity = Integer.parseInt(reader.readLine().trim());

            if (reader.readLine().trim().equals("")) {
                stackingConstraints = readMatrix(reader, numberOfItems);
            }

            if (reader.readLine().trim().equals("")) {
                stackConstraints = readMatrix(reader, numberOfItems);
            }

            costs = readMatrix(reader, numberOfItems);

        } catch (IOException e) {
            e.printStackTrace();
        }

        String instancename = filename.replace("res/", "").replace(".txt", "");
        return new Instance(numberOfItems, numberOfStacks, stackCapacity, stackingConstraints, stackConstraints, costs, instancename);
    }
}
