package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Here's our CDCL (Conflict-Driven Clause Learning) SAT solver.
 * It figures out if a boolean formula can be satisfied by guessing values,
 * seeing what those guesses force, and learning from its mistakes.
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
     * The main engine of the solver. It keeps guessing and checking until
     * we either find a working solution or prove that it's completely impossible.
     */
    public boolean solve() {
        // First things first: let's see what we can figure out logically before making any actual guesses (this is level 0).
        Clause conflict = propagate();
        if (conflict != null) return false;

        // Now we dive into the main guess-and-check loop.
        while (true) {
            // Did we successfully assign a value to every variable without crashing?
            // If yes, we win!
            if (assignment.isComplete()) {
                return true;
            }

            // Time to make a guess. We ask our heuristic to pick a variable and decide whether to try 'true' or 'false' first.
            Long var = heuristic.chooseVariable(assignment);
            if (var == null) return true;

            boolean val = heuristic.chooseValue(var);
            assignment.decide(var, val);

            // See what our new guess forces us to do down the line.
            conflict = propagate();
            while (conflict != null) {
                if (assignment.getCurrentLevel() == 0) {
                    return false; // UNSAT
                }

                // Figure out *why* we failed and create a new rule (a learned clause)
                // so we never make this exact same mistake again.
                ImplicationGraph.AnalysisResult result = graph.analyzeConflict(conflict);
                learnedClauses.add(result.learnedClause);

                // Tell our heuristic to prioritize the variables involved in this mess,
                // since they seem to be the tricky ones causing problems.
                heuristic.bumpActivities(result.learnedClause);
                heuristic.decayActivities();

                // Time travel! Jump back up the decision tree to fix the mistake.
                if (result.backtrackLevel < 0) {
                    return false;
                }
                assignment.backtrack(result.backtrackLevel);

                // We've backtracked and learned a new rule. Now let's see what
                // this new information forces us to do before we guess again.
                conflict = propagate();
            }
        }
    }

    /**
     * Boolean Constraint Propagation (BCP).
     * This sweeps through the clauses looking for situations where we have no choice
     * but to set a specific variable to true or false to avoid failing.
     */
    private Clause propagate() {
        boolean hasChange = true;

        while (hasChange) {
            hasChange = false;

            // Check every single clause we know about.
            for (Clause c : getAllClauses()) {
                // If this clause is already satisfied, cool. We can ignore it.
                if (c.isSatisfied(assignment)) {
                    continue;
                }

                // Detect conflict
                if (c.isConflicting(assignment)) {
                    return c;
                }

                // Unit propagation: If there's only one unassigned variable left in
                // the clause, we are FORCED to assign it a specific way to satisfy the clause.
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
     * Grabs everything we know: the original formula we started with,
     * plus all the hard lessons (learned clauses) we picked up along the way.
     */
    private List<Clause> getAllClauses() {
        List<Clause> all = new ArrayList<>(originalClauses);
        all.addAll(learnedClauses);
        return all;
    }

    /**
     * Returns the current state of our variables (which ones are true, false, or still empty).
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Just a handy counter for how many new rules we've generated during this run.
     */
    public int getLearnedClauseCount() {
        return learnedClauses.size();
    }
}
