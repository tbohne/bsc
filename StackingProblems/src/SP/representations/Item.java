package SP.representations;

/**
 * Represents an item that is going to be assigned to a stack position as part of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Item {

    private int idx;
    private int length;
    private int width;

    /**
     * Constructor
     *
     * @param idx    - the index that identifies the item
     * @param length - the length of the item
     * @param width  - the width of the item
     */
    public Item(int idx, int length, int width) {
        this.idx = idx;
        this.length = length;
        this.width = width;
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
    public int getLength() {
        return  this.length;
    }

    /**
     * Returns the item's width
     *
     * @return the item's width
     */
    public int getWidth() {
        return this.width;
    }
}
