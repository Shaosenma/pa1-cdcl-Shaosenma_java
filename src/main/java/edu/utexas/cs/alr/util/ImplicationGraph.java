package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Performs conflict analysis using the implication graph.
 * The graph is represented implicitly via reason clauses in the assignment.
 */
public class ImplicationGraph {
    private final Assignment assignment;

    public ImplicationGraph(Assignment assignment) {
        this.assignment = assignment;
    }

    /**
     * Analyze a conflict and return the learned clause and backtrack level.
     *
     * Uses the First UIP (Unique Implication Point) scheme:
     * - Start with the conflict clause
     * - Resolve backward through the assignment trail
     * - Stop when only one variable from the current decision level remains
     *
     * @param conflictClause The clause that caused the conflict
     * @return A pair (learned clause, backtrack level)
     */
    public ConflictAnalysisResult analyzeConflict(Clause conflictClause) {
        int currentLevel = assignment.getCurrentLevel();

        // Start with the conflict clause itself (not negated)
        Set<Long> learnedLiterals = new HashSet<>();
        for (long lit : conflictClause.getLiterals()) {
            learnedLiterals.add(lit);
        }

        // Count variables at the current decision level
        int currentLevelCount = countVariablesAtLevel(learnedLiterals, currentLevel);

        // Get the assignment trail
        List<Long> trail = assignment.getTrail();

        // Resolve backward until we have only one variable at the current level (First UIP)
        int trailIndex = trail.size() - 1;

        while (currentLevelCount > 1 && trailIndex >= 0) {
            long var = trail.get(trailIndex);
            trailIndex--;

            // Check if this variable is in the learned clause and at current level
            if (assignment.getDecisionLevel(var) != currentLevel) {
                continue;
            }

            // Check if the variable (or its negation) is in the learned clause
            boolean varInClause = learnedLiterals.contains(var) || learnedLiterals.contains(-var);
            if (!varInClause) {
                continue;
            }

            // Get the reason for this variable's assignment
            Clause reason = assignment.getReason(var);
            if (reason == null) {
                // This is a decision variable, not a propagated one
                continue;
            }

            // Resolve: remove the variable's literal from learned clause
            // The learned clause contains a literal that conflicts with the reason clause
            long litToRemove;
            if (learnedLiterals.contains(var)) {
                litToRemove = var;
            } else {
                litToRemove = -var;
            }

            learnedLiterals.remove(litToRemove);
            currentLevelCount--;

            // Add other literals from the reason clause (resolution)
            for (long reasonLit : reason.getLiterals()) {
                long reasonVar = Math.abs(reasonLit);
                if (reasonVar != var) {
                    // Add the literal directly (not negated) for resolution
                    if (!learnedLiterals.contains(reasonLit) && !learnedLiterals.contains(-reasonLit)) {
                        learnedLiterals.add(reasonLit);
                        if (assignment.getDecisionLevel(reasonVar) == currentLevel) {
                            currentLevelCount++;
                        }
                    }
                }
            }
        }

        // Create the learned clause
        Clause learnedClause = new Clause(new ArrayList<>(learnedLiterals));

        // Compute the backtrack level (second-highest decision level in the learned clause)
        int backtrackLevel = computeBacktrackLevel(learnedLiterals, currentLevel);

        return new ConflictAnalysisResult(learnedClause, backtrackLevel);
    }

    /**
     * Count how many variables in the literal set are at the given decision level.
     */
    private int countVariablesAtLevel(Set<Long> literals, int level) {
        int count = 0;
        for (long lit : literals) {
            long var = Math.abs(lit);
            Integer varLevel = assignment.getDecisionLevel(var);
            if (varLevel != null && varLevel == level) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compute the backtrack level (second-highest decision level in the clause).
     * If there's only one level, backtrack to level 0.
     */
    private int computeBacktrackLevel(Set<Long> literals, int currentLevel) {
        Set<Integer> levels = new HashSet<>();

        for (long lit : literals) {
            long var = Math.abs(lit);
            Integer level = assignment.getDecisionLevel(var);
            if (level != null) {
                levels.add(level);
            }
        }

        // Remove the current level and find the maximum
        List<Integer> sortedLevels = new ArrayList<>(levels);
        Collections.sort(sortedLevels, Collections.reverseOrder());

        if (sortedLevels.size() <= 1) {
            return 0; // Backtrack to level 0
        }

        // Return the second-highest level
        // If the highest is the current level, return the second one
        if (sortedLevels.get(0) == currentLevel && sortedLevels.size() > 1) {
            return sortedLevels.get(1);
        }

        return sortedLevels.get(0);
    }

    /**
     * Result of conflict analysis.
     */
    public static class ConflictAnalysisResult {
        public final Clause learnedClause;
        public final int backtrackLevel;

        public ConflictAnalysisResult(Clause learnedClause, int backtrackLevel) {
            this.learnedClause = learnedClause;
            this.backtrackLevel = backtrackLevel;
        }
    }
}
