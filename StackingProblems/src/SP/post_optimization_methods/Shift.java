package SP.post_optimization_methods;

import SP.representations.StorageAreaPosition;

public class Shift {

    private int item;
    private StorageAreaPosition shiftPos;

    public Shift(int item, StorageAreaPosition shiftPos) {
        this.item = item;
        this.shiftPos = shiftPos;
    }

    public int getItem() {
        return this.item;
    }

    public StorageAreaPosition getShiftPos() {
        return this.shiftPos;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Shift) {
            return (this.item == ((Shift)object).getItem() && this.shiftPos.equals(((Shift)object).getShiftPos()));
        }
        return false;
    }

    @Override
    public String toString() {
        return this.item + " --- " + this.shiftPos.toString();
    }
}
