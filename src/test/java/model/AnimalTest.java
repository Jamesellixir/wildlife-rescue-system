package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AnimalTest {
    @Test
    void eachAnimalSuppliesItsCategoryAndHandlingSkill() {
        assertAnimal(new Bird(Condition.INJURED, 2), AnimalCategory.BIRD, Skill.BIRD_HANDLING);
        assertAnimal(new Reptile(Condition.INJURED, 2), AnimalCategory.REPTILE, Skill.REPTILE_HANDLING);
        assertAnimal(new Mammal(Condition.INJURED, 2), AnimalCategory.MAMMAL, Skill.MAMMAL_HANDLING);
    }

    @Test
    void rejectsZeroAndNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new Bird(Condition.TRAPPED, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reptile(Condition.TRAPPED, -1));
    }

    @Test
    void requiresAnAnimalCondition() {
        assertThrows(IllegalArgumentException.class, () -> new Mammal(null, 1));
    }

    private void assertAnimal(Animal animal, AnimalCategory category, Skill skill) {
        assertEquals(category, animal.category());
        assertEquals(skill, animal.requiredHandlingSkill());
        assertEquals(Condition.INJURED, animal.condition());
        assertEquals(2, animal.count());
    }
}
