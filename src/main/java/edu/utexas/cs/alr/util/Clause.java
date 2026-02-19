package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Represents a single CNF clause using a list of literals.
 * Positive numbers are standard variables (e.g., 5 means x5),
 * negative numbers are negated variables (e.g., -5 means NOT x5).
 */
public class Clause {
    private final List<Long> lits;

    public Clause(List<Long> literals) {
        this.lits = new ArrayList<>(literals);
    }

    public Clause(Set<Long> literals) {
        this.lits = new ArrayList<>(literals);
    }

    public List<Long> getLiterals() {
        return Collections.unmodifiableList(lits);
    }

    public int size() {
        return lits.size();
    }

    public boolean isEmpty() {
        return lits.isEmpty();
    }

    /**
     * Checks if the clause is already satisfied (at least one literal evaluates to true).
     */
    public boolean isSatisfied(Assignment assign) {
        for (long lit : lits) {
            long varId = Math.abs(lit);
            Boolean val = assign.getValue(varId);

            if (val != null) {
                boolean litVal = (lit > 0) == val;
                if (litVal) return true;
            }
        }
        return false;
    }

    /**
     * Checks if the clause is a dead end (all literals are currently false).
     */
    public boolean isConflicting(Assignment assign) {
        for (long lit : lits) {
            long varId = Math.abs(lit);
            Boolean val = assign.getValue(varId);

            // If we find an unassigned or true literal, we haven't conflicted yet.
            if (val == null) return false;
            boolean litVal = (lit > 0) == val;
            if (litVal) return false;
        }
        return true;
    }

    /**
     * Finds the last remaining unassigned literal so we can force its value.
     * Returns null if the clause is already satisfied or if there are multiple unknowns left.
     */
    public Long getUnitLiteral(Assignment assign) {
        Long unassigned = null;
        int countUnassigned = 0;

        for (long lit : lits) {
            long varId = Math.abs(lit);
            Boolean val = assign.getValue(varId);

            if (val == null) {
                unassigned = lit;
                countUnassigned++;
                if (countUnassigned > 1) return null;
            } else {
                boolean litVal = (lit > 0) == val;
                if (litVal) return null; // Already satisfied
            }
        }

        return (countUnassigned == 1) ? unassigned : null;
    }

    /**
     * Checks if the clause is a tautology (contains both x and -x), meaning it's always true.
     */
    public boolean isTautology() {
        Set<Long> seen = new HashSet<>();
        for (long lit : lits) {
            if (seen.contains(-lit)) return true;
            seen.add(lit);
        }
        return false;
    }

    @Override
    public String toString() {
        return lits.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Clause)) return false;
        Clause other = (Clause) o;
        return new HashSet<>(lits).equals(new HashSet<>(other.lits));
    }

    @Override
    public int hashCode() {
        return new HashSet<>(lits).hashCode();
    }
}
