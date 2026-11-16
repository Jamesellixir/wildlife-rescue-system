package service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import model.IncidentStatus;
import model.RescueIncident;

public final class RescueReportService {
    public record Summary(Map<IncidentStatus, Long> counts, int total) {
        public long count(IncidentStatus status) { return counts.getOrDefault(status, 0L); }
        public long active() { return count(IncidentStatus.ASSIGNED) + count(IncidentStatus.IN_PROGRESS); }
    }

    public Summary summarize(List<RescueIncident> incidents) {
        Map<IncidentStatus, Long> counts = new EnumMap<>(IncidentStatus.class);
        for (var status : IncidentStatus.values()) counts.put(status, 0L);
        for (var incident : incidents) counts.merge(incident.status(), 1L, Long::sum);
        return new Summary(Map.copyOf(counts), incidents.size());
    }

    public List<List<String>> rows(List<RescueIncident> incidents) {
        var calculator = new PriorityCalculator();
        List<List<String>> rows = new ArrayList<>();
        for (var i : incidents) {
            var priority = calculator.calculate(i);
            rows.add(List.of(i.id().toString(), i.reportedAt().toString(), i.animal().category().name(),
                    i.animal().condition().name(), Integer.toString(i.animal().count()), i.location(), i.zone(),
                    priority.level().name(), Integer.toString(priority.score()), i.status().name(),
                    i.assignedTeamId() == null ? "" : i.assignedTeamId()));
        }
        return List.copyOf(rows);
    }
}
