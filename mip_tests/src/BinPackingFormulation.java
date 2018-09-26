import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

public class BinPackingFormulation {

    public static void main(String[] args) {

        //////////////////////////////////////////////////////////////////////

        // Instance of the SLP - TODO: Read instance from file

        ArrayList<Integer> items = new ArrayList<>();
        ArrayList<ArrayList<Integer>> stacks = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            items.add(i);
        }

        for (int i = 0; i < 2; i++) {
            stacks.add(new ArrayList<>());
        }

        int b = 3;

        int[][] stackingConstraints = {
                {0, 1, 0, 0, 1, 1},
                {0, 0, 1, 1, 1, 0},
                {0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0, 0}
        };

        int[][] c = {
                {1, 1},
                {1, 1},
                {1, 1},
                {1, 1},
                {1, 1},
                {1, 1}
        };
        //////////////////////////////////////////////////////////////////////

        try {

            // define new model
            IloCplex cplex = new IloCplex();

            // VARIABLES

            IloIntVar[][] x = new IloIntVar[items.size()][];

            for (int i = 0; i < items.size(); i++) {
                x[i] = cplex.intVarArray(stacks.size(), 0, 1);
            }

            // OBJECTIVE FUNCTION

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (int i = 0; i < items.size(); i++) {
                for (int q = 0; q < stacks.size(); q++) {
                    objective.addTerm(c[i][q], x[i][q]);
                }
            }

            cplex.addMinimize(objective);

            // CONSTRAINTS

            // --- (2) ---
            for (int i = 0; i < items.size(); i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int q = 0; q < stacks.size(); q++) {
                    expr.addTerm(1, x[i][q]);
                }
                cplex.addEq(expr, 1);
            }

            // --- (3) ---
            for (int q = 0; q < stacks.size(); q++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < items.size(); i++) {
                    expr.addTerm(1, x[i][q]);
                }
                cplex.addLe(expr, b);
            }

            // --- (4) ---
            for (int i = 0; i < items.size(); i++) {
                for (int j = 0; j < items.size(); j++) {

                    if (i != j && stackingConstraints[i][j] == 0 && stackingConstraints[j][i] == 0) {
                        for (int q = 0; q < stacks.size(); q++) {
                            IloLinearIntExpr expr = cplex.linearIntExpr();
                            expr.addTerm(1, x[i][q]);
                            expr.addTerm(1, x[j][q]);
                            cplex.addLe(expr, 1);
                        }
                    }
                }
            }

            if (cplex.solve()) {
                System.out.println("obj = " + cplex.getObjValue());
            } else {
                System.out.println("problem not solved");
            }

            cplex.end();

        } catch (IloException e) {
            e.printStackTrace();
        }
    }

}
