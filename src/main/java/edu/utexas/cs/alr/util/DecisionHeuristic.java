package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * VSIDS-based variable selection heuristic.
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

        // Initialize all scores to zero
        for (long v : variables) {
            scores.put(v, 0.0);
        }
    }

    /**
     * Select unassigned variable with highest score.
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
     * Choose value for variable (simple: false first).
     */
    public boolean chooseValue(long varId) {
        return false;
    }

    /**
     * Increase score for single variable.
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
     * Increase scores for all variables in clause.
     */
    public void bumpActivities(Clause c) {
        for (long lit : c.getLiterals()) {
            bumpActivity(Math.abs(lit));
        }
    }

    /**
     * Decay all scores over time.
     */
    public void decayActivities() {
        increment /= DECAY;
    }

    /**
     * Rescale to prevent overflow.
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
