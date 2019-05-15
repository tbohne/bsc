package SP.experiments;

import SP.representations.Solution;

public class OptimizableSolution {

    private Solution sol;
    private double optimalObjectiveValue;

    public OptimizableSolution(Solution sol, double optimalObjectiveValue) {
        this.sol = sol;
        this.optimalObjectiveValue = optimalObjectiveValue;
    }

    public Solution getSol() {
        return this.sol;
    }

    public double getOptimalObjectiveValue() {
        return this.optimalObjectiveValue;
    }
}
