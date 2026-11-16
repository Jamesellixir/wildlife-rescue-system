package service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import model.*;
import org.junit.jupiter.api.Test;

class ConcurrentMatcherTest {
    private RescueIncident report(IncidentManager m, boolean danger) {
        return m.reportIncident(new Bird(Condition.INJURED,1),"Park","North",danger,
                EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
    }
    private void register(IncidentManager m) {
        m.teamRegistry().register(new RescueTeam("T1","Team",List.of("Alex"),Set.of(AnimalCategory.BIRD),
                Set.of(Skill.BIRD_HANDLING),Set.of(),"North",true,2));
    }
    @Test void usesTwoWorkersAndMatchesSequentialResultsInPriorityOrder() {
        var m=new IncidentManager();register(m);var low=report(m,false);var urgent=report(m,true);var snapshot=m.matchingSnapshot();
        var ready=new CountDownLatch(2);var threads=Collections.synchronizedList(new ArrayList<Thread>());
        var service=new ConcurrentMatcher((incident,teams,all)->{
            ready.countDown();
            try { if (!ready.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not start"); }
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            return new TeamMatcher().match(incident,teams,all);
        },task->{var thread=new Thread(task,"test-worker-"+(threads.size()+1));threads.add(thread);return thread;});
        var result=service.matchPending(snapshot);
        assertEquals(List.of(urgent.id(),low.id()),result.stream().map(x->x.incident().id()).toList());
        assertEquals(2,result.stream().map(ConcurrentMatcher.Outcome::worker).distinct().count());
        for(var outcome:result){assertNull(outcome.error());assertEquals(new TeamMatcher().match(outcome.incident(),snapshot.teams(),snapshot.incidents()),outcome.result());}
        assertEquals(2,threads.size());assertTrue(threads.stream().noneMatch(Thread::isAlive));
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT,m.findIncident(urgent.id()).orElseThrow().status());
    }
    @Test void snapshotDoesNotChangeWhenLiveStateChanges() {
        var m=new IncidentManager();register(m);var incident=report(m,false);var snapshot=m.matchingSnapshot();
        m.teamRegistry().changeAvailability("T1",false);report(m,true);
        var outcomes=new ConcurrentMatcher().matchPending(snapshot);
        assertEquals(1,outcomes.size());assertEquals(incident.id(),outcomes.get(0).incident().id());
        assertEquals(1,outcomes.get(0).result().candidates().size());
        assertThrows(UnsupportedOperationException.class,()->snapshot.incidents().clear());
        assertThrows(UnsupportedOperationException.class,()->snapshot.teams().clear());
    }
    @Test void reportsOneWorkerFailureAndKeepsOtherResults() {
        var m=new IncidentManager();register(m);var first=report(m,false);var second=report(m,true);
        var service=new ConcurrentMatcher((incident,teams,all)->{
            if(incident.id().equals(first.id()))throw new IllegalStateException("test failure");
            return new TeamMatcher().match(incident,teams,all);
        },r->new Thread(r,"test-worker"));
        var result=service.matchPending(m.matchingSnapshot());
        assertEquals(second.id(),result.get(0).incident().id());assertNull(result.get(0).error());
        assertEquals(first.id(),result.get(1).incident().id());assertTrue(result.get(1).error().contains("test failure"));
    }
    @Test void excludesAssignedIncidentsAndHandlesEmptyQueue() {
        var m=new IncidentManager();register(m);assertTrue(new ConcurrentMatcher().matchPending(m.matchingSnapshot()).isEmpty());
        var incident=report(m,false);m.assign(incident.id(),"T1",m.teamRegistry());
        assertTrue(new ConcurrentMatcher().matchPending(m.matchingSnapshot()).isEmpty());
    }
    @Test void interruptionCancelsWorkersAndPreservesInterruptFlag() throws Exception {
        var m=new IncidentManager();register(m);report(m,false);
        var entered=new CountDownLatch(1);var threads=Collections.synchronizedList(new ArrayList<Thread>());
        var service=new ConcurrentMatcher((incident,teams,all)->{
            entered.countDown();
            try { new CountDownLatch(1).await(); }
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("cancelled",e);}
            throw new AssertionError("unexpected release");
        },task->{var thread=new Thread(task,"interrupt-test-worker");threads.add(thread);return thread;});
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        var failed=new java.util.concurrent.atomic.AtomicBoolean();
        Thread caller=new Thread(()->{
            try { service.matchPending(m.matchingSnapshot()); }
            catch(IllegalStateException expected){failed.set(true);interrupted.set(Thread.currentThread().isInterrupted());}
        });
        caller.start();assertTrue(entered.await(5,TimeUnit.SECONDS));caller.interrupt();caller.join(5000);
        assertFalse(caller.isAlive());assertTrue(failed.get());assertTrue(interrupted.get());
        assertEquals(1,threads.size());assertTrue(threads.stream().noneMatch(Thread::isAlive));
    }
}
