package model;

public final class Reptile extends Animal {
    public Reptile(Condition condition, int count) {
        super(condition, count);
    }

    @Override
    public AnimalCategory category() {
        return AnimalCategory.REPTILE;
    }

    @Override
    public Skill requiredHandlingSkill() {
        return Skill.REPTILE_HANDLING;
    }
}
