package service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import model.Animal;
import model.EnvironmentalRisk;
import model.Equipment;
import model.RescueIncident;
import model.Skill;

public final class IncidentManager {
    private final Map<UUID, RescueIncident> incidents = new LinkedHashMap<>();

    public RescueIncident reportIncident(Animal animal, String location, String zone, boolean immediateDanger,
                                         EnvironmentalRisk risk, Set<Skill> requiredSkills,
                                         Set<Equipment> requiredEquipment, String reporterName,
                                         String reporterContact) {
        UUID id = UUID.randomUUID();
        while (incidents.containsKey(id)) {
            id = UUID.randomUUID();
        }

        RescueIncident incident = new RescueIncident(id, Instant.now(), animal, location, zone,
                immediateDanger, risk, requiredSkills, requiredEquipment, reporterName, reporterContact);
        incidents.put(id, incident);
        return incident;
    }

    public List<RescueIncident> listIncidents() {
        return List.copyOf(incidents.values());
    }

    public Optional<RescueIncident> findIncident(UUID id) {
        return Optional.ofNullable(incidents.get(id));
    }
}
