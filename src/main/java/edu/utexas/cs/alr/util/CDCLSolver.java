package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * CDCL (Conflict-Driven Clause Learning) SAT Solver.
 */
public class CDCLSolver {
    private final List<Clause> clauses;
    private final Set<Long> variables;
    private final Assignment assignment;
    private final DecisionHeuristic heuristic;
    private final ImplicationGraph implGraph;

    // Learned clauses
    private final List<Clause> learnedClauses;

    public CDCLSolver(List<Clause> clauses, Set<Long> variables) {
        this.clauses = new ArrayList<>(clauses);
        this.variables = new HashSet<>(variables);
        this.assignment = new Assignment(variables);
        this.heuristic = new DecisionHeuristic(variables);
        this.implGraph = new ImplicationGraph(assignment);
        this.learnedClauses = new ArrayList<>();
    }

    /**
     * Main CDCL solving algorithm.
     * Returns true if SAT, false if UNSAT.
     */
    public boolean solve() {
        // Initial unit propagation at level 0
        Clause conflict = unitPropagate();
        if (conflict != null) {
            // Conflict at level 0 means UNSAT
            return false;
        }

        // Main CDCL loop
        while (true) {
            // Check if all variables are assigned
            if (assignment.isComplete()) {
                return true; // SAT
            }

            // Make a decision
            Long var = heuristic.chooseVariable(assignment);
            if (var == null) {
                // Should not happen if assignment is not complete
                return true;
            }

            boolean value = heuristic.chooseValue(var);
            assignment.decide(var, value);

            // Unit propagate
            conflict = unitPropagate();

            // Handle conflicts
            while (conflict != null) {
                if (assignment.getCurrentLevel() == 0) {
                    // Conflict at level 0 means UNSAT
                    return false;
                }

                // Analyze conflict
                ImplicationGraph.ConflictAnalysisResult result = implGraph.analyzeConflict(conflict);
                Clause learnedClause = result.learnedClause;
                int backtrackLevel = result.backtrackLevel;

                // Learn the clause
                learnedClauses.add(learnedClause);

                // Bump activities of variables in the learned clause
                heuristic.bumpActivities(learnedClause);
                heuristic.decayActivities();

                // Backtrack
                if (backtrackLevel < 0) {
                    return false; // UNSAT
                }
                assignment.backtrack(backtrackLevel);

                // Unit propagate with the learned clause
                conflict = unitPropagate();
            }
        }
    }

    /**
     * Boolean Constraint Propagation (BCP) - Unit Propagation.
     * Returns a conflict clause if a conflict is detected, null otherwise.
     */
    private Clause unitPropagate() {
        boolean changed = true;

        while (changed) {
            changed = false;

            // Check all clauses (original + learned)
            List<Clause> allClauses = new ArrayList<>(clauses);
            allClauses.addAll(learnedClauses);

            for (Clause clause : allClauses) {
                // Skip if clause is already satisfied
                if (clause.isSatisfied(assignment)) {
                    continue;
                }

                // Check for conflict
                if (clause.isConflicting(assignment)) {
                    return clause; // Return the conflict clause
                }

                // Check for unit clause
                Long unitLit = clause.getUnitLiteral(assignment);
                if (unitLit != null) {
                    long var = Math.abs(unitLit);
                    boolean value = (unitLit > 0);

                    // Propagate the unit literal
                    assignment.propagate(var, value, clause);
                    changed = true;

                    // After propagation, check for immediate conflicts
                    // This will be caught in the next iteration
                }
            }
        }

        return null; // No conflict
    }

    /**
     * Get the current assignment (for debugging/testing).
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Get the number of learned clauses.
     */
    public int getLearnedClauseCount() {
        return learnedClauses.size();
    }
}
