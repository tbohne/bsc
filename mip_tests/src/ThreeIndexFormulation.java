import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class ThreeIndexFormulation {

    private Instance instance;

    ThreeIndexFormulation(Instance instance) {
        this.instance = instance;
    }

    public void solve() {

        try {

            IloCplex cplex = new IloCplex();

            // VARIABLES

            IloIntVar[][][] x = new IloIntVar[this.instance.getItems().size()][][];

            for (int i = 0; i < this.instance.getItems().size(); i++) {
                x[i] = new IloIntVar[this.instance.getStacks().size()][];
            }

            for (int i = 0; i < this.instance.getItems().size(); i++) {
                for (int j = 0; j < this.instance.getStacks().size(); j++) {
                    x[i][j] = cplex.intVarArray(this.instance.getStackCapacity(), 0, 1);
                }
            }

            // OBJECTIVE FUNCTION

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (int i = 0; i < this.instance.getItems().size(); i++) {
                for (int q = 0; q < this.instance.getStacks().size(); q++) {
                    for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                        objective.addTerm(this.instance.getCosts()[i][q], x[i][q][l]);
                    }
                }
            }

            cplex.addMinimize(objective);

            // CONSTRAINTS

            // --- (7) ---
            for (int i = 0; i < this.instance.getItems().size(); i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int q = 0; q < this.instance.getStacks().size(); q++) {
                    for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                        expr.addTerm(1, x[i][q][l]);
                    }
                }
                cplex.addEq(expr, 1);
            }

            // --- (8) ---
            for (int q = 0; q < this.instance.getStacks().size(); q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    IloLinearIntExpr expr = cplex.linearIntExpr();
                    for (int i = 0; i < this.instance.getItems().size(); i++) {
                        expr.addTerm(1, x[i][q][l]);
                    }
                    cplex.addLe(expr, 1);
                }
            }

            // --- (9) ---
            for (int i = 0; i < this.instance.getItems().size(); i++) {
                for (int q = 0; q < this.instance.getStacks().size(); q++) {
                    for (int l = 1; l < this.instance.getStackCapacity(); l++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        for (int j = 0; j < this.instance.getItems().size(); j++) {
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
            cplex.setOut(null);

            double startTime = cplex.getCplexTime();

            if (cplex.solve()) {
                System.out.println("time to solve: " + String.format("%.2f", cplex.getCplexTime() - startTime) + " s");
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

    public void setStacks(IloCplex cplex, IloIntVar[][][] x) {
        for (int i = 0; i < this.instance.getItems().size(); i++) {
            for (int q = 0; q < this.instance.getStacks().size(); q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    try {
                        if (cplex.getValue(x[i][q][l]) == 1.0) {
                            this.instance.getStacks().get(q).add(i);
                        }
                    } catch (IloException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
