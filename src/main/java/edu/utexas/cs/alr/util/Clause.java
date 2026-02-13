package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * CNF clause representation using literal list.
 * Positive literal = variable ID, Negative literal = -variable ID
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
     * Check if at least one literal evaluates to true.
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
     * Check if all literals evaluate to false.
     */
    public boolean isConflicting(Assignment assign) {
        for (long lit : lits) {
            long varId = Math.abs(lit);
            Boolean val = assign.getValue(varId);

            // If unassigned or true, not a conflict
            if (val == null) return false;
            boolean litVal = (lit > 0) == val;
            if (litVal) return false;
        }
        return true;
    }

    /**
     * Returns the single unassigned literal if this is a unit clause.
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
     * Detect tautology: contains both L and ¬L for some variable.
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
