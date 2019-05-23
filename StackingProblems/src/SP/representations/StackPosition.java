package SP.representations;

/**
 * Represents a position in the storage area.
 *
 * @author Tim Bohne
 */
public class StackPosition {

    private int stackIdx;
    private int level;

    /**
     * Constructor
     *
     * @param stackIdx - the index of the stack
     * @param level    - the level inside the stack
     */
    public StackPosition(int stackIdx, int level) {
        this.stackIdx = stackIdx;
        this.level = level;
    }

    /**
     * Returns the position's stack index.
     *
     * @return the position's stack index
     */
    public int getStackIdx() {
        return this.stackIdx;
    }

    /**
     * Returns the position's level inside the stack.
     *
     * @return the position's level inside the stack
     */
    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "(stack: " + this.stackIdx + ", level: " + this.level + ")";
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof StackPosition) {
            return this.level == ((StackPosition)object).level && this.stackIdx == ((StackPosition)object).stackIdx;
        }
        return false;
    }
}
