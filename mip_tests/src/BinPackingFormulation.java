import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class BinPackingFormulation {

    private Instance instance;

    BinPackingFormulation(Instance instance) {
        this.instance = instance;
    }

    public Solution solve() {

        Solution sol = new Solution(0.0, 0.0, this.instance.getStacks());

        try {

            // define new model
            IloCplex cplex = new IloCplex();

            // VARIABLES

            IloIntVar[][] x = new IloIntVar[this.instance.getItems().length][];

            for (int i = 0; i < this.instance.getItems().length; i++) {
                x[i] = cplex.intVarArray(this.instance.getStacks().length, 0, 1);
            }

            // OBJECTIVE FUNCTION

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (int i = 0; i < this.instance.getItems().length; i++) {
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    objective.addTerm(this.instance.getCosts()[i][q], x[i][q]);
                }
            }

            cplex.addMinimize(objective);

            // CONSTRAINTS

            // --- (2) ---
            for (int i = 0; i < this.instance.getItems().length; i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int q = 0; q < this.instance.getStacks().length; q++) {
                    expr.addTerm(1, x[i][q]);
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
            System.out.println();
            cplex.setOut(null);

            double startTime = cplex.getCplexTime();

            if (cplex.solve()) {
                System.out.println("time to solve: " + String.format("%.2f", cplex.getCplexTime() - startTime) + " s");
                System.out.println("obj = " + cplex.getObjValue());
                this.setStacks(cplex, x);
                this.getSolutionFromStackAssignment();
                sol = new Solution(cplex.getCplexTime() - startTime, cplex.getObjValue(), this.instance.getStacks());
            } else {
                System.out.println("problem not solved");
            }

            cplex.end();

        } catch (IloException e) {
            e.printStackTrace();
        }

        return sol;
    }

    public void getSolutionFromStackAssignment() {

        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {

            boolean somethingChanged = true;

            while (somethingChanged) {

                somethingChanged = false;

                for (int item = 0; item < this.instance.getStackCapacity() - 1; item++) {

                    if (this.instance.getStacks()[stack][item] != -1 && this.instance.getStacks()[stack][item + 1] != -1) {
                        if (this.instance.getStackingConstraints()[this.instance.getStacks()[stack][item]][this.instance.getStacks()[stack][item + 1]] == 0) {
                            int tmp = this.instance.getStacks()[stack][item];
                            this.instance.getStacks()[stack][item] = this.instance.getStacks()[stack][item + 1];
                            this.instance.getStacks()[stack][item + 1] = tmp;
                            somethingChanged = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setStacks(IloCplex cplex, IloIntVar[][] x) {
        for (int i = 0; i < x.length; i++) {
            for (int q = 0; q < x[0].length; q++) {
                try {
                    if (cplex.getValue(x[i][q]) == 1.0) {
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

