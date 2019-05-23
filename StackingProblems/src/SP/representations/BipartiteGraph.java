package SP.representations;

import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Set;

/**
 * Represents a bipartite graph consisting of two partitions.
 * It is not checked whether the graph actually is bipartite, it's just a representation
 * to store undirected graphs together with two partitions.
 *
 * @author Tim Bohne
 */
public class BipartiteGraph {

    private final Set<String> partitionOne;
    private final Set<String> partitionTwo;
    private final DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph;

    /**
     * Constructor
     *
     * @param partitionOne - first partition of the bipartite graph
     * @param partitionTwo - second partition of the bipartite graph
     * @param graph        - the actual graph
     */
    public BipartiteGraph(
        Set<String> partitionOne,
        Set<String> partitionTwo,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph
    ) {
        this.partitionOne = partitionOne;
        this.partitionTwo = partitionTwo;
        this.graph = graph;
    }

    /**
     * Returns the bipartite graph's first partition.
     *
     * @return first partition of the graph
     */
    public Set<String> getPartitionOne() {
        return this.partitionOne;
    }

    /**
     * Returns the bipartite graph's second partition.
     *
     * @return second partition of the graph
     */
    public Set<String> getPartitionTwo() {
        return this.partitionTwo;
    }

    /**
     * Returns the bipartite graph.
     *
     * @return bipartite graph
     */
    public DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> getGraph() {
        return this.graph;
    }
}
