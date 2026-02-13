package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Variable assignment tracker with decision level management.
 */
public class Assignment {
    // Core assignment data
    private final Map<Long, Boolean> values;
    private final Map<Long, Integer> levels;
    private final Map<Long, Clause> antecedents;
    private final List<Long> history;

    // Problem variables
    private final Set<Long> vars;

    // Current decision depth
    private int depth;

    public Assignment(Set<Long> variables) {
        this.vars = new HashSet<>(variables);
        this.values = new HashMap<>();
        this.levels = new HashMap<>();
        this.antecedents = new HashMap<>();
        this.history = new ArrayList<>();
        this.depth = 0;
    }

    /**
     * Decision: assign variable and increment level.
     */
    public void decide(long varId, boolean value) {
        depth++;
        setVariable(varId, value, null);
    }

    /**
     * Propagation: assign variable at current level.
     */
    public void propagate(long varId, boolean value, Clause reason) {
        setVariable(varId, value, reason);
    }

    /**
     * Internal: record variable assignment.
     */
    private void setVariable(long varId, boolean value, Clause reason) {
        if (values.containsKey(varId)) {
            throw new IllegalStateException("Variable already assigned: " + varId);
        }

        values.put(varId, value);
        levels.put(varId, depth);
        antecedents.put(varId, reason);
        history.add(varId);
    }

    /**
     * Get variable's current value.
     */
    public Boolean getValue(long varId) {
        return values.get(varId);
    }

    /**
     * Get variable's decision level.
     */
    public Integer getDecisionLevel(long varId) {
        return levels.get(varId);
    }

    /**
     * Get reason clause for propagated variable.
     */
    public Clause getReason(long varId) {
        return antecedents.get(varId);
    }

    /**
     * Check if variable is assigned.
     */
    public boolean isAssigned(long varId) {
        return values.containsKey(varId);
    }

    /**
     * Check if all variables are assigned.
     */
    public boolean isComplete() {
        return values.size() == vars.size();
    }

    /**
     * Get current decision level.
     */
    public int getCurrentLevel() {
        return depth;
    }

    /**
     * Get assignment trail.
     */
    public List<Long> getTrail() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Get assigned variables.
     */
    public Set<Long> getAssignedVariables() {
        return new HashSet<>(values.keySet());
    }

    /**
     * Get unassigned variables.
     */
    public Set<Long> getUnassignedVariables() {
        Set<Long> result = new HashSet<>(vars);
        result.removeAll(values.keySet());
        return result;
    }

    /**
     * Non-chronological backtracking to target level.
     */
    public void backtrack(int targetLevel) {
        if (targetLevel < 0 || targetLevel > depth) {
            throw new IllegalArgumentException("Invalid backtrack level: " + targetLevel);
        }

        // Undo assignments in reverse order
        while (!history.isEmpty()) {
            long varId = history.get(history.size() - 1);
            int lvl = levels.get(varId);

            if (lvl <= targetLevel) break;

            history.remove(history.size() - 1);
            values.remove(varId);
            levels.remove(varId);
            antecedents.remove(varId);
        }

        depth = targetLevel;
    }

    /**
     * Get variables assigned at specific level.
     */
    public List<Long> getVariablesAtLevel(int level) {
        List<Long> result = new ArrayList<>();
        for (long varId : history) {
            if (levels.get(varId) == level) {
                result.add(varId);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Assignment[level=").append(depth).append(", vars={");
        for (long varId : history) {
            sb.append("x").append(varId).append("=").append(values.get(varId));
            sb.append("@").append(levels.get(varId));
            sb.append(antecedents.get(varId) != null ? "(P)" : "(D)");
            sb.append(" ");
        }
        sb.append("}]");
        return sb.toString();
    }
}
