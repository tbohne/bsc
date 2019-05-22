package SP.post_optimization_methods;

public class TabuSearchConfig {

    /**
     * Enumeration containing the different stopping criteria for the tabu search.
     */
    public enum StoppingCriteria {
        ITERATIONS,
        TABU_LIST_CLEARS,
        NON_IMPROVING_ITERATIONS
    }

    /**
     * Enumeration containing the different short term strategies for the tabu search.
     */
    public enum ShortTermStrategies {
        FIRST_FIT,
        BEST_FIT
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    public static final ShortTermStrategies SHORT_TERM_STRATEGY = ShortTermStrategies.BEST_FIT;
    public static final StoppingCriteria STOPPING_CRITERION = StoppingCriteria.NON_IMPROVING_ITERATIONS;

    public static final boolean FEASIBLE_ONLY = true;
    public static final int XSWAP_INTERVAL_UB = 4;

    public static final int NUMBER_OF_ITERATIONS = 50;
    public static final int NUMBER_OF_TABU_LIST_CLEARS = 10;
    public static final int NUMBER_OF_NON_IMPROVING_ITERATIONS = 50;

    public static final int NUMBER_OF_NEIGHBORS = 1000;
    public static final int MAX_TABU_LIST_LENGTH_FACTOR = 9;
    public static final int UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS = 5000;
    public static final int UNSUCCESSFUL_X_SWAP_ATTEMPTS = 1000;
    /////////////////////////////////////////////////////////////////////////////////////////////////
}
