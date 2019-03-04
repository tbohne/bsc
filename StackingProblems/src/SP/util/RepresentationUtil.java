package SP.util;

/**
 * A collection of general utility methods used in the representations.
 *
 * @author Tim Bohne
 */
public class RepresentationUtil {

    /**
     * Helper method used in toString of Solution.
     *
     * @param numberOfStacks - the number of available stacks
     * @return the maximum string offset
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
     * Helper method used in toString of Solution.
     *
     * @param idx       - the index of the stack
     * @param maxOffset - the maximum space offset
     * @return the space used for the current stack visualization
     */
    public static String getCurrentSpace(int idx, int maxOffset) {
        String space = "";
        if (idx < 10) {
            for (int i = 0; i < maxOffset; i++) {
                space += " ";
            }
            return space;
        } else if (idx < 100) {
            for (int i = 0; i < maxOffset - 1; i++) {
                space += " ";
            }
            return space;
        } else if (idx < 1000) {
            for (int i = 0; i < maxOffset - 2; i++) {
                space += " ";
            }
            return space;
        } else {
            for (int i = 0; i < maxOffset - 3; i++) {
                space += " ";
            }
            return space;
        }
    }
}
