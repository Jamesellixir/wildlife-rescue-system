package model;

public final class Bird extends Animal {
    public Bird(Condition condition, int count) {
        super(condition, count);
    }

    @Override
    public AnimalCategory category() {
        return AnimalCategory.BIRD;
    }

    @Override
    public Skill requiredHandlingSkill() {
        return Skill.BIRD_HANDLING;
    }
}
