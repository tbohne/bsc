package SP.post_optimization_methods;

import SP.representations.StorageAreaPosition;

public class Swap {

    private StorageAreaPosition posOne;
    private StorageAreaPosition posTwo;
    private int itemOne;
    private int itemTwo;

    public Swap(StorageAreaPosition posOne, StorageAreaPosition posTwo, int itemOne, int itemTwo) {
        this.posOne = posOne;
        this.posTwo = posTwo;
        this.itemOne = itemOne;
        this.itemTwo = itemTwo;
    }

    public StorageAreaPosition getPosOne() {
        return this.posOne;
    }

    public StorageAreaPosition getPosTwo() {
        return this.posTwo;
    }

    public int getItemOne() {
        return this.itemOne;
    }

    public int getItemTwo() {
        return this.itemTwo;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Swap) {

            // slots the same
            if (this.posOne.equals(((Swap)object).getPosOne()) && this.posTwo.equals(((Swap)object).getPosTwo())) {
                // associated items the same
                if (this.itemOne == ((Swap)object).getItemOne() && this.itemTwo == ((Swap)object).getItemTwo()) {
                    return true;
                }
                // associated items the same
                if (this.itemOne == ((Swap)object).getItemTwo() && this.itemTwo == ((Swap)object).getItemOne()) {
                    return true;
                }
            }

            // slots the same
            if (this.posOne.equals(((Swap)object).getPosTwo()) && this.posTwo.equals(((Swap)object).getPosOne())) {
                // associated items the same
                if (this.itemOne == ((Swap)object).getItemOne() && this.itemTwo == ((Swap)object).getItemTwo()) {
                    return true;
                }
                // associated items the same
                if (this.itemOne == ((Swap)object).getItemTwo() && this.itemTwo == ((Swap)object).getItemOne()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.posOne.toString() + " --- " + this.posTwo.toString();
    }
}
