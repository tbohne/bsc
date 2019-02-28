package SP.representations;

public class Item {

    private int idx;
    private int length;
    private int width;

    public Item(int idx, int length, int width) {
        this.idx = idx;
        this.length = length;
        this.width = width;
    }

    public int getIdx() {
        return this.idx;
    }

    public int getLength() {
        return  this.length;
    }

    public int getWidth() {
        return this.width;
    }
}
