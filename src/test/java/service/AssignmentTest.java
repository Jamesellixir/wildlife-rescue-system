package service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import model.*;
import org.junit.jupiter.api.Test;

class AssignmentTest {
    private RescueTeam team(int capacity) {
        return new RescueTeam("T1", "Team", List.of("Alex"), Set.of(AnimalCategory.BIRD),
                Set.of(Skill.BIRD_HANDLING), Set.of(), "North", true, capacity);
    }
    private RescueIncident report(IncidentManager manager) {
        return manager.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,
                EnvironmentalRisk.LOW,Set.of(),Set.of(),"Reporter","contact");
    }
    @Test void assignsAndRejectsAlreadyAssignedIncident() {
        var manager=new IncidentManager();var teams=new TeamRegistry(manager);teams.register(team(2));var incident=report(manager);
        var assigned=manager.assign(incident.id(),"T1",teams);
        assertEquals(IncidentStatus.ASSIGNED,assigned.status());assertEquals("T1",assigned.assignedTeamId());
        assertEquals(assigned,manager.findIncident(incident.id()).orElseThrow());assertEquals(1,teams.activeCount("T1"));
        assertThrows(IllegalStateException.class,()->manager.assign(incident.id(),"T1",teams));
        assertThrows(IllegalStateException.class,()->teams.changeAvailability("T1",false));
    }
    @Test void rejectsStaleUnavailableUnknownAndWrongRegistryWithoutChangingIncident() {
        var manager=new IncidentManager();var teams=new TeamRegistry(manager);teams.register(team(1));var incident=report(manager);
        assertFalse(new TeamMatcher().match(incident,teams.listTeams(),manager.listIncidents()).candidates().isEmpty());
        teams.changeAvailability("T1",false);
        assertThrows(IllegalStateException.class,()->manager.assign(incident.id(),"T1",teams));
        assertThrows(IllegalArgumentException.class,()->manager.assign(incident.id(),"missing",teams));
        assertThrows(IllegalArgumentException.class,()->manager.assign(UUID.randomUUID(),"T1",teams));
        assertThrows(IllegalArgumentException.class,()->manager.assign(incident.id(),"T1",new TeamRegistry(new IncidentManager())));
        assertEquals(incident,manager.findIncident(incident.id()).orElseThrow());assertEquals(0,teams.activeCount("T1"));
    }
    @Test void rejectsUnsuitableTeamAndFullCapacity() {
        var manager=new IncidentManager();var teams=new TeamRegistry(manager);teams.register(team(1));var first=report(manager);var second=report(manager);
        var reptile=manager.reportIncident(new Reptile(Condition.INJURED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Reporter","contact");
        assertThrows(IllegalStateException.class,()->manager.assign(reptile.id(),"T1",teams));
        manager.assign(first.id(),"T1",teams);
        assertThrows(IllegalStateException.class,()->manager.assign(second.id(),"T1",teams));
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT,manager.findIncident(second.id()).orElseThrow().status());
    }
    @Test void competingRequestsCannotClaimLastPlaceTwice() throws Exception {
        var manager=new IncidentManager();var teams=new TeamRegistry(manager);teams.register(team(1));var first=report(manager);var second=report(manager);
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> one=()->{start.await();try{manager.assign(first.id(),"T1",teams);return true;}catch(IllegalStateException ex){return false;}};
            Callable<Boolean> two=()->{start.await();try{manager.assign(second.id(),"T1",teams);return true;}catch(IllegalStateException ex){return false;}};
            var a=pool.submit(one);var b=pool.submit(two);start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(1,teams.activeCount("T1"));
            assertEquals(1,manager.listIncidents().stream().filter(i->i.status()==IncidentStatus.ASSIGNED).count());
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
}
