package SP.post_optimization_methods;

public class Swap {

    Shift shiftOne;
    Shift shiftTwo;

    public Swap(Shift shiftOne, Shift shiftTwo) {
        this.shiftOne = shiftOne;
        this.shiftTwo = shiftTwo;
    }

    public Shift getShiftOne() {
        return this.shiftOne;
    }

    public Shift getShiftTwo() {
        return this.shiftTwo;
    }
}
