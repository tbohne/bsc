package SP.util;

import SP.representations.MCMEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of experimental utility methods not yet used in the heuristics.
 *
 * @author Tim Bohne
 */
public class ExperimentalApproaches {

    // IDEA:
    // - choose a number n (20%) of random elements to be replaced
    // - choose the next n unused elements from the ordered list
    // - exchange the elements
    public static ArrayList<MCMEdge> edgeExchange(List<MCMEdge> edges, int[][] stacks) {

        ArrayList tmpEdges = new ArrayList(edges);

        int numberOfEdgesToBeReplaced = (int) (0.3 * stacks.length);
        if (numberOfEdgesToBeReplaced > (edges.size() - stacks.length)) {
            numberOfEdgesToBeReplaced = edges.size() - stacks.length;
        }
        ArrayList<Integer> toBeReplaced = new ArrayList<>();
        for (int i = 0; i < numberOfEdgesToBeReplaced; i++) {
            toBeReplaced.add(HeuristicUtil.getRandomValueInBetween(0, stacks.length - 1));
        }
        for (int i = 0; i < toBeReplaced.size(); i++) {
            Collections.swap(tmpEdges, toBeReplaced.get(i), i + stacks.length);
        }
        return new ArrayList(tmpEdges);
    }
}
