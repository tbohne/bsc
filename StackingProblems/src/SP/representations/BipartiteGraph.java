package SP.representations;

import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Set;

/**
 * Represents a bipartite graph consisting of two partitions.
 *
 * @author Tim Bohne
 */
public class BipartiteGraph {

    private Set<String> partitionOne;
    private Set<String> partitionTwo;
    private DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph;

    /**
     * Constructor
     *
     * @param partitionOne - the first partition of the bipartite graph
     * @param partitionTwo - the second partition of the bipartite graph
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
     * @return the bipartite graph's first partition
     */
    public Set<String> getPartitionOne() {
        return this.partitionOne;
    }

    /**
     * Returns the bipartite graph's second partition.
     *
     * @return the bipartite graph's second partition
     */
    public Set<String> getPartitionTwo() {
        return this.partitionTwo;
    }

    /**
     * Returns the bipartite graph.
     *
     * @return the bipartite graph
     */
    public DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> getGraph() {
        return this.graph;
    }
}
