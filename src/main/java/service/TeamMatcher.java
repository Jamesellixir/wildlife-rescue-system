package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import model.IncidentStatus;
import model.RescueIncident;
import model.RescueTeam;
import model.Skill;

public final class TeamMatcher {
    public record Candidate(RescueTeam team, int activeCount, boolean sameZone) { }
    public record Rejection(RescueTeam team, List<String> reasons) { }
    public record Result(List<Candidate> candidates, List<Rejection> rejected) { }

    public Result match(RescueIncident incident, List<RescueTeam> teams, List<RescueIncident> incidents) {
        if (incident.status() != IncidentStatus.PENDING_ASSIGNMENT) {
            throw new IllegalArgumentException("The incident is not pending assignment.");
        }
        List<Candidate> candidates = new ArrayList<>();
        List<Rejection> rejected = new ArrayList<>();
        Set<Skill> skills = EnumSet.noneOf(Skill.class);
        skills.add(incident.animal().requiredHandlingSkill());
        skills.addAll(incident.requiredSkills());
        for (RescueTeam team : teams) {
            int active = (int) incidents.stream()
                    .filter(i -> team.id().equals(i.assignedTeamId()))
                    .filter(i -> i.status() == IncidentStatus.ASSIGNED || i.status() == IncidentStatus.IN_PROGRESS)
                    .count();
            List<String> reasons = new ArrayList<>();
            if (!team.categories().contains(incident.animal().category())) reasons.add("animal category not supported");
            if (!team.skills().containsAll(skills)) reasons.add("required skills missing");
            if (!team.equipment().containsAll(incident.requiredEquipment())) reasons.add("required equipment missing");
            if (!team.operationallyAvailable()) reasons.add("team unavailable");
            if (active >= team.capacity()) reasons.add("team at capacity");
            if (reasons.isEmpty()) candidates.add(new Candidate(team, active, team.baseZone().equalsIgnoreCase(incident.zone())));
            else rejected.add(new Rejection(team, List.copyOf(reasons)));
        }
        candidates.sort(Comparator.comparing(Candidate::sameZone).reversed()
                .thenComparing((a, b) -> Long.compare((long) a.activeCount() * b.team().capacity(),
                        (long) b.activeCount() * a.team().capacity()))
                .thenComparing(c -> c.team().id()));
        return new Result(List.copyOf(candidates), List.copyOf(rejected));
    }
}
