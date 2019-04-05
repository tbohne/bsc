package SP.io;

import SP.representations.Instance;
import SP.representations.Position;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Provides functionalities to read instances of stacking problems from the file system.
 *
 * @author Tim Bohne
 */
public class InstanceReader {

    /**
     * Reads matrices (stacking constraints, costs, ...) from an instance file using the specified reader.
     *
     * @param reader        - the buffered reader pointing to the part of the file containing the matrix to be read
     * @param numberOfItems - the number of items that are part of the instance
     * @return the read matrix
     */
    private static double[][] readMatrix(BufferedReader reader, int numberOfItems) {

        double[][] matrix = new double[numberOfItems][];

        try {
            String line = reader.readLine().trim();
            int row = 0;

            // Reading as long as there are lines belonging to the matrix.
            while (!line.equals("")) {
                String[] stringOfIntegers = line.split(" ");
                matrix[row] = new double[stringOfIntegers.length];
                for (int col = 0; col < stringOfIntegers.length; col++) {
                    matrix[row][col] = Double.parseDouble(stringOfIntegers[col]);
                }
                line = reader.readLine();
                line = line == null ? "" : line.trim();
                row++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matrix;
    }

    /**
     * Reads positions (item pos / stack pos) from an instance file using the specified reader.
     *
     * @param positions - the list of positions to be filled
     * @param reader    - the reader pointing to the part of the file containing the positions
     */
    private static void readPositions(ArrayList<Position> positions, BufferedReader reader) {
        try {
            String line = reader.readLine().trim();
            String[] stringOfPositions = line.split(" ");
            for (String position : stringOfPositions) {
                double xCoord = Double.parseDouble(position.split(",")[0].replace("(", "").trim());
                double yCoord = Double.parseDouble(position.split(",")[1].replace(")", "").trim());
                positions.add(new Position(xCoord, yCoord));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the corresponding integer matrix for the given double matrix.
     *
     * @param matrix - the matrix whose entries are going to be casted
     * @return the corresponding integer matrix
     */
    public static int[][] castFloatingPointMatrix(double[][] matrix) {
        int[][] integerMatrix = new int[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                integerMatrix[i][j] = (int)matrix[i][j];
            }
        }
        return integerMatrix;
    }

    /**
     * Reads the instance from the specified file.
     *
     * @param filename - the name of the file to be read from
     * @return the read instance of a stacking problem
     */
    public static Instance readInstance(String filename) {
        int numberOfItems = 0;
        int numberOfStacks = 0;
        int stackCapacity = 0;
        ArrayList<Position> itemPositions = new ArrayList<>();
        ArrayList<Position> stackPositions = new ArrayList<>();
        int[][] stackingConstraints = new int[numberOfItems][];
        double[][] costs = new double[numberOfItems][];

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            numberOfItems = Integer.parseInt(reader.readLine().trim());
            numberOfStacks = Integer.parseInt(reader.readLine().trim());
            stackCapacity = Integer.parseInt(reader.readLine().trim());

            if (reader.readLine().trim().equals("")) {
                stackingConstraints = castFloatingPointMatrix(readMatrix(reader, numberOfItems));
            }
            costs = readMatrix(reader, numberOfItems);
            readPositions(itemPositions, reader);
            readPositions(stackPositions, reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String instanceName = filename.replace("res/", "").replace(".txt", "");
        return new Instance(
            numberOfItems, numberOfStacks, itemPositions, stackPositions,
            stackCapacity, stackingConstraints, costs, instanceName
        );
    }
}
