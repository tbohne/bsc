package SP.representations;

/**
 * Represents positions for stacks and items in a grid layout.
 *
 * @author Tim Bohne
 */
public class Position {

    private final double xCoord;
    private final double yCoord;

    /**
     * Constructor
     *
     * @param xCoord - x coordinate of position
     * @param yCoord - y coordinate of position
     */
    public Position(double xCoord, double yCoord) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    /**
     * Copy-Constructor
     *
     * @param pos - position to be copied
     */
    public Position(Position pos) {
        this.xCoord = pos.getXCoord();
        this.yCoord = pos.getYCoord();
    }

    /**
     * Returns the position's x coordinate.
     *
     * @return x coordinate of position
     */
    public double getXCoord() {
        return this.xCoord;
    }

    /**
     * Returns the position's y coordinate
     *
     * @return y coordinate of position
     */
    public double getYCoord() {
        return this.yCoord;
    }

    /**
     * Provides an appropriate visualization for a position.
     *
     * @return string visualizing the position
     */
    @Override
    public String toString() {
        return "(" + String.format("%.2f", this.xCoord) + "," + String.format("%.2f", this.yCoord) + ")";
    }
}
