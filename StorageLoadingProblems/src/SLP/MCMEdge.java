package SLP;

public class MCMEdge implements Comparable<MCMEdge> {

    private int vertexOne;
    private int vertexTwo;
    private int rating;

    public MCMEdge(int vertexOne, int vertexTwo, int rating) {
        this.vertexOne = vertexOne;
        this.vertexTwo = vertexTwo;
        this.rating = rating;
    }

    public int getVertexOne() {
        return vertexOne;
    }

    public int getVertexTwo() {
        return vertexTwo;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public int compareTo(MCMEdge e) {
        return this.rating - e.rating;
    }

    public String toString() {
        return "(" + this.vertexOne + ", " + this.vertexTwo + ")";
    }
}
