package SP.post_optimization_methods;

import SP.representations.StorageAreaPosition;

public class Swap {

    private StorageAreaPosition posOne;
    private StorageAreaPosition posTwo;

    public Swap(StorageAreaPosition posOne, StorageAreaPosition posTwo) {
        this.posOne = posOne;
        this.posTwo = posTwo;
    }

    public StorageAreaPosition getPosOne() {
        return this.posOne;
    }

    public StorageAreaPosition getPosTwo() {
        return this.posTwo;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Swap) {
            return (this.posOne.equals(((Swap)object).getPosOne()) && this.posTwo.equals(((Swap)object).getPosTwo()))
                ||(this.posOne.equals(((Swap)object).getPosTwo()) && this.posTwo.equals(((Swap)object).getPosOne()));
        }
        return false;
    }

    @Override
    public String toString() {
        return this.posOne.toString() + " --- " + this.posTwo.toString();
    }
}
