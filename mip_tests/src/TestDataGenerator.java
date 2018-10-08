public class TestDataGenerator {

    public static void main(String[] args) {

        int numOfItems = 10;
        int numOfStacks = 5;
        int stackCap = 3;

        int[][] matrix = TestDataGenerator.generateStackingConstraintMatrix(numOfItems, numOfItems, true);

        int[][] costs = new int[numOfItems][numOfStacks];
        for (int i = 0; i < numOfItems; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                costs[i][j] = (int)Math.round(Math.random());
            }
        }

        Instance instance = new Instance(numOfItems, numOfStacks, stackCap, matrix, costs);
        Writer.writeInstance("res/slp_instance_generated_1.txt", instance);
    }

    public static int[][] generateStackingConstraintMatrix(int dimOne, int dimTwo, boolean transitiveStackingConstraints) {

        int[][] matrix = new int[dimOne][dimTwo];

        for (int i = 0; i < dimOne; i++) {
            for (int j = 0; j < dimTwo; j++) {

                if (i == j) {
                    matrix[i][j] = 1;
                } else {
                    // Sets the entry to 1 in 10% of the cases.
                    if (Math.random() >= 0.90) {
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

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                // we only have to add the ones that follow from transitivity
                if (i != j && matrix[i][j] == 0) {
                    int saved = j;
                    for (int k = 0; k < matrix[0].length; k++) {
                        if (matrix[i][k] == 1 && i != k && j != k) {
                            if (matrix[k][saved] == 1) {
                                matrix[i][j] = 1;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return matrix;
    }

}
