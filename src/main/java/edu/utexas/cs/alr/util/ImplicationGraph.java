package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Handles figuring out what went wrong when we hit a conflict.
 * Specifically, it uses the First UIP (Unique Implication Point) method
 * to trace the root cause and learn a new clause so we don't make the same mistake twice.
 */
public class ImplicationGraph {
    private final Assignment assign;

    public ImplicationGraph(Assignment assignment) {
        this.assign = assignment;
    }

    /**
     * Digs into a conflict to build a shiny new learned clause,
     * and figures out exactly how far back in time we need to jump to fix it.
     */
    public AnalysisResult analyzeConflict(Clause conflictClause) {
        int curLevel = assign.getCurrentLevel();

        // Start off with the literals from the clause that just blew up
        Set<Long> resolvent = new HashSet<>();
        for (long lit : conflictClause.getLiterals()) {
            resolvent.add(lit);
        }

        // How many of these literals belong to the decision level we're currently on?
        int curLevelVars = countAtLevel(resolvent, curLevel);

        // Walk backwards through the assignment trail until we hit the First UIP.
        // We know we've hit it when there's only one current-level variable left in our resolvent.
        List<Long> trail = assign.getTrail();
        int idx = trail.size() - 1;

        while (curLevelVars > 1 && idx >= 0) {
            long v = trail.get(idx--);

            // If this variable was set at an earlier level, we don't care about it right now
            if (assign.getDecisionLevel(v) != curLevel) {
                continue;
            }

            // Check if this variable is actually in our melting pot (resolvent), and grab its sign
            long litInResolvent = 0;
            if (resolvent.contains(v)) {
                litInResolvent = v;
            } else if (resolvent.contains(-v)) {
                litInResolvent = -v;
            } else {
                continue;
            }

            // Grab the clause that forced this variable to be set in the first place
            Clause ant = assign.getReason(v);
            if (ant == null) continue; // Decision variable

            // Do the actual resolution: yank out the resolved literal...
            resolvent.remove(litInResolvent);
            curLevelVars--;

            for (long antLit : ant.getLiterals()) {
                long antVar = Math.abs(antLit);
                if (antVar != v) {
                    if (!resolvent.contains(antLit) && !resolvent.contains(-antLit)) {
                        resolvent.add(antLit);
                        if (assign.getDecisionLevel(antVar) == curLevel) {
                            curLevelVars++;
                        }
                    }
                }
            }
        }

        // Package the final resolvent into a brand new learned clause
        Clause learned = new Clause(new ArrayList<>(resolvent));

        // Figure out how far back up the decision tree we need to jump
        int btLevel = computeBackjumpLevel(resolvent, curLevel);

        return new AnalysisResult(learned, btLevel);
    }

    /**
     * Helper to tally up how many literals in our set belong to a specific decision level.
     */
    private int countAtLevel(Set<Long> literals, int level) {
        int cnt = 0;
        for (long lit : literals) {
            long v = Math.abs(lit);
            Integer lvl = assign.getDecisionLevel(v);
            if (lvl != null && lvl == level) {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Calculates where to jump back to. In standard CDCL, we want to jump back
     * to the second highest decision level present in the learned clause.
     * This turns our learned clause into an asserting clause at that target level!
     */
    private int computeBackjumpLevel(Set<Long> literals, int curLevel) {
        Set<Integer> levelsSet = new HashSet<>();

        for (long lit : literals) {
            long v = Math.abs(lit);
            Integer lvl = assign.getDecisionLevel(v);
            if (lvl != null) {
                levelsSet.add(lvl);
            }
        }

        List<Integer> levels = new ArrayList<>(levelsSet);
        Collections.sort(levels, Collections.reverseOrder());

        if (levels.size() <= 1) return 0;

        // Grab the second highest level, assuming the highest is our current one
        if (levels.get(0) == curLevel && levels.size() > 1) {
            return levels.get(1);
        }

        return levels.get(0);
    }

    /**
     * Just a simple data box to hand back our newly learned clause
     * along with the level we need to backtrack to.
     */
    public static class AnalysisResult {
        public final Clause learnedClause;
        public final int backtrackLevel;

        public AnalysisResult(Clause clause, int level) {
            this.learnedClause = clause;
            this.backtrackLevel = level;
        }
    }
}
