package SP.mip_formulations;

import SP.representations.Instance;
import SP.representations.Solution;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Param;

/**
 * Represents the three-index formulation (MIP) of stacking problems to be used with CPLEX.
 *
 * @author Tim Bohne
 */
public class ThreeIndexFormulation {

    private Instance instance;
    private double timeLimit;

    /**
     * Constructor
     *
     * @param instance  - the instance to be solved
     * @param timeLimit - the time limit for the solving procedure
     */
    public ThreeIndexFormulation(Instance instance, double timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Solves the stacking problem using the bin-packing formulation.
     *
     * @return the generated solution to the stacking problem
     */
    public Solution solve() {

        Solution sol = new Solution();

        try {
            // Defines a new model.
            IloCplex cplex = new IloCplex();

            // VARIABLES

            IloIntVar[][][] x = new IloIntVar[this.instance.getItems().length][][];

            for (int i = 0; i < this.instance.getItems().length; i++) {
                x[i] = new IloIntVar[this.instance.getStacks().length][];
            }

            for (int i = 0; i < this.instance.getItems().length; i++) {
                for (int j = 0; j < this.instance.getStacks().length; j++) {
                    x[i][j] = cplex.intVarArray(this.instance.getStackCapacity(), 0, 1);
                }
            }

            // OBJECTIVE FUNCTION

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (int i = 0; i < this.instance.getItems().length; i++) {
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                        objective.addTerm(this.instance.getCosts()[i][q], x[i][q][l]);
                    }
                }
            }

            cplex.addMinimize(objective);

            // CONSTRAINTS

            // --- (7) ---
            for (int i = 0; i < this.instance.getItems().length; i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    if (this.instance.getPlacementConstraints()[i][q] == 1) {
                        for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                            expr.addTerm(1, x[i][q][l]);
                        }
                    }
                }
                cplex.addEq(expr, 1);
            }

            // --- (8) ---
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    IloLinearIntExpr expr = cplex.linearIntExpr();
                    for (int i = 0; i < this.instance.getItems().length; i++) {
                        expr.addTerm(1, x[i][q][l]);
                    }
                    cplex.addLe(expr, 1);
                }
            }

            // --- (9) ---
            for (int i = 0; i < this.instance.getItems().length; i++) {
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    for (int l = 1; l < this.instance.getStackCapacity(); l++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        for (int j = 0; j < this.instance.getItems().length; j++) {
                            if (i != j && this.instance.getStackingConstraints()[i][j] == 1) {
                                expr.addTerm(1, x[j][q][l - 1]);
                            }
                        }
                        expr.addTerm(-1, x[i][q][l]);
                        cplex.addGe(expr, 0);
                    }
                }
            }

            System.out.println();
//            cplex.setOut(null);

            // sets the time limit
            cplex.setParam(Param.TimeLimit, timeLimit);

            double startTime = cplex.getCplexTime();
            // 1 --> emphasizes feasibility over optimality
            cplex.setParam(IloCplex.IntParam.MIPEmphasis, 1);
            // sets the tolerance to 0.0 - CPLEX will only terminate before the time limit if the actual optimum is found
            cplex.setParam(IloCplex.DoubleParam.EpAGap, 0.0);
            cplex.setParam(IloCplex.DoubleParam.EpGap, 0.0);

            if (cplex.solve()) {
                double timeToSolve = cplex.getCplexTime() - startTime;
                this.setStacks(cplex, x);
                sol = new Solution(timeToSolve, Math.round(cplex.getObjValue() * 100.0) / 100.0, timeLimit, this.instance);
                sol.lowerItemsThatAreStackedInTheAir();
            }
            cplex.end();

        } catch (IloException e) {
            e.printStackTrace();
        }
        return sol;
    }

    /**
     * Reverses the item order for each stack to be consistent with the other solvers.
     */
    public void reverseItemOrderForEachStack() {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length / 2; j++) {
                int tmp = this.instance.getStacks()[i][j];
                this.instance.getStacks()[i][j] = this.instance.getStacks()[i][this.instance.getStacks()[i].length - j - 1];
                this.instance.getStacks()[i][this.instance.getStacks()[i].length - j - 1] = tmp;
            }
        }
    }

    /**
     * Sets the stacks based on the CPLEX variables.
     *
     * @param cplex - the CPLEX model
     * @param x     - the variable x_iq
     */
    public void setStacks(IloCplex cplex, IloIntVar[][][] x) {

        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    try {
                        if (Math.round(cplex.getValue(x[i][q][l])) == 1) {
                            this.instance.getStacks()[q][l] = i;
                        }
                    } catch (IloException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Used to end up with a top to bottom order.
        this.reverseItemOrderForEachStack();
    }
}
