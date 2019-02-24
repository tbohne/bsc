package SLP.representations;

import SLP.post_optimization_methods.Exchange;

public class StorageAreaPosition {

    private int stackIdx;
    private int level;

    public StorageAreaPosition(int stackIdx, int level) {
        this.stackIdx = stackIdx;
        this.level = level;
    }

    public int getStackIdx() {
        return this.stackIdx;
    }

    public int getLevel() {
        return this.level;
    }
}
