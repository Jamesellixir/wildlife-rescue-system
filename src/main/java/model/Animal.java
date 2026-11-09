package model;

public abstract class Animal {
    private final Condition condition;
    private final int count;

    protected Animal(Condition condition, int count) {
        this.condition = ModelValidation.required(condition, "Animal condition");
        this.count = ModelValidation.positive(count, "Animal count");
    }

    public Condition condition() {
        return condition;
    }

    public int count() {
        return count;
    }

    public abstract AnimalCategory category();

    public abstract Skill requiredHandlingSkill();
}
