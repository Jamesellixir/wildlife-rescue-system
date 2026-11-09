package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RescueIncidentTest {
    @Test
    void createsPendingIncidentAndKeepsLocationPunctuation() {
        RescueIncident incident = incident(" Park Road, Colombo ", "reporter@example.test", Set.of());
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT, incident.status());
        assertNull(incident.assignedTeamId());
        assertEquals("Park Road, Colombo", incident.location());
    }

    @Test
    void rejectsBlankLocationAndContact() {
        assertThrows(IllegalArgumentException.class, () -> incident(" ", "reporter@example.test", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> incident("Park Road", "", Set.of()));
    }

    @Test
    void copiesRequiredSkillsAndPreventsDirectChanges() {
        Set<Skill> skills = new HashSet<>(Set.of(Skill.CLIMBING));
        RescueIncident incident = incident("Park Road", "reporter@example.test", skills);

        skills.clear();

        assertEquals(Set.of(Skill.CLIMBING), incident.requiredSkills());
        assertThrows(UnsupportedOperationException.class, () -> incident.requiredSkills().clear());
    }

    @Test
    void recordsEachStatusWithoutChangingPreviousIncidentVersions() {
        RescueIncident pending = incident("Park Road", "reporter@example.test", Set.of());
        RescueIncident assigned = pending.withAssignment(" TEAM-1 ");
        RescueIncident active = assigned.withStatus(IncidentStatus.IN_PROGRESS);
        RescueIncident complete = active.withStatus(IncidentStatus.COMPLETED);

        assertEquals(IncidentStatus.PENDING_ASSIGNMENT, pending.status());
        assertNull(pending.assignedTeamId());
        assertEquals(IncidentStatus.ASSIGNED, assigned.status());
        assertEquals(IncidentStatus.IN_PROGRESS, active.status());
        assertEquals(IncidentStatus.COMPLETED, complete.status());
        assertEquals("TEAM-1", complete.assignedTeamId());
        assertEquals(pending.id(), complete.id());
    }

    @Test
    void rejectsSkippingStagesReassignmentAndReopening() {
        RescueIncident pending = incident("Park Road", "reporter@example.test", Set.of());
        RescueIncident assigned = pending.withAssignment("TEAM-1");
        RescueIncident complete = assigned.withStatus(IncidentStatus.IN_PROGRESS)
                .withStatus(IncidentStatus.COMPLETED);

        assertThrows(IllegalArgumentException.class, () -> pending.withAssignment(" "));
        assertThrows(IllegalStateException.class, () -> pending.withStatus(IncidentStatus.ASSIGNED));
        assertThrows(IllegalStateException.class, () -> assigned.withStatus(IncidentStatus.COMPLETED));
        assertThrows(IllegalStateException.class, () -> assigned.withAssignment("TEAM-2"));
        assertThrows(IllegalStateException.class, () -> complete.withStatus(IncidentStatus.IN_PROGRESS));
    }

    private RescueIncident incident(String location, String contact, Set<Skill> skills) {
        return new RescueIncident(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-11-10T12:00:00Z"), new Bird(Condition.TRAPPED, 1),
                location, "Central", false, EnvironmentalRisk.LOW, skills,
                Set.of(Equipment.ANIMAL_CARRIER), "Sample Reporter", contact);
    }
}
