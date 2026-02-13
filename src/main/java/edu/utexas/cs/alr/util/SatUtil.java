package edu.utexas.cs.alr.util;

import edu.utexas.cs.alr.ast.*;

import java.util.*;

public class SatUtil {
    /**
     * Check if a CNF formula is satisfiable using CDCL.
     *
     * @param expr A CNF formula
     * @return true if SAT, false if UNSAT
     */
    public static boolean checkSAT(Expr expr) {
        // Verify the expression is in CNF
        if (!ExprUtils.isCNF(expr)) {
            throw new IllegalArgumentException("Expression must be in CNF format");
        }

        // Extract clauses and variables from the expression
        Set<Long> variables = new HashSet<>();
        List<Clause> clauses = extractClauses(expr, variables);

        // Handle edge cases
        if (clauses.isEmpty()) {
            // Empty formula is SAT
            return true;
        }

        // Check for empty clauses (immediately UNSAT)
        for (Clause clause : clauses) {
            if (clause.isEmpty()) {
                return false;
            }
        }

        // Filter out tautology clauses (they're always satisfied)
        List<Clause> filteredClauses = new ArrayList<>();
        for (Clause clause : clauses) {
            if (!clause.isTautology()) {
                filteredClauses.add(clause);
            }
        }

        if (filteredClauses.isEmpty()) {
            // All clauses are tautologies
            return true;
        }

        // Create and run the CDCL solver
        CDCLSolver solver = new CDCLSolver(filteredClauses, variables);
        return solver.solve();
    }

    /**
     * Extract all clauses from a CNF expression.
     *
     * @param expr      The CNF expression
     * @param variables Set to collect all variables (modified in place)
     * @return List of clauses
     */
    private static List<Clause> extractClauses(Expr expr, Set<Long> variables) {
        List<Clause> clauses = new ArrayList<>();
        Stack<Expr> stack = new Stack<>();
        stack.push(expr);

        while (!stack.isEmpty()) {
            Expr e = stack.pop();

            switch (e.getKind()) {
                case AND:
                    // AND node: push both children
                    AndExpr andExpr = (AndExpr) e;
                    stack.push(andExpr.getLeft());
                    stack.push(andExpr.getRight());
                    break;

                case OR:
                    // OR node: extract clause
                    OrExpr orExpr = (OrExpr) e;
                    Set<Long> literals = ExprUtils.getLiteralsForClause(orExpr, variables);
                    clauses.add(new Clause(literals));
                    break;

                case VAR:
                    // Single positive literal
                    VarExpr varExpr = (VarExpr) e;
                    long varId = varExpr.getId();
                    variables.add(varId);
                    clauses.add(new Clause(Collections.singletonList(varId)));
                    break;

                case NEG:
                    // Single negative literal
                    NegExpr negExpr = (NegExpr) e;
                    if (negExpr.getExpr().getKind() != Expr.ExprKind.VAR) {
                        throw new IllegalArgumentException("Expression is not in CNF");
                    }
                    VarExpr negVarExpr = (VarExpr) negExpr.getExpr();
                    long negVarId = negVarExpr.getId();
                    variables.add(negVarId);
                    clauses.add(new Clause(Collections.singletonList(-negVarId)));
                    break;

                default:
                    throw new IllegalArgumentException("Unexpected expression type: " + e.getKind());
            }
        }

        return clauses;
    }
}
