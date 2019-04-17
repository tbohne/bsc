package SP.post_optimization_methods;

import SP.representations.StorageAreaPosition;

public class Exchange {

    private StorageAreaPosition posOne;
    private StorageAreaPosition posTwo;

    public Exchange(StorageAreaPosition posOne, StorageAreaPosition posTwo) {
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
        if (object != null && object instanceof Exchange) {
            return (this.posOne.equals(((Exchange)object).getPosOne()) && this.posTwo.equals(((Exchange)object).getPosTwo()))
                ||(this.posOne.equals(((Exchange)object).getPosTwo()) && this.posTwo.equals(((Exchange)object).getPosOne()));
        }
        return false;
    }

    @Override
    public String toString() {
        return this.posOne.toString() + " --- " + this.posTwo.toString();
    }
}
