package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import model.Bird;
import model.Condition;
import model.EnvironmentalRisk;
import model.IncidentStatus;
import model.RescueIncident;
import org.junit.jupiter.api.Test;

class IncidentManagerTest {
    @Test
    void recordsDistinctPendingReportsAndFindsThemById() {
        IncidentManager manager = new IncidentManager();
        RescueIncident first = report(manager, "Park Road");
        RescueIncident second = report(manager, "Station Road");

        assertNotEquals(first.id(), second.id());
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT, first.status());
        assertEquals(first, manager.findIncident(first.id()).orElseThrow());
        assertEquals(second, manager.listIncidents().get(1));
        assertTrue(manager.findIncident(new UUID(0, 0)).isEmpty());
    }

    @Test
    void rejectsInvalidReportWithoutAddingItToTheSession() {
        IncidentManager manager = new IncidentManager();
        RescueIncident valid = report(manager, "Park Road");

        assertThrows(IllegalArgumentException.class, () -> report(manager, " "));
        assertEquals(java.util.List.of(valid), manager.listIncidents());
    }

    private RescueIncident report(IncidentManager manager, String location) {
        return manager.reportIncident(new Bird(Condition.TRAPPED, 1), location, "Central",
                false, EnvironmentalRisk.LOW, Set.of(), Set.of(), "Sample Reporter", "reporter@example.test");
    }
}
