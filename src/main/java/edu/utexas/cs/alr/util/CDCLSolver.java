package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * CDCL SAT solver implementation.
 */
public class CDCLSolver {
    private final List<Clause> originalClauses;
    private final List<Clause> learnedClauses;
    private final Set<Long> variables;
    private final Assignment assignment;
    private final DecisionHeuristic heuristic;
    private final ImplicationGraph graph;

    public CDCLSolver(List<Clause> clauses, Set<Long> vars) {
        this.originalClauses = new ArrayList<>(clauses);
        this.learnedClauses = new ArrayList<>();
        this.variables = new HashSet<>(vars);
        this.assignment = new Assignment(vars);
        this.heuristic = new DecisionHeuristic(vars);
        this.graph = new ImplicationGraph(assignment);
    }

    /**
     * Main CDCL search loop.
     */
    public boolean solve() {
        // Initial BCP at level 0
        Clause conflict = propagate();
        if (conflict != null) return false;

        // Main search
        while (true) {
            // Check for complete assignment
            if (assignment.isComplete()) {
                return true;
            }

            // Make decision
            Long var = heuristic.chooseVariable(assignment);
            if (var == null) return true;

            boolean val = heuristic.chooseValue(var);
            assignment.decide(var, val);

            // Propagate and handle conflicts
            conflict = propagate();
            while (conflict != null) {
                if (assignment.getCurrentLevel() == 0) {
                    return false; // UNSAT
                }

                // Learn from conflict
                ImplicationGraph.AnalysisResult result = graph.analyzeConflict(conflict);
                learnedClauses.add(result.learnedClause);

                // Update heuristic
                heuristic.bumpActivities(result.learnedClause);
                heuristic.decayActivities();

                // Backtrack
                if (result.backtrackLevel < 0) {
                    return false;
                }
                assignment.backtrack(result.backtrackLevel);

                // Continue propagation
                conflict = propagate();
            }
        }
    }

    /**
     * Boolean Constraint Propagation.
     */
    private Clause propagate() {
        boolean hasChange = true;

        while (hasChange) {
            hasChange = false;

            // Check all clauses
            for (Clause c : getAllClauses()) {
                // Skip satisfied clauses
                if (c.isSatisfied(assignment)) {
                    continue;
                }

                // Detect conflict
                if (c.isConflicting(assignment)) {
                    return c;
                }

                // Unit propagation
                Long unitLit = c.getUnitLiteral(assignment);
                if (unitLit != null) {
                    long var = Math.abs(unitLit);
                    boolean val = (unitLit > 0);
                    assignment.propagate(var, val, c);
                    hasChange = true;
                }
            }
        }

        return null;
    }

    /**
     * Get all clauses (original + learned).
     */
    private List<Clause> getAllClauses() {
        List<Clause> all = new ArrayList<>(originalClauses);
        all.addAll(learnedClauses);
        return all;
    }

    /**
     * Get current assignment.
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Get learned clause count.
     */
    public int getLearnedClauseCount() {
        return learnedClauses.size();
    }
}
