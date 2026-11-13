package service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import model.Animal;
import model.EnvironmentalRisk;
import model.Equipment;
import model.RescueIncident;
import model.RescueTeam;
import model.IncidentStatus;
import model.Skill;

public final class IncidentManager {
    private final Map<UUID, RescueIncident> incidents = new LinkedHashMap<>();

    public synchronized RescueIncident reportIncident(Animal animal, String location, String zone, boolean immediateDanger,
                                         EnvironmentalRisk risk, Set<Skill> requiredSkills,
                                         Set<Equipment> requiredEquipment, String reporterName,
                                         String reporterContact) {
        UUID id = UUID.randomUUID();
        while (incidents.containsKey(id)) {
            id = UUID.randomUUID();
        }

        RescueIncident incident = new RescueIncident(id, Instant.now(), animal, location, zone,
                immediateDanger, risk, requiredSkills, requiredEquipment, reporterName, reporterContact);
        incidents.put(id, incident);
        return incident;
    }

    public synchronized List<RescueIncident> listIncidents() {
        return List.copyOf(incidents.values());
    }

    public synchronized List<RescueIncident> pendingInPriorityOrder() {
        PriorityCalculator calculator = new PriorityCalculator();
        return incidents.values().stream()
                .filter(incident -> incident.status() == IncidentStatus.PENDING_ASSIGNMENT)
                .sorted(calculator.order())
                .toList();
    }

    public synchronized RescueIncident assign(UUID incidentId, String teamId, TeamRegistry registry) {
        if (registry == null || !registry.belongsTo(this)) {
            throw new IllegalArgumentException("Team registry does not belong to this incident manager.");
        }
        RescueIncident incident = incidents.get(incidentId);
        if (incident == null) throw new IllegalArgumentException("No incident found for that ID.");
        if (incident.status() != IncidentStatus.PENDING_ASSIGNMENT) {
            throw new IllegalStateException("The incident is not pending assignment.");
        }
        RescueTeam team = registry.findTeam(teamId)
                .orElseThrow(() -> new IllegalArgumentException("No team found for that ID."));
        var result = new TeamMatcher().match(incident, List.of(team), List.copyOf(incidents.values()));
        if (result.candidates().isEmpty()) {
            throw new IllegalStateException("Team is not suitable: " + String.join(", ", result.rejected().get(0).reasons()));
        }
        RescueIncident assigned = incident.withAssignment(team.id());
        incidents.put(incidentId, assigned);
        return assigned;
    }

    public synchronized RescueIncident updateStatus(UUID incidentId, IncidentStatus nextStatus) {
        RescueIncident current = incidents.get(incidentId);
        if (current == null) throw new IllegalArgumentException("No incident found for that ID.");
        RescueIncident updated = current.withStatus(nextStatus);
        incidents.put(incidentId, updated);
        return updated;
    }

    public synchronized Optional<RescueIncident> findIncident(UUID id) {
        return Optional.ofNullable(incidents.get(id));
    }
}
