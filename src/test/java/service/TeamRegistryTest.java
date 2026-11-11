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
}
