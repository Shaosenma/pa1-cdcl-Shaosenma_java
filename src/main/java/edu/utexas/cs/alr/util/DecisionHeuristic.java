package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * A VSIDS-based heuristic. It basically tracks which variables are
 * involved in the most recent conflicts and prioritizes them.
 */
public class DecisionHeuristic {
    private final Map<Long, Double> scores;
    private double increment;

    private static final double DECAY = 0.95;
    private static final double INIT_INCREMENT = 1.0;
    private static final double RESCALE_THRESHOLD = 1e100;
    private static final double RESCALE_FACTOR = 1e-100;

    public DecisionHeuristic(Set<Long> variables) {
        this.scores = new HashMap<>();
        this.increment = INIT_INCREMENT;

        // Initialize to zero
        for (long v : variables) {
            scores.put(v, 0.0);
        }
    }

    /**
     * Scans through the unassigned variables and picks the one
     * with the highest activity score.
     */
    public Long chooseVariable(Assignment assign) {
        Set<Long> unassigned = assign.getUnassignedVariables();
        if (unassigned.isEmpty()) return null;

        long best = -1;
        double bestScore = -1.0;

        for (long v : unassigned) {
            double s = scores.getOrDefault(v, 0.0);
            if (s > bestScore) {
                bestScore = s;
                best = v;
            }
        }

        return best;
    }

    /**
     * Decides whether to try true or false first. We're keeping
     * it super simple here and just defaulting to false.
     */
    public boolean chooseValue(long varId) {
        return false;
    }

    /**
     * Bumps up a single variable's score by the current increment step.
     */
    public void bumpActivity(long varId) {
        if (!scores.containsKey(varId)) return;

        double newScore = scores.get(varId) + increment;
        scores.put(varId, newScore);

        if (newScore > RESCALE_THRESHOLD) {
            rescaleScores();
        }
    }

    /**
     * Bumps the scores for every variable sitting inside a specific
     * clause (usually a newly learned conflict clause).
     */
    public void bumpActivities(Clause c) {
        for (long lit : c.getLiterals()) {
            bumpActivity(Math.abs(lit));
        }
    }

    /**
     * Simulates decay. Instead of dividing all existing scores (which
     * is slow), we just make the increment larger. Same math, way less work!
     */
    public void decayActivities() {
        increment /= DECAY;
    }

    /**
     * Scales all scores and the increment down if they get dangerously
     * close to double overflow.
     */
    private void rescaleScores() {
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            entry.setValue(entry.getValue() * RESCALE_FACTOR);
        }
        increment *= RESCALE_FACTOR;
    }

    @Override
    public String toString() {
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder("Heuristic[top10={");
        int count = Math.min(10, sorted.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<Long, Double> e = sorted.get(i);
            sb.append("x").append(e.getKey()).append(":");
            sb.append(String.format("%.2f", e.getValue()));
            if (i < count - 1) sb.append(", ");
        }
        sb.append("}]");
        return sb.toString();
    }
}
