import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

public class BinPackingFormulation {

    private Instance instance;

    BinPackingFormulation(Instance instance) {
        this.instance = instance;
    }

    public void solve() {

        try {

            // define new model
            IloCplex cplex = new IloCplex();

            // VARIABLES

            IloIntVar[][] x = new IloIntVar[this.instance.getItems().size()][];

            for (int i = 0; i < this.instance.getItems().size(); i++) {
                x[i] = cplex.intVarArray(this.instance.getStacks().size(), 0, 1);
            }

            // OBJECTIVE FUNCTION

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (int i = 0; i < this.instance.getItems().size(); i++) {
                for (int q = 0; q < this.instance.getStacks().size(); q++) {
                    objective.addTerm(this.instance.getCosts()[i][q], x[i][q]);
                }
            }

            cplex.addMinimize(objective);

            // CONSTRAINTS

            // --- (2) ---
            for (int i = 0; i < this.instance.getItems().size(); i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int q = 0; q < this.instance.getStacks().size(); q++) {
                    expr.addTerm(1, x[i][q]);
                }
                cplex.addEq(expr, 1);
            }

            // --- (3) ---
            for (int q = 0; q < this.instance.getStacks().size(); q++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < this.instance.getItems().size(); i++) {
                    expr.addTerm(1, x[i][q]);
                }
                cplex.addLe(expr, this.instance.getStackCapacity());
            }

            // --- (4) ---
            for (int i = 0; i < this.instance.getItems().size(); i++) {
                for (int j = 0; j < this.instance.getItems().size(); j++) {

                    if (i != j && this.instance.getStackingConstraints()[i][j] == 0 && this.instance.getStackingConstraints()[j][i] == 0) {
                        for (int q = 0; q < this.instance.getStacks().size(); q++) {
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

            if (cplex.solve()) {
                System.out.println("obj = " + cplex.getObjValue());
                this.setStacks(cplex, x);
                this.printStacks();
            } else {
                System.out.println("problem not solved");
            }

            cplex.end();

        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    public void printStacks() {
        for (int i = 0; i < this.instance.getStacks().size(); i++) {
            System.out.print("stack " + i + ": ");
            System.out.println(this.instance.getStacks().get(i));
        }
    }

    public void setStacks(IloCplex cplex, IloIntVar[][] x) {
        for (int i = 0; i < 6; i++) {
            for (int q = 0; q < 2; q++) {
                try {
                    if (cplex.getValue(x[i][q]) == 1.0) {
                        this.instance.getStacks().get(q).add(i);
                    }
                } catch (IloException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

