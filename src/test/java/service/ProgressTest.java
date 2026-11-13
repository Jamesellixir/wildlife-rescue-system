package service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import model.*;
import org.junit.jupiter.api.Test;

class ProgressTest {
    private RescueIncident report(IncidentManager m) {
        return m.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,
                EnvironmentalRisk.LOW,Set.of(),Set.of(),"Reporter","contact");
    }
    private TeamRegistry teams(IncidentManager m) {
        var registry=new TeamRegistry(m);
        registry.register(new RescueTeam("T1","Team",List.of("Alex"),Set.of(AnimalCategory.BIRD),
                Set.of(Skill.BIRD_HANDLING),Set.of(),"North",true,1));
        return registry;
    }
    @Test void progressesAndReleasesCapacityExactlyOnce() {
        var m=new IncidentManager();var teams=teams(m);var first=report(m);var second=report(m);
        m.assign(first.id(),"T1",teams);assertEquals(1,teams.activeCount("T1"));
        assertEquals(IncidentStatus.IN_PROGRESS,m.updateStatus(first.id(),IncidentStatus.IN_PROGRESS).status());
        assertEquals(1,teams.activeCount("T1"));
        assertThrows(IllegalStateException.class,()->teams.changeAvailability("T1",false));
        assertEquals(IncidentStatus.COMPLETED,m.updateStatus(first.id(),IncidentStatus.COMPLETED).status());
        assertEquals(0,teams.activeCount("T1"));
        assertThrows(IllegalStateException.class,()->m.updateStatus(first.id(),IncidentStatus.COMPLETED));
        assertThrows(IllegalStateException.class,()->m.updateStatus(first.id(),IncidentStatus.IN_PROGRESS));
        assertEquals(0,teams.activeCount("T1"));assertEquals(2,m.listIncidents().size());
        assertEquals(IncidentStatus.ASSIGNED,m.assign(second.id(),"T1",teams).status());
        assertEquals(1,teams.activeCount("T1"));
    }
    @Test void rejectsInvalidTransitionsAndLeavesStateIntact() {
        var m=new IncidentManager();var teams=teams(m);var i=report(m);
        assertThrows(IllegalStateException.class,()->m.updateStatus(i.id(),IncidentStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class,()->m.updateStatus(i.id(),IncidentStatus.COMPLETED));
        assertThrows(IllegalArgumentException.class,()->m.updateStatus(UUID.randomUUID(),IncidentStatus.IN_PROGRESS));
        assertEquals(i,m.findIncident(i.id()).orElseThrow());
        m.assign(i.id(),"T1",teams);
        assertThrows(IllegalStateException.class,()->m.updateStatus(i.id(),IncidentStatus.COMPLETED));
        assertThrows(IllegalStateException.class,()->m.updateStatus(i.id(),IncidentStatus.ASSIGNED));
        assertEquals(1,teams.activeCount("T1"));
        assertEquals(IncidentStatus.ASSIGNED,m.findIncident(i.id()).orElseThrow().status());
    }
}
