package model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class RescueIncident {
    private final UUID id;
    private final Instant reportedAt;
    private final Animal animal;
    private final String location;
    private final String zone;
    private final boolean immediateDanger;
    private final EnvironmentalRisk risk;
    private final Set<Skill> requiredSkills;
    private final Set<Equipment> requiredEquipment;
    private final String reporterName;
    private final String reporterContact;
    private final IncidentStatus status;
    private final String assignedTeamId;

    public RescueIncident(UUID id, Instant reportedAt, Animal animal, String location, String zone,
                          boolean immediateDanger, EnvironmentalRisk risk, Set<Skill> requiredSkills,
                          Set<Equipment> requiredEquipment, String reporterName, String reporterContact) {
        this(id, reportedAt, animal, location, zone, immediateDanger, risk, requiredSkills,
                requiredEquipment, reporterName, reporterContact, IncidentStatus.PENDING_ASSIGNMENT, null);
    }

    private RescueIncident(UUID id, Instant reportedAt, Animal animal, String location, String zone,
                           boolean immediateDanger, EnvironmentalRisk risk, Set<Skill> requiredSkills,
                           Set<Equipment> requiredEquipment, String reporterName, String reporterContact,
                           IncidentStatus status, String assignedTeamId) {
        this.id = ModelValidation.required(id, "Incident ID");
        this.reportedAt = ModelValidation.required(reportedAt, "Report time");
        this.animal = ModelValidation.required(animal, "Animal");
        this.location = ModelValidation.text(location, "Location");
        this.zone = ModelValidation.text(zone, "Zone");
        this.immediateDanger = immediateDanger;
        this.risk = ModelValidation.required(risk, "Environmental risk");
        this.requiredSkills = Set.copyOf(ModelValidation.required(requiredSkills, "Required skills"));
        this.requiredEquipment = Set.copyOf(ModelValidation.required(requiredEquipment, "Required equipment"));
        this.reporterName = ModelValidation.text(reporterName, "Reporter name");
        this.reporterContact = ModelValidation.text(reporterContact, "Reporter contact");
        this.status = status;
        this.assignedTeamId = assignedTeamId;
    }

    public RescueIncident withAssignment(String teamId) {
        if (status != IncidentStatus.PENDING_ASSIGNMENT) {
            throw new IllegalStateException("Only pending incidents can be assigned.");
        }
        return copyWith(IncidentStatus.ASSIGNED, ModelValidation.text(teamId, "Team ID"));
    }

    public RescueIncident withStatus(IncidentStatus nextStatus) {
        boolean startsRescue = status == IncidentStatus.ASSIGNED && nextStatus == IncidentStatus.IN_PROGRESS;
        boolean completesRescue = status == IncidentStatus.IN_PROGRESS && nextStatus == IncidentStatus.COMPLETED;
        if (!startsRescue && !completesRescue) {
            throw new IllegalStateException("Cannot change incident status from " + status + " to " + nextStatus + ".");
        }
        return copyWith(nextStatus, assignedTeamId);
    }

    // The manager can save the updated incident before replacing the original.
    private RescueIncident copyWith(IncidentStatus nextStatus, String teamId) {
        return new RescueIncident(id, reportedAt, animal, location, zone, immediateDanger, risk,
                requiredSkills, requiredEquipment, reporterName, reporterContact, nextStatus, teamId);
    }

    public UUID id() {
        return id;
    }

    public Instant reportedAt() {
        return reportedAt;
    }

    public Animal animal() {
        return animal;
    }

    public String location() {
        return location;
    }

    public String zone() {
        return zone;
    }

    public boolean immediateDanger() {
        return immediateDanger;
    }

    public EnvironmentalRisk risk() {
        return risk;
    }

    public Set<Skill> requiredSkills() {
        return requiredSkills;
    }

    public Set<Equipment> requiredEquipment() {
        return requiredEquipment;
    }

    public String reporterName() {
        return reporterName;
    }

    public String reporterContact() {
        return reporterContact;
    }

    public IncidentStatus status() {
        return status;
    }

    public String assignedTeamId() {
        return assignedTeamId;
    }
}
