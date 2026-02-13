package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Represents a CNF clause as a list of literals.
 * Literals are represented as long values:
 * - Positive literal: var_id (e.g., 5 for x5)
 * - Negative literal: -var_id (e.g., -5 for ¬x5)
 */
public class Clause {
    private final List<Long> literals;

    public Clause(List<Long> literals) {
        this.literals = new ArrayList<>(literals);
    }

    public Clause(Set<Long> literals) {
        this.literals = new ArrayList<>(literals);
    }

    public List<Long> getLiterals() {
        return Collections.unmodifiableList(literals);
    }

    public int size() {
        return literals.size();
    }

    public boolean isEmpty() {
        return literals.isEmpty();
    }

    /**
     * Check if this clause is satisfied under the given assignment.
     * A clause is satisfied if at least one literal is true.
     */
    public boolean isSatisfied(Assignment assignment) {
        for (long lit : literals) {
            long var = Math.abs(lit);
            Boolean value = assignment.getValue(var);

            if (value != null) {
                boolean litValue = (lit > 0) ? value : !value;
                if (litValue) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if this clause is conflicting (all literals are false).
     */
    public boolean isConflicting(Assignment assignment) {
        for (long lit : literals) {
            long var = Math.abs(lit);
            Boolean value = assignment.getValue(var);

            if (value == null) {
                return false; // Unassigned literal, not a conflict yet
            }

            boolean litValue = (lit > 0) ? value : !value;
            if (litValue) {
                return false; // At least one literal is true
            }
        }
        return true; // All literals are false
    }

    /**
     * Get the unit literal if this clause is unit under the given assignment.
     * A clause is unit if exactly one literal is unassigned and all others are false.
     * Returns null if the clause is not unit.
     */
    public Long getUnitLiteral(Assignment assignment) {
        Long unassignedLit = null;
        int unassignedCount = 0;

        for (long lit : literals) {
            long var = Math.abs(lit);
            Boolean value = assignment.getValue(var);

            if (value == null) {
                unassignedLit = lit;
                unassignedCount++;
                if (unassignedCount > 1) {
                    return null; // More than one unassigned
                }
            } else {
                boolean litValue = (lit > 0) ? value : !value;
                if (litValue) {
                    return null; // Clause is already satisfied
                }
            }
        }

        // Unit clause if exactly one unassigned literal
        return (unassignedCount == 1) ? unassignedLit : null;
    }

    /**
     * Check if this clause is a tautology (contains both x and ¬x).
     */
    public boolean isTautology() {
        Set<Long> vars = new HashSet<>();
        for (long lit : literals) {
            long var = Math.abs(lit);
            if (vars.contains(-lit)) {
                return true;
            }
            vars.add(lit);
        }
        return false;
    }

    @Override
    public String toString() {
        return literals.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clause clause = (Clause) o;
        return new HashSet<>(literals).equals(new HashSet<>(clause.literals));
    }

    @Override
    public int hashCode() {
        return new HashSet<>(literals).hashCode();
    }
}
