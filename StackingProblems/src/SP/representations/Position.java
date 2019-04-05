package SP.representations;

/**
 * Represents positions for stacks and items in a grid layout.
 *
 * @author Tim Bohne
 */
public class Position {

    private double xCoord;
    private double yCoord;

    /**
     * Constructor
     *
     * @param xCoord - the position's x coordinate
     * @param yCoord - the position's y corrdinate
     */
    public Position(double xCoord, double yCoord) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    /**
     * Copy-Constructor
     *
     * @param pos - the position to be copied
     */
    public Position(Position pos) {
        this.xCoord = pos.xCoord;
        this.yCoord = pos.yCoord;
    }

    /**
     * Returns the position's x coordinate.
     *
     * @return the position's x coordinate
     */
    public double getXCoord() {
        return this.xCoord;
    }

    /**
     * Returns the position's y coordinate
     *
     * @return the position's y coordinate
     */
    public double getYCoord() {
        return this.yCoord;
    }

    /**
     * Provides an appropriate visualization for a position.
     *
     * @return the string visualizing the position
     */
    @Override
    public String toString() {
        return "(" + String.format("%.2f", this.xCoord) + "," + String.format("%.2f", this.yCoord) + ")";
    }
}
