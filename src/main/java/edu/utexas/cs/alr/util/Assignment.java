package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Keeps track of which variables are set, what level they were set at, and why.
 */
public class Assignment {
    // Basic storage for variable states
    private final Map<Long, Boolean> values;
    private final Map<Long, Integer> levels;
    private final Map<Long, Clause> antecedents;
    private final List<Long> history;

    // The set of all variables we're tracking
    private final Set<Long> vars;

    // How deep we are in the decision tree
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
     * Manual pick: assign a value and bump the decision level.
     */
    public void decide(long varId, boolean value) {
        depth++;
        setVariable(varId, value, null);
    }

    /**
     * Forced assignment: set a variable because a clause required it.
     */
    public void propagate(long varId, boolean value, Clause reason) {
        setVariable(varId, value, reason);
    }

    /**
     * Helper: actually save the assignment data to our maps.
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
     * Look up what a variable is currently set to.
     */
    public Boolean getValue(long varId) {
        return values.get(varId);
    }

    /**
     * Find out at which depth this variable was assigned.
     */
    public Integer getDecisionLevel(long varId) {
        return levels.get(varId);
    }

    /**
     * Get the clause that forced this assignment (returns null if it was a manual decision).
     */
    public Clause getReason(long varId) {
        return antecedents.get(varId);
    }

    /**
     * Has this variable been assigned yet?
     */
    public boolean isAssigned(long varId) {
        return values.containsKey(varId);
    }

    /**
     * Check if every single variable in the problem has a value.
     */
    public boolean isComplete() {
        return values.size() == vars.size();
    }

    /**
     * Current search depth/decision level.
     */
    public int getCurrentLevel() {
        return depth;
    }

    /**
     * Get the ordered list of assignments (the trail).
     */
    public List<Long> getTrail() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Get the set of all variables currently assigned.
     */
    public Set<Long> getAssignedVariables() {
        return new HashSet<>(values.keySet());
    }

    /**
     * Get the set of variables we haven't touched yet.
     */
    public Set<Long> getUnassignedVariables() {
        Set<Long> result = new HashSet<>(vars);
        result.removeAll(values.keySet());
        return result;
    }

    /**
     * Jump back to a previous level and wipe out any assignments made after that point.
     */
    public void backtrack(int targetLevel) {
        if (targetLevel < 0 || targetLevel > depth) {
            throw new IllegalArgumentException("Invalid backtrack level: " + targetLevel);
        }

        // Pop assignments off the trail until we're back where we need to be
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
     * List all variables that were assigned at a specific level.
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
