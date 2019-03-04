package SP.representations;

/**
 * Represents an edge in a maximum cardinality matching.
 *
 * @author Tim Bohne
 */
public class MCMEdge implements Comparable<MCMEdge> {

    private int vertexOne;
    private int vertexTwo;
    private int rating;

    /**
     * Constructor
     *
     * @param vertexOne - the edge's first vertex
     * @param vertexTwo - the edge's second vertex
     * @param rating    - the edge's rating
     */
    public MCMEdge(int vertexOne, int vertexTwo, int rating) {
        this.vertexOne = vertexOne;
        this.vertexTwo = vertexTwo;
        this.rating = rating;
    }

    /**
     * Copy-Constructor
     *
     * @param edge - the edge to be copied
     */
    public MCMEdge(MCMEdge edge) {
        this.vertexOne = edge.getVertexOne();
        this.vertexTwo = edge.getVertexTwo();
        this.rating = edge.rating;
    }

    /**
     * Returns the index of the edge's first vertex.
     *
     * @return the edge's first vertex
     */
    public int getVertexOne() {
        return vertexOne;
    }

    /**
     * Returns the index of the edge's second vertex.
     *
     * @return the edge's second vertex
     */
    public int getVertexTwo() {
        return vertexTwo;
    }

    /**
     * Returns the edge's rating.
     *
     * @return the edge's rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the edge's rating.
     *
     * @param rating - the rating to be set
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Flips the edge's vertices.
     */
    public void flipVertices() {
        int tmp = this.vertexOne;
        this.vertexOne = this.vertexTwo;
        this.vertexTwo = tmp;
    }

    /**
     * Compares two edges in terms of their rating.
     *
     * @param e - the edge to be compared to
     * @return whether this edge's rating is less than, equal to, or greater than the other edge's rating
     */
    @Override
    public int compareTo(MCMEdge e) {
        return this.rating - e.rating;
    }

    /**
     * Returns a string visualizing the edge.
     *
     * @return a string visualizing the edge
     */
    public String toString() {
        return "(" + this.vertexOne + ", " + this.vertexTwo + ")";
    }
}
