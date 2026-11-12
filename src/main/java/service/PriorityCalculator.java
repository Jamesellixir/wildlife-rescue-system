package service;

import java.util.Comparator;
import model.PriorityLevel;
import model.RescueIncident;

public final class PriorityCalculator {
    public record Priority(int conditionPoints, int dangerPoints, int countPoints,
                           int riskPoints, int score, PriorityLevel level) { }

    public Comparator<RescueIncident> order() {
        return Comparator.comparing((RescueIncident incident) -> calculate(incident).level())
                .thenComparing(incident -> calculate(incident).score(), Comparator.reverseOrder())
                .thenComparing(RescueIncident::reportedAt)
                .thenComparing(RescueIncident::id);
    }

    public Priority calculate(RescueIncident incident) {
        int condition = switch (incident.animal().condition()) {
            case INJURED -> 30;
            case TRAPPED -> 25;
            case ABANDONED -> 15;
            case DISPLACED -> 10;
        };
        int danger = incident.immediateDanger() ? 40 : 0;
        int count = incident.animal().count() >= 5 ? 10 : incident.animal().count() >= 2 ? 5 : 0;
        int risk = switch (incident.risk()) {
            case LOW -> 0;
            case MEDIUM -> 10;
            case HIGH -> 20;
        };
        int score = condition + danger + count + risk;
        PriorityLevel level;
        if (incident.immediateDanger() || score >= 70) level = PriorityLevel.CRITICAL;
        else if (score >= 45) level = PriorityLevel.HIGH;
        else if (score >= 25) level = PriorityLevel.MEDIUM;
        else level = PriorityLevel.LOW;
        return new Priority(condition, danger, count, risk, score, level);
    }
}
