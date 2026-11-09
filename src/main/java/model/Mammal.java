package model;

public final class Mammal extends Animal {
    public Mammal(Condition condition, int count) {
        super(condition, count);
    }

    @Override
    public AnimalCategory category() {
        return AnimalCategory.MAMMAL;
    }

    @Override
    public Skill requiredHandlingSkill() {
        return Skill.MAMMAL_HANDLING;
    }
}
