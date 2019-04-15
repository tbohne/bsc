package SP.representations;

/**
 * Represents an item that is going to be assigned to a stack position as part of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Item {

    private int idx;
    private float length;
    private float width;
    private Position pos;

    /**
     * Constructor
     *
     * @param idx    - the index that identifies the item
     * @param length - the length of the item
     * @param width  - the width of the item
     * @param pos    - the position of the item on the vehicle
     */
    public Item(int idx, float length, float width, Position pos) {
        this.idx = idx;
        this.length = length;
        this.width = width;
        this.pos = pos;
    }

    /**
     * Copy-Constructor
     *
     * @param item - the item to be copied
     */
    public Item(Item item) {
        this.idx = item.getIdx();
        this.length = item.getLength();
        this.width = item.getWidth();
        this.pos = new Position(item.getPosition());
    }

    /**
     * Returns the item's index.
     *
     * @return the item's index
     */
    public int getIdx() {
        return this.idx;
    }

    /**
     * Returns the item's length.
     *
     * @return the item's length
     */
    public float getLength() {
        return  this.length;
    }

    /**
     * Returns the item's width
     *
     * @return the item's width
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * Returns the item's position on the vehicle.
     *
     * @return the item's position
     */
    public Position getPosition() {
        return this.pos;
    }
}
