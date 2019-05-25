package SP.mip_formulations;

import SP.representations.Instance;
import SP.representations.Solution;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

/**
 * Represents the bin-packing MIP formulation of stacking problems that is solved by CPLEX.
 *
 * @author Tim Bohne
 */
public class BinPackingFormulation {

    private Instance instance;
    private double timeLimit;

    /**
     * Constructor
     *
     * @param instance  - instance to be solved
     * @param timeLimit - time limit for the solving procedure
     */
    public BinPackingFormulation(Instance instance, double timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Adds the bin-packing formulation's constraints to the CPLEX model.
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iq
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void addConstraints(IloCplex cplex, IloIntVar[][] x) throws ilog.concert.IloException {

        // --- (2) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                if (this.instance.getPlacementConstraints()[i][q] == 1) {
                    expr.addTerm(1, x[i][q]);
                }
            }
            cplex.addEq(expr, 1);
        }

        // --- (3) ---
        for (int q = 0; q < this.instance.getStacks().length; q++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int i = 0; i < this.instance.getItems().length; i++) {
                expr.addTerm(1, x[i][q]);
            }
            cplex.addLe(expr, this.instance.getStackCapacity());
        }

        // --- (4) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int j = 0; j < this.instance.getItems().length; j++) {

                if (i != j && this.instance.getStackingConstraints()[i][j] == 0 && this.instance.getStackingConstraints()[j][i] == 0) {
                    for (int q = 0; q < this.instance.getStacks().length; q++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        expr.addTerm(1, x[i][q]);
                        expr.addTerm(1, x[j][q]);
                        cplex.addLe(expr, 1);
                    }
                }
            }
        }
    }

    private void setCPLEXConfig(IloCplex cplex) throws ilog.concert.IloException {
        // set CPLEX output
        cplex.setOut(null);

        // set time limit
        cplex.setParam(IloCplex.Param.TimeLimit, this.timeLimit);

        // 1 --> emphasize feasibility over optimality
        cplex.setParam(IloCplex.IntParam.MIPEmphasis, 1);

        // set tolerance to 0.0 - CPLEX will only terminate before the time limit if the actual optimum is found
        cplex.setParam(IloCplex.DoubleParam.EpAGap, 0.0);
        cplex.setParam(IloCplex.DoubleParam.EpGap, 0.0);
    }

    /**
     * Solves the stacking problem using the bin-packing formulation.
     *
     * @return generated solution to the stacking problem
     */
    public Solution solve() {

        Solution sol = new Solution();

        try {
            // define new model
            IloCplex cplex = new IloCplex();

            // init variables x_iq
            IloIntVar[][] x = new IloIntVar[this.instance.getItems().length][];
            for (int i = 0; i < this.instance.getItems().length; i++) {
                x[i] = cplex.intVarArray(this.instance.getStacks().length, 0, 1);
            }

            // define objective
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for (int i = 0; i < this.instance.getItems().length; i++) {
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    objective.addTerm(this.instance.getCosts()[i][q], x[i][q]);
                }
            }
            cplex.addMinimize(objective);

            this.addConstraints(cplex, x);

            this.setCPLEXConfig(cplex);
            double startTime = cplex.getCplexTime();

            if (cplex.solve()) {
                double timeToSolve = cplex.getCplexTime() - startTime;
                this.setStacks(cplex, x);
                this.instance.lowerItemsThatAreStackedInTheAir();
                sol = new Solution(timeToSolve, timeLimit, this.instance);
                sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
            }
            cplex.end();

        } catch (IloException e) {
            e.printStackTrace();
        }
        return sol;
    }

    /**
     * Sets the stacks based on the CPLEX variables.
     *
     * @param cplex - the cplex model
     * @param x     - the variables x_iq
     */
    public void setStacks(IloCplex cplex, IloIntVar[][] x) {
        for (int i = 0; i < x.length; i++) {
            for (int q = 0; q < x[0].length; q++) {
                try {
                    if (Math.round(cplex.getValue(x[i][q])) == 1) {
                        int idx = 0;
                        while (this.instance.getStacks()[q][idx] != -1) {
                            idx++;
                        }
                        this.instance.getStacks()[q][idx] = i;
                    }
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

