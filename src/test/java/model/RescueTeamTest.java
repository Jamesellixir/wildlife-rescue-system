package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RescueTeamTest {
    @Test
    void requiresPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> team(List.of("Sample Member"), Set.of(AnimalCategory.BIRD), 0));
        assertThrows(IllegalArgumentException.class, () -> team(List.of("Sample Member"), Set.of(AnimalCategory.BIRD), -1));
    }

    @Test
    void requiresMembersAndAnAnimalCategory() {
        assertThrows(IllegalArgumentException.class, () -> team(List.of(), Set.of(AnimalCategory.BIRD), 1));
        assertThrows(IllegalArgumentException.class, () -> team(List.of("Sample Member"), Set.of(), 1));
    }

    @Test
    void rejectsBlankMemberAndCsvListSeparator() {
        assertThrows(IllegalArgumentException.class, () -> team(List.of(" "), Set.of(AnimalCategory.BIRD), 1));
        assertThrows(IllegalArgumentException.class, () -> team(List.of("Sam|Alex"), Set.of(AnimalCategory.BIRD), 1));
    }

    @Test
    void copiesMemberNamesAndCategories() {
        List<String> names = new ArrayList<>(List.of(" Sample Member "));
        Set<AnimalCategory> categories = new HashSet<>(Set.of(AnimalCategory.BIRD));
        RescueTeam team = team(names, categories, 2);

        names.clear();
        categories.clear();

        assertEquals(List.of("Sample Member"), team.members());
        assertEquals(Set.of(AnimalCategory.BIRD), team.categories());
        assertThrows(UnsupportedOperationException.class, () -> team.members().clear());
    }

    @Test
    void derivesStatusFromAvailabilityAndActiveCount() {
        RescueTeam available = team(List.of("Sample Member"), Set.of(AnimalCategory.BIRD), 2);
        RescueTeam unavailable = available.withAvailability(false);

        assertEquals(TeamStatus.AVAILABLE, available.status(0));
        assertEquals(TeamStatus.ASSIGNED, available.status(1));
        assertEquals(TeamStatus.ASSIGNED, available.status(2));
        assertEquals(TeamStatus.UNAVAILABLE, unavailable.status(0));
        assertThrows(IllegalArgumentException.class, () -> available.status(-1));
        assertThrows(IllegalArgumentException.class, () -> available.status(3));
    }

    private RescueTeam team(List<String> members, Set<AnimalCategory> categories, int capacity) {
        return new RescueTeam("TEAM-1", "Sample Rescue Team", members, categories, Set.of(Skill.BIRD_HANDLING),
                Set.of(Equipment.ANIMAL_CARRIER), "Central", true, capacity);
    }
}
