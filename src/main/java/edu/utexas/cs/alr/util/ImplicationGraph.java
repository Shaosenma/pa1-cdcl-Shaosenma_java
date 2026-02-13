package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Conflict analysis using First UIP scheme.
 */
public class ImplicationGraph {
    private final Assignment assign;

    public ImplicationGraph(Assignment assignment) {
        this.assign = assignment;
    }

    /**
     * Analyze conflict and produce learned clause with backtrack level.
     */
    public AnalysisResult analyzeConflict(Clause conflictClause) {
        int curLevel = assign.getCurrentLevel();

        // Initialize with conflict clause
        Set<Long> resolvent = new HashSet<>();
        for (long lit : conflictClause.getLiterals()) {
            resolvent.add(lit);
        }

        // Count current-level variables
        int curLevelVars = countAtLevel(resolvent, curLevel);

        // Resolution until First UIP
        List<Long> trail = assign.getTrail();
        int idx = trail.size() - 1;

        while (curLevelVars > 1 && idx >= 0) {
            long v = trail.get(idx--);

            // Skip if not at current level
            if (assign.getDecisionLevel(v) != curLevel) {
                continue;
            }

            // Find literal in resolvent
            long litInResolvent = 0;
            if (resolvent.contains(v)) {
                litInResolvent = v;
            } else if (resolvent.contains(-v)) {
                litInResolvent = -v;
            } else {
                continue;
            }

            // Get antecedent
            Clause ant = assign.getReason(v);
            if (ant == null) continue; // Decision variable

            // Resolve
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

        // Build learned clause
        Clause learned = new Clause(new ArrayList<>(resolvent));

        // Compute backtrack level
        int btLevel = computeBackjumpLevel(resolvent, curLevel);

        return new AnalysisResult(learned, btLevel);
    }

    /**
     * Count variables at given level.
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
     * Compute backjump level (second highest).
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

        // Return second highest (or highest if not current)
        if (levels.get(0) == curLevel && levels.size() > 1) {
            return levels.get(1);
        }

        return levels.get(0);
    }

    /**
     * Result container for conflict analysis.
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
