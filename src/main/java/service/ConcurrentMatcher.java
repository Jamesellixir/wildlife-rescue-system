package service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import model.IncidentStatus;
import model.RescueIncident;
import model.RescueTeam;

public final class ConcurrentMatcher {
    @FunctionalInterface interface MatchFunction {
        TeamMatcher.Result match(RescueIncident incident, List<RescueTeam> teams, List<RescueIncident> incidents);
    }
    public record Outcome(RescueIncident incident, String worker, TeamMatcher.Result result, String error) { }
    private final MatchFunction match;
    private final ThreadFactory factory;

    public ConcurrentMatcher() {
        this(new TeamMatcher()::match, namedWorkers());
    }
    ConcurrentMatcher(MatchFunction match, ThreadFactory factory) {
        this.match = match;
        this.factory = factory;
    }
    private static ThreadFactory namedWorkers() {
        AtomicInteger ids = new AtomicInteger();
        return task -> new Thread(task, "matching-worker-" + ids.incrementAndGet());
    }

    public List<Outcome> matchPending(IncidentManager.MatchingSnapshot snapshot) {
        var incidents = snapshot.incidents();
        var teams = snapshot.teams();
        var pending = incidents.stream().filter(i -> i.status() == IncidentStatus.PENDING_ASSIGNMENT)
                .sorted(new PriorityCalculator().order()).toList();
        if (pending.isEmpty()) return List.of();
        ExecutorService pool = Executors.newFixedThreadPool(2, factory);
        List<Future<Outcome>> futures = new ArrayList<>();
        try {
            for (var incident : pending) {
                futures.add(pool.submit(() -> {
                    String worker = Thread.currentThread().getName();
                    try { return new Outcome(incident, worker, match.match(incident, teams, incidents), null); }
                    catch (RuntimeException failure) {
                        return new Outcome(incident, worker, null, failure.getClass().getSimpleName() + ": " + failure.getMessage());
                    }
                }));
            }
            List<Outcome> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                try { results.add(futures.get(i).get()); }
                catch (ExecutionException failure) {
                    results.add(new Outcome(pending.get(i), "unknown", null,
                            "Worker failed: " + failure.getCause().getClass().getSimpleName()));
                }
            }
            return List.copyOf(results);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent matching was interrupted.", interrupted);
        } finally {
            boolean wasInterrupted = Thread.interrupted();
            if (wasInterrupted) pool.shutdownNow();
            else pool.shutdown();
            try {
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Matching workers did not stop.");
                    }
                }
            } catch (InterruptedException interrupted) {
                pool.shutdownNow();
                wasInterrupted = true;
            } finally {
                if (wasInterrupted) Thread.currentThread().interrupt();
            }
        }
    }
}
