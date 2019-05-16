package SP.experiments;

import SP.representations.Solution;

public class OptimizableSolution {

    private Solution sol;
    private double optimalObjectiveValue;
    private double runtimeForOptimalSolution;

    public OptimizableSolution(Solution sol, double optimalObjectiveValuem, double runtimeForOptimalSolution) {
        this.sol = sol;
        this.optimalObjectiveValue = optimalObjectiveValuem;
        this.runtimeForOptimalSolution = runtimeForOptimalSolution;
    }

    public Solution getSol() {
        return this.sol;
    }

    public double getOptimalObjectiveValue() {
        return this.optimalObjectiveValue;
    }

    public double getRuntimeForOptimalSolution() {
        return this.runtimeForOptimalSolution;
    }
}
