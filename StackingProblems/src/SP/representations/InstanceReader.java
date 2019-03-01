package SP.representations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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

    public static void readCoordinates(ArrayList<Position> coordinates, BufferedReader reader) {
        try {
            String line = reader.readLine().trim();
            String[] coords = line.split(" ");
            for (String coord : coords) {
                int xCoord = Integer.parseInt(coord.split(",")[0].replace("(", "").trim());
                int yCoord = Integer.parseInt(coord.split(",")[1].replace(")", "").trim());
                coordinates.add(new Position(xCoord, yCoord));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Instance readInstance(String filename) {

        int numberOfItems = 0;
        int numberOfStacks = 0;
        int stackCapacity = 0;

        ArrayList<Position> itemPositions = new ArrayList<>();
        ArrayList<Position> stackPositions = new ArrayList<>();

        int[][] stackingConstraints = new int[numberOfItems][];
        int[][] costs = new int [numberOfItems][];

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            numberOfItems = Integer.parseInt(reader.readLine().trim());
            numberOfStacks = Integer.parseInt(reader.readLine().trim());
            stackCapacity = Integer.parseInt(reader.readLine().trim());

            if (reader.readLine().trim().equals("")) {
                stackingConstraints = readMatrix(reader, numberOfItems);
            }

            costs = readMatrix(reader, numberOfItems);

            readCoordinates(itemPositions, reader);
            readCoordinates(stackPositions, reader);

        } catch (IOException e) {
            e.printStackTrace();
        }

        String instancename = filename.replace("res/", "").replace(".txt", "");
        return new Instance(numberOfItems, numberOfStacks, itemPositions, stackPositions, stackCapacity, stackingConstraints, costs, instancename);
    }
}
