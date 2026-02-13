package edu.utexas.cs.alr.util;

import java.util.*;

/**
 * Decision heuristic for choosing variables and values.
 * Uses a VSIDS-like (Variable State Independent Decaying Sum) approach.
 */
public class DecisionHeuristic {
    // Activity scores for each variable
    private final Map<Long, Double> activities;

    // Increment value for activity bumps
    private double activityIncrement;

    // Decay factor for activity scores
    private static final double DECAY_FACTOR = 0.95;

    // Initial activity increment
    private static final double INITIAL_INCREMENT = 1.0;

    public DecisionHeuristic(Set<Long> variables) {
        this.activities = new HashMap<>();
        this.activityIncrement = INITIAL_INCREMENT;

        // Initialize all variables with activity 0
        for (long var : variables) {
            activities.put(var, 0.0);
        }
    }

    /**
     * Choose the next unassigned variable with the highest activity.
     */
    public Long chooseVariable(Assignment assignment) {
        Set<Long> unassigned = assignment.getUnassignedVariables();

        if (unassigned.isEmpty()) {
            return null;
        }

        // Find the variable with the highest activity
        long bestVar = -1;
        double bestActivity = -1.0;

        for (long var : unassigned) {
            double activity = activities.getOrDefault(var, 0.0);
            if (activity > bestActivity) {
                bestActivity = activity;
                bestVar = var;
            }
        }

        return bestVar;
    }

    /**
     * Choose a value for the given variable.
     * Simple heuristic: try false first.
     */
    public boolean chooseValue(long var) {
        // Simple heuristic: try false first
        return false;
    }

    /**
     * Increase the activity of a variable (called when it appears in a conflict).
     */
    public void bumpActivity(long var) {
        if (!activities.containsKey(var)) {
            return;
        }

        double newActivity = activities.get(var) + activityIncrement;
        activities.put(var, newActivity);

        // Normalize if activities get too large
        if (newActivity > 1e100) {
            rescaleActivities();
        }
    }

    /**
     * Bump activities for all variables in a clause.
     */
    public void bumpActivities(Clause clause) {
        for (long lit : clause.getLiterals()) {
            long var = Math.abs(lit);
            bumpActivity(var);
        }
    }

    /**
     * Decay all activity scores.
     */
    public void decayActivities() {
        activityIncrement /= DECAY_FACTOR;
    }

    /**
     * Rescale all activities to prevent overflow.
     */
    private void rescaleActivities() {
        for (Map.Entry<Long, Double> entry : activities.entrySet()) {
            entry.setValue(entry.getValue() * 1e-100);
        }
        activityIncrement *= 1e-100;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DecisionHeuristic(activities={");

        // Sort by activity for readability
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(activities.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map.Entry<Long, Double> entry = sorted.get(i);
            sb.append("x").append(entry.getKey()).append("=").append(String.format("%.2f", entry.getValue()));
            if (i < sorted.size() - 1) {
                sb.append(", ");
            }
        }

        if (sorted.size() > 10) {
            sb.append(", ...");
        }

        sb.append("})");
        return sb.toString();
    }
}
