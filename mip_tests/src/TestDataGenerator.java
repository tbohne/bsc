public class TestDataGenerator {

    public static void main(String[] args) {

        int numOfItems = 100;
        int numOfStacks = 16;
        int stackCap = 16;

        int[][] matrix = TestDataGenerator.generateStackingConstraintMatrix(numOfItems, numOfItems, true);
        int[][] costs = new int[numOfItems][numOfStacks];
        for (int i = 0; i < numOfItems; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                costs[i][j] = (int)Math.round(Math.random());
            }
        }

        Instance instance = new Instance(numOfItems, numOfStacks, stackCap, matrix, costs, "slp_instance_generated_3");
        Writer.writeInstance("res/slp_instance_generated_3.txt", instance);
    }

    public static int[][] generateStackingConstraintMatrix(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                } else {
                    if (Math.random() >= 0.98) {
                        matrix[i][j] = 1;
                    } else {
                        matrix[i][j] = 0;
                    }
                }
            }
        }

        if (transitiveStackingConstraints) {
            matrix = makeMatrixTransitive(matrix);
        }
        return matrix;
    }

    public static int[][] makeMatrixTransitive(int[][] matrix) {

        // The ones that are added can then induce new ones in other rows,
        // therefore we have to repeat the process as long as there are changes.

        boolean somethingChanged = true;

        while (somethingChanged) {
            somethingChanged = false;
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    // we only have to add the ones that follow from transitivity
                    if (i != j && matrix[i][j] == 0) {
                        for (int k = 0; k < matrix[0].length; k++) {
                            if (matrix[i][k] == 1 && i != k && j != k) {
                                if (matrix[k][j] == 1) {
                                    matrix[i][j] = 1;
                                    somethingChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return matrix;
    }
}
