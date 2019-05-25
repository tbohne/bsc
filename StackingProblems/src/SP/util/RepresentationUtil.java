package SP.util;

/**
 * A collection of general utility methods used in the representations.
 *
 * @author Tim Bohne
 */
public class RepresentationUtil {

    /**
     * Helper method used in toString().
     *
     * @param numberOfStacks - number of available stacks
     * @return maximum string offset
     */
    public static int getMaximumStringOffset(int numberOfStacks) {
        int cnt = 0;
        while ((numberOfStacks / 10) > 0) {
            numberOfStacks /= 10;
            cnt++;
        }
        return cnt;
    }

    /**
     * Helper method used in toString().
     *
     * @param idx       - index of the stack
     * @param maxOffset - maximum space offset
     * @return space used for the current stack visualization
     */
    public static String getCurrentSpace(int idx, int maxOffset) {
        StringBuilder space = new StringBuilder();
        String idxStr = Integer.toString(idx);
        for (int i = 0; i < maxOffset - idxStr.length() - 1; i++) {
            space.append(" ");
        }
        return space.toString();
    }

    /**
     * Returns the corresponding integer matrix for the given double matrix.
     *
     * @param matrix - matrix whose entries are going to be casted
     * @return corresponding integer matrix
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
}
