package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Tracks variable assignments, decision levels, and reasons for propagations.
 */
public class Assignment {
    // Variable assignments: var_id -> Boolean value
    private final Map<Long, Boolean> assignments;

    // Decision level for each variable
    private final Map<Long, Integer> decisionLevels;

    // Reason clause for each implied assignment (null for decisions)
    private final Map<Long, Clause> reasons;

    // Assignment trail (chronological order)
    private final List<Long> trail;

    // Current decision level
    private int currentLevel;

    // Set of all variables in the problem
    private final Set<Long> allVariables;

    public Assignment(Set<Long> variables) {
        this.allVariables = new HashSet<>(variables);
        this.assignments = new HashMap<>();
        this.decisionLevels = new HashMap<>();
        this.reasons = new HashMap<>();
        this.trail = new ArrayList<>();
        this.currentLevel = 0;
    }

    /**
     * Make a decision assignment (increment decision level).
     */
    public void decide(long var, boolean value) {
        currentLevel++;
        assign(var, value, null);
    }

    /**
     * Make a propagated assignment (keep current decision level).
     */
    public void propagate(long var, boolean value, Clause reason) {
        assign(var, value, reason);
    }

    /**
     * Internal method to assign a variable.
     */
    private void assign(long var, boolean value, Clause reason) {
        if (assignments.containsKey(var)) {
            throw new IllegalStateException("Variable " + var + " is already assigned");
        }

        assignments.put(var, value);
        decisionLevels.put(var, currentLevel);
        reasons.put(var, reason);
        trail.add(var);
    }

    /**
     * Get the value of a variable (null if unassigned).
     */
    public Boolean getValue(long var) {
        return assignments.get(var);
    }

    /**
     * Get the decision level of a variable.
     */
    public Integer getDecisionLevel(long var) {
        return decisionLevels.get(var);
    }

    /**
     * Get the reason clause for a variable's assignment.
     */
    public Clause getReason(long var) {
        return reasons.get(var);
    }

    /**
     * Check if a variable is assigned.
     */
    public boolean isAssigned(long var) {
        return assignments.containsKey(var);
    }

    /**
     * Check if all variables are assigned.
     */
    public boolean isComplete() {
        return assignments.size() == allVariables.size();
    }

    /**
     * Get the current decision level.
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Get the assignment trail.
     */
    public List<Long> getTrail() {
        return Collections.unmodifiableList(trail);
    }

    /**
     * Get all assigned variables.
     */
    public Set<Long> getAssignedVariables() {
        return new HashSet<>(assignments.keySet());
    }

    /**
     * Get all unassigned variables.
     */
    public Set<Long> getUnassignedVariables() {
        Set<Long> unassigned = new HashSet<>(allVariables);
        unassigned.removeAll(assignments.keySet());
        return unassigned;
    }

    /**
     * Backtrack to the given decision level.
     * All assignments made after this level are undone.
     */
    public void backtrack(int targetLevel) {
        if (targetLevel < 0 || targetLevel > currentLevel) {
            throw new IllegalArgumentException("Invalid target level: " + targetLevel);
        }

        // Remove assignments from the trail in reverse order
        while (!trail.isEmpty()) {
            long var = trail.get(trail.size() - 1);
            int level = decisionLevels.get(var);

            if (level <= targetLevel) {
                break;
            }

            trail.remove(trail.size() - 1);
            assignments.remove(var);
            decisionLevels.remove(var);
            reasons.remove(var);
        }

        currentLevel = targetLevel;
    }

    /**
     * Get variables assigned at a specific decision level.
     */
    public List<Long> getVariablesAtLevel(int level) {
        List<Long> vars = new ArrayList<>();
        for (long var : trail) {
            if (decisionLevels.get(var) == level) {
                vars.add(var);
            }
        }
        return vars;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Assignment(level=").append(currentLevel).append(", assignments={");
        for (long var : trail) {
            sb.append("x").append(var).append("=").append(assignments.get(var));
            sb.append("@L").append(decisionLevels.get(var));
            if (reasons.get(var) != null) {
                sb.append("[propagated]");
            } else {
                sb.append("[decision]");
            }
            sb.append(", ");
        }
        sb.append("})");
        return sb.toString();
    }
}
