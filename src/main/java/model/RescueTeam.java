package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RescueTeam {
    private final String id;
    private final String name;
    private final List<String> members;
    private final Set<AnimalCategory> categories;
    private final Set<Skill> skills;
    private final Set<Equipment> equipment;
    private final String baseZone;
    private final boolean operationallyAvailable;
    private final int capacity;

    public RescueTeam(String id, String name, List<String> members, Set<AnimalCategory> categories,
                      Set<Skill> skills, Set<Equipment> equipment, String baseZone,
                      boolean operationallyAvailable, int capacity) {
        this.id = ModelValidation.text(id, "Team ID");
        this.name = ModelValidation.text(name, "Team name");
        this.members = validatedMembers(members);
        this.categories = Set.copyOf(ModelValidation.required(categories, "Animal categories"));
        if (this.categories.isEmpty()) {
            throw new IllegalArgumentException("Select at least one animal category.");
        }
        this.skills = Set.copyOf(ModelValidation.required(skills, "Team skills"));
        this.equipment = Set.copyOf(ModelValidation.required(equipment, "Team equipment"));
        this.baseZone = ModelValidation.text(baseZone, "Base zone");
        this.operationallyAvailable = operationallyAvailable;
        this.capacity = ModelValidation.positive(capacity, "Team capacity");
    }

    private static List<String> validatedMembers(List<String> members) {
        ModelValidation.required(members, "Team members");
        if (members.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one team member.");
        }
        List<String> names = new ArrayList<>();
        for (String member : members) {
            String name = ModelValidation.text(member, "Team member name");
            if (name.contains("|")) {
                throw new IllegalArgumentException("Team member names cannot contain |.");
            }
            names.add(name);
        }
        return List.copyOf(names);
    }

    public RescueTeam withAvailability(boolean available) {
        return new RescueTeam(id, name, members, categories, skills, equipment, baseZone, available, capacity);
    }

    // The manager supplies the active incident count so the team does not store a second workload.
    public TeamStatus status(int activeCount) {
        if (activeCount < 0 || activeCount > capacity) {
            throw new IllegalArgumentException("Active incident count must be between zero and team capacity.");
        }
        if (!operationallyAvailable) {
            return TeamStatus.UNAVAILABLE;
        }
        if (activeCount == 0) {
            return TeamStatus.AVAILABLE;
        }
        return TeamStatus.ASSIGNED;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<String> members() {
        return members;
    }

    public Set<AnimalCategory> categories() {
        return categories;
    }

    public Set<Skill> skills() {
        return skills;
    }

    public Set<Equipment> equipment() {
        return equipment;
    }

    public String baseZone() {
        return baseZone;
    }

    public boolean operationallyAvailable() {
        return operationallyAvailable;
    }

    public int capacity() {
        return capacity;
    }
}
