package SLP.post_optimization_methods;

public class TabuListEntry {

    private int idxItemOne;
    private int idxItemTwo;

    public TabuListEntry(int idxItemOne, int idxItemTwo) {
        this.idxItemOne = idxItemOne;
        this.idxItemTwo = idxItemTwo;
    }

    public int getIdxItemOne() {
        return this.idxItemOne;
    }

    public int getIdxItemTwo() {
        return this.idxItemTwo;
    }
}
