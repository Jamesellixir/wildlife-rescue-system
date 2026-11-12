package service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import model.*;
import org.junit.jupiter.api.Test;

class TeamMatcherTest {
    private final TeamMatcher matcher = new TeamMatcher();
    private RescueIncident incident() {
        return new RescueIncident(UUID.fromString("00000000-0000-0000-0000-000000000001"), Instant.EPOCH,
                new Bird(Condition.TRAPPED, 1), "Roof", "North", false, EnvironmentalRisk.LOW,
                Set.of(Skill.CLIMBING), Set.of(Equipment.CLIMBING_EQUIPMENT), "Reporter", "contact");
    }
    private RescueTeam team(String id, String zone, boolean available, int capacity,
                            Set<AnimalCategory> cats, Set<Skill> skills, Set<Equipment> equipment) {
        return new RescueTeam(id, id, List.of("Alex"), cats, skills, equipment, zone, available, capacity);
    }
    private RescueTeam eligible(String id, String zone, int capacity) {
        return team(id, zone, true, capacity, Set.of(AnimalCategory.BIRD),
                Set.of(Skill.BIRD_HANDLING, Skill.CLIMBING), Set.of(Equipment.CLIMBING_EQUIPMENT));
    }
    @Test void excludesEveryUnsuitableTeamAndExplainsWhy() {
        var wrongAnimal=team("animal", "North", true, 2, Set.of(AnimalCategory.MAMMAL), Set.of(Skill.BIRD_HANDLING, Skill.CLIMBING), Set.of(Equipment.CLIMBING_EQUIPMENT));
        var missingHandling=team("handling", "North", true, 2, Set.of(AnimalCategory.BIRD), Set.of(Skill.CLIMBING), Set.of(Equipment.CLIMBING_EQUIPMENT));
        var missingSkill=team("skill", "North", true, 2, Set.of(AnimalCategory.BIRD), Set.of(Skill.BIRD_HANDLING), Set.of(Equipment.CLIMBING_EQUIPMENT));
        var missingKit=team("kit", "North", true, 2, Set.of(AnimalCategory.BIRD), Set.of(Skill.BIRD_HANDLING, Skill.CLIMBING), Set.of());
        var unavailable=team("away", "North", false, 2, Set.of(AnimalCategory.BIRD), Set.of(Skill.BIRD_HANDLING, Skill.CLIMBING), Set.of(Equipment.CLIMBING_EQUIPMENT));
        var full=eligible("full", "North", 1);var active=incident().withAssignment("full");
        var result=matcher.match(incident(),List.of(wrongAnimal,missingHandling,missingSkill,missingKit,unavailable,full),List.of(active));
        assertTrue(result.candidates().isEmpty()); assertEquals(6,result.rejected().size());
        assertTrue(result.rejected().get(0).reasons().contains("animal category not supported"));
        assertTrue(result.rejected().get(1).reasons().contains("required skills missing"));
        assertTrue(result.rejected().get(2).reasons().contains("required skills missing"));
        assertTrue(result.rejected().get(3).reasons().contains("required equipment missing"));
        assertTrue(result.rejected().get(4).reasons().contains("team unavailable"));
        assertTrue(result.rejected().get(5).reasons().contains("team at capacity"));
    }
    @Test void ranksByZoneThenWorkloadRatioThenId() {
        var localBusy=eligible("B","north",4);var localFree=eligible("A","North",2);
        var localTie=eligible("C","North",4);var remote=eligible("D","South",4);
        var assigned=incident().withAssignment("B"); var completed=incident().withAssignment("A").withStatus(IncidentStatus.IN_PROGRESS).withStatus(IncidentStatus.COMPLETED);
        var result=matcher.match(incident(),List.of(remote,localTie,localBusy,localFree),List.of(assigned,completed));
        assertEquals(List.of("A","C","B","D"),result.candidates().stream().map(c->c.team().id()).toList());
        assertEquals(1,result.candidates().get(2).activeCount()); assertTrue(result.candidates().get(0).sameZone());
    }
    @Test void acceptsEmptyInputsWithoutChangingIncidentOrReservingTeams() {
        var input=incident(); var result=matcher.match(input,List.of(),List.of());
        assertTrue(result.candidates().isEmpty());assertTrue(result.rejected().isEmpty());
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT,input.status()); assertNull(input.assignedTeamId());
        assertThrows(IllegalArgumentException.class,()->matcher.match(input.withAssignment("A"),List.of(),List.of()));
    }
}
