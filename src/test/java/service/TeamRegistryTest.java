package service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import model.*;
import org.junit.jupiter.api.Test;

class TeamRegistryTest {
    private RescueTeam team() {
        return new RescueTeam("T1", "North", List.of("Alex"), Set.of(AnimalCategory.BIRD),
                Set.of(Skill.BIRD_HANDLING), Set.of(), "North", true, 2);
    }

    @Test
    void registersListsAndRejectsDuplicateIds() {
        TeamRegistry registry = new TeamRegistry(new IncidentManager());
        RescueTeam original = team();
        registry.register(original);
        assertThrows(IllegalArgumentException.class, () -> registry.register(team()));
        assertEquals(List.of(original), registry.listTeams());
        assertThrows(UnsupportedOperationException.class, () -> registry.listTeams().clear());
        assertEquals(original, registry.findTeam("T1").orElseThrow());
    }

    @Test
    void changesAvailabilityAndRejectsUnknownIds() {
        TeamRegistry registry = new TeamRegistry(new IncidentManager());
        registry.register(team());
        assertFalse(registry.changeAvailability("T1", false).operationallyAvailable());
        assertTrue(registry.changeAvailability("T1", true).operationallyAvailable());
        assertThrows(IllegalArgumentException.class, () -> registry.changeAvailability("missing", false));
    }
    @Test
    void rejectsUnavailableWhenTeamHasActiveIncident() throws Exception {
        IncidentManager manager = new IncidentManager();
        TeamRegistry registry = new TeamRegistry(manager);
        registry.register(team());
        RescueIncident incident = manager.reportIncident(new Bird(Condition.INJURED, 1), "Park", "North",
                false, EnvironmentalRisk.LOW, Set.of(), Set.of(), "Alex", "alex@example.test");
        var field = IncidentManager.class.getDeclaredField("incidents");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var stored = (java.util.Map<java.util.UUID, RescueIncident>) field.get(manager);
        stored.put(incident.id(), incident.withAssignment("T1"));
        assertEquals(1, registry.activeCount("T1"));
        assertThrows(IllegalStateException.class, () -> registry.changeAvailability("T1", false));
        assertTrue(registry.findTeam("T1").orElseThrow().operationallyAvailable());
    }
}
