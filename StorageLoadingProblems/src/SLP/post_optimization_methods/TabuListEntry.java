package SLP.post_optimization_methods;

import SLP.representations.StorageAreaPosition;

public class TabuListEntry {

    private StorageAreaPosition posOne;
    private StorageAreaPosition posTwo;

    public TabuListEntry(StorageAreaPosition posOne, StorageAreaPosition posTwo) {
        this.posOne = posOne;
        this.posTwo = posTwo;
    }

    public StorageAreaPosition getPosOne() {
        return this.posOne;
    }

    public StorageAreaPosition getPosTwo() {
        return this.posTwo;
    }
}
