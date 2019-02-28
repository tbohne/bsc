package SLP.representations;

public class Coordinates {

    private int xCoord;
    private int yCoord;

    public Coordinates(int xCoord, int yCoord) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    public Coordinates(Coordinates coords) {
        this.xCoord = coords.xCoord;
        this.yCoord = coords.yCoord;
    }

    public int getXCoord() {
        return this.xCoord;
    }

    public int getYCoord() {
        return this.yCoord;
    }

    @Override
    public String toString() {
        return "(" + xCoord + "," + yCoord + ")";
    }
}
