package edu.utexas.cs.alr.util;

import edu.utexas.cs.alr.ast.*;

import java.util.*;

public class SatUtil {
    /**
     * SAT checking using CDCL algorithm.
     */
    public static boolean checkSAT(Expr expr) {
        // Validate CNF format
        if (!ExprUtils.isCNF(expr)) {
            throw new IllegalArgumentException("Input must be in CNF format");
        }

        // Extract problem data
        Set<Long> vars = new HashSet<>();
        List<Clause> clauses = buildClauseList(expr, vars);

        // Handle trivial cases
        if (clauses.isEmpty()) return true;

        for (Clause c : clauses) {
            if (c.isEmpty()) return false;
        }

        // Remove tautologies
        List<Clause> filtered = new ArrayList<>();
        for (Clause c : clauses) {
            if (!c.isTautology()) {
                filtered.add(c);
            }
        }

        if (filtered.isEmpty()) return true;

        // Run CDCL
        CDCLSolver solver = new CDCLSolver(filtered, vars);
        return solver.solve();
    }

    /**
     * Extract clauses from CNF expression.
     */
    private static List<Clause> buildClauseList(Expr expr, Set<Long> vars) {
        List<Clause> result = new ArrayList<>();
        Deque<Expr> stack = new ArrayDeque<>();
        stack.push(expr);

        while (!stack.isEmpty()) {
            Expr e = stack.pop();

            switch (e.getKind()) {
                case AND:
                    AndExpr and = (AndExpr) e;
                    stack.push(and.getRight());
                    stack.push(and.getLeft());
                    break;

                case OR:
                    OrExpr or = (OrExpr) e;
                    Set<Long> lits = ExprUtils.getLiteralsForClause(or, vars);
                    result.add(new Clause(lits));
                    break;

                case VAR:
                    VarExpr var = (VarExpr) e;
                    vars.add(var.getId());
                    result.add(new Clause(Collections.singletonList(var.getId())));
                    break;

                case NEG:
                    NegExpr neg = (NegExpr) e;
                    if (neg.getExpr().getKind() != Expr.ExprKind.VAR) {
                        throw new IllegalArgumentException("Not in CNF");
                    }
                    VarExpr negVar = (VarExpr) neg.getExpr();
                    vars.add(negVar.getId());
                    result.add(new Clause(Collections.singletonList(-negVar.getId())));
                    break;

                default:
                    throw new IllegalArgumentException("Unexpected: " + e.getKind());
            }
        }

        return result;
    }
}
