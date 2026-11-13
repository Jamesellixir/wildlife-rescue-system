package service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import model.IncidentStatus;
import model.RescueIncident;
import model.RescueTeam;

public final class TeamRegistry {
    private final Map<String, RescueTeam> teams = new LinkedHashMap<>();
    private final IncidentManager incidents;

    public TeamRegistry(IncidentManager incidents) {
        this.incidents = Objects.requireNonNull(incidents);
    }

    boolean belongsTo(IncidentManager manager) {
        return incidents == manager;
    }

    public void register(RescueTeam team) {
        synchronized (incidents) {
            Objects.requireNonNull(team);
            if (teams.putIfAbsent(team.id(), team) != null) {
                throw new IllegalArgumentException("A team with this ID already exists.");
            }
        }
    }

    public List<RescueTeam> listTeams() {
        synchronized (incidents) {
            return List.copyOf(teams.values());
        }
    }

    public Optional<RescueTeam> findTeam(String id) {
        synchronized (incidents) {
            return Optional.ofNullable(teams.get(id));
        }
    }

    public int activeCount(String id) {
        synchronized (incidents) {
            return (int) incidents.listIncidents().stream()
                    .filter(i -> id.equals(i.assignedTeamId()))
                    .filter(i -> i.status() == IncidentStatus.ASSIGNED || i.status() == IncidentStatus.IN_PROGRESS)
                    .count();
        }
    }

    public RescueTeam changeAvailability(String id, boolean available) {
        synchronized (incidents) {
            RescueTeam team = teams.get(id);
            if (team == null) throw new IllegalArgumentException("No team found for that ID.");
            if (!available && activeCount(id) > 0) {
                throw new IllegalStateException("Complete active incidents before marking the team unavailable.");
            }
            RescueTeam updated = team.withAvailability(available);
            teams.put(id, updated);
            return updated;
        }
    }
}
