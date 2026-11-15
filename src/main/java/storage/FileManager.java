package storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import model.*;
import org.apache.commons.csv.*;

public final class FileManager implements AutoCloseable {
    private static final String[] TEAM_HEADERS = {"id", "name", "members", "categories", "skills", "equipment", "base_zone", "available", "capacity"};
    private static final String[] INCIDENT_HEADERS = {"id", "reported_at", "category", "condition", "count", "location", "zone", "risk", "danger", "skills", "equipment", "reporter_name", "reporter_contact", "status", "team_id"};
    private static final Set<Path> OPEN_DIRECTORIES = new HashSet<>();
    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder().setIgnoreEmptyLines(false).get();
    private final Path directory;
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;
    public record Loaded(List<RescueTeam> teams, List<RescueIncident> incidents) { }

    private FileManager(Path directory, FileChannel channel, FileLock lock) {
        this.directory = directory; this.channel = channel; this.lock = lock;
    }

    public static synchronized FileManager open(Path directory, boolean initialize) throws IOException {
        Path dir = directory.toAbsolutePath().normalize();
        if (initialize) Files.createDirectories(dir);
        if (!Files.isDirectory(dir)) throw new IOException("Data directory does not exist. Run with --init to create it.");
        dir = dir.toRealPath();
        if (OPEN_DIRECTORIES.contains(dir)) throw new IOException("Another application instance is using the data directory.");
        FileChannel channel = FileChannel.open(dir.resolve(".writer.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try { lock = channel.tryLock(); }
        catch (OverlappingFileLockException e) { channel.close(); throw new IOException("Another application instance is using the data directory.", e); }
        catch (IOException | RuntimeException e) { channel.close(); throw e; }
        if (lock == null) { channel.close(); throw new IOException("Another application instance is using the data directory."); }
        FileManager store = new FileManager(dir, channel, lock);
        OPEN_DIRECTORIES.add(dir);
        try {
            Path teams = dir.resolve("teams.csv"), incidents = dir.resolve("incidents.csv");
            if (initialize) {
                if (Files.exists(teams) || Files.exists(incidents)) throw new IOException("Setup requires both data files to be absent; existing files were not changed.");
                store.createEmpty(teams, TEAM_HEADERS);
                store.createEmpty(incidents, INCIDENT_HEADERS);
            }
            if (!Files.isRegularFile(teams) || !Files.isRegularFile(incidents)) throw new IOException("Both teams.csv and incidents.csv are required. Existing files were not replaced.");
            return store;
        } catch (IOException | RuntimeException e) { store.close(); throw e; }
    }

    private void createEmpty(Path path, String[] headers) throws IOException {
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
             var csv = FORMAT.print(writer)) { csv.printRecord((Object[]) headers); }
    }

    private static <T> T field(String file, CSVRecord row, String name, Function<String,T> parser) {
        try { return parser.apply(row.get(name)); }
        catch (RuntimeException e) { throw new IllegalArgumentException(file + " record " + row.getRecordNumber() + " field " + name + ": " + e.getMessage(), e); }
    }
    private static boolean bool(String s) {
        if (s.equals("true")) return true;
        if (s.equals("false")) return false;
        throw new IllegalArgumentException("expected true or false");
    }
    private static List<String> members(String s) {
        if (s.isEmpty()) throw new IllegalArgumentException("at least one member required");
        return Arrays.asList(s.split("\\|", -1));
    }
    private static <E extends Enum<E>> Set<E> enums(String s, Class<E> type) {
        Set<E> values = EnumSet.noneOf(type);
        if (!s.isEmpty()) for (String value : s.split("\\|", -1)) {
            if (!values.add(Enum.valueOf(type, value))) throw new IllegalArgumentException("duplicate list value: " + value);
        }
        return values;
    }
    private static String enumList(Set<? extends Enum<?>> values) {
        return String.join("|", values.stream().map(Enum::name).sorted().toList());
    }
    private static CSVParser parser(Path path, String[] headers) throws IOException {
        CSVParser p = FORMAT.builder().setHeader().setSkipHeaderRecord(true).get().parse(Files.newBufferedReader(path, StandardCharsets.UTF_8));
        if (!p.getHeaderNames().equals(List.of(headers))) { p.close(); throw new IOException(path.getFileName() + ": invalid header"); }
        return p;
    }
    private static void consistent(String file, CSVRecord row) {
        if (!row.isConsistent()) throw new IllegalArgumentException(file + " record " + row.getRecordNumber() + ": incorrect number of fields");
    }
    public Loaded load() throws IOException {
        List<RescueTeam> teams = new ArrayList<>(); Map<String,RescueTeam> byId = new HashMap<>();
        try (var csv = parser(directory.resolve("teams.csv"), TEAM_HEADERS)) {
            for (var r : csv) {
                consistent("teams.csv", r);
                String id = field("teams.csv",r,"id",x -> x);
                try {
                    RescueTeam team = new RescueTeam(id, field("teams.csv",r,"name",x -> x), field("teams.csv",r,"members",FileManager::members),
                        field("teams.csv",r,"categories",x -> enums(x, AnimalCategory.class)), field("teams.csv",r,"skills",x -> enums(x, Skill.class)),
                        field("teams.csv",r,"equipment",x -> enums(x, Equipment.class)), field("teams.csv",r,"base_zone",x -> x),
                        field("teams.csv",r,"available",FileManager::bool), field("teams.csv",r,"capacity",Integer::parseInt));
                    if (byId.putIfAbsent(team.id(), team) != null) throw new IllegalArgumentException("duplicate team ID " + team.id());
                    teams.add(team);
                } catch (RuntimeException e) { throw new IllegalArgumentException("teams.csv record " + r.getRecordNumber() + ": " + e.getMessage(), e); }
            }
        }
        List<RescueIncident> incidents = new ArrayList<>(); Set<UUID> ids = new HashSet<>(); Map<String,Integer> active = new HashMap<>();
        try (var csv = parser(directory.resolve("incidents.csv"), INCIDENT_HEADERS)) {
            for (var r : csv) {
                consistent("incidents.csv", r);
                try {
                    UUID id = field("incidents.csv",r,"id",UUID::fromString);
                    Instant reported = field("incidents.csv",r,"reported_at",Instant::parse);
                    AnimalCategory category = field("incidents.csv",r,"category",AnimalCategory::valueOf);
                    Condition condition = field("incidents.csv",r,"condition",Condition::valueOf);
                    int count = field("incidents.csv",r,"count",Integer::parseInt);
                    Animal animal = switch(category) { case BIRD -> new Bird(condition,count); case REPTILE -> new Reptile(condition,count); case MAMMAL -> new Mammal(condition,count); };
                    IncidentStatus status = field("incidents.csv",r,"status",IncidentStatus::valueOf);
                    String assigned = field("incidents.csv",r,"team_id",x -> x.isEmpty() ? null : x);
                    RescueIncident i = RescueIncident.restore(id,reported,animal,field("incidents.csv",r,"location",x -> x),field("incidents.csv",r,"zone",x -> x),
                        field("incidents.csv",r,"danger",FileManager::bool),field("incidents.csv",r,"risk",EnvironmentalRisk::valueOf),
                        field("incidents.csv",r,"skills",x -> enums(x,Skill.class)),field("incidents.csv",r,"equipment",x -> enums(x,Equipment.class)),
                        field("incidents.csv",r,"reporter_name",x -> x),field("incidents.csv",r,"reporter_contact",x -> x),status,assigned);
                    if (!ids.add(id)) throw new IllegalArgumentException("field id: duplicate incident ID " + id);
                    if (assigned != null && !byId.containsKey(assigned)) throw new IllegalArgumentException("field team_id: unknown team " + assigned);
                    if (status == IncidentStatus.ASSIGNED || status == IncidentStatus.IN_PROGRESS) {
                        int workload = active.merge(assigned, 1, Integer::sum); RescueTeam team = byId.get(assigned);
                        if (!team.operationallyAvailable()) throw new IllegalArgumentException("field team_id: active team is unavailable");
                        if (workload > team.capacity()) throw new IllegalArgumentException("field team_id: team capacity exceeded");
                    }
                    incidents.add(i);
                } catch (RuntimeException e) { throw new IllegalArgumentException("incidents.csv record " + r.getRecordNumber() + ": " + e.getMessage(), e); }
            }
        }
        return new Loaded(List.copyOf(teams), List.copyOf(incidents));
    }
    public void saveTeams(List<RescueTeam> teams) {
        atomicWrite("teams.csv", TEAM_HEADERS, csv -> {
            for (var t : teams) csv.printRecord(t.id(),t.name(),String.join("|",t.members()),enumList(t.categories()),enumList(t.skills()),enumList(t.equipment()),t.baseZone(),t.operationallyAvailable(),t.capacity());
        });
    }
    public void saveIncidents(List<RescueIncident> incidents) {
        atomicWrite("incidents.csv", INCIDENT_HEADERS, csv -> {
            for (var i : incidents) csv.printRecord(i.id(),i.reportedAt(),i.animal().category(),i.animal().condition(),i.animal().count(),i.location(),i.zone(),i.risk(),i.immediateDanger(),enumList(i.requiredSkills()),enumList(i.requiredEquipment()),i.reporterName(),i.reporterContact(),i.status(),i.assignedTeamId()==null?"":i.assignedTeamId());
        });
    }
    @FunctionalInterface private interface Rows { void write(CSVPrinter printer) throws IOException; }
    private void atomicWrite(String filename, String[] headers, Rows rows) {
        Path temp = null;
        try {
            if (!lock.isValid()) throw new IOException("Data directory lock is no longer held.");
            temp = Files.createTempFile(directory, ".rescue-", ".tmp");
            try (var out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8); var csv = FORMAT.print(out)) {
                csv.printRecord((Object[]) headers); rows.write(csv);
            }
            Files.move(temp, directory.resolve(filename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { throw new UncheckedIOException("Could not save " + filename + "; change not accepted", e); }
        finally { if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { } }
    }
    @Override public void close() throws IOException {
        synchronized (FileManager.class) {
            if (closed) return;
            closed = true;
            try { lock.release(); }
            finally {
                try { channel.close(); }
                finally { OPEN_DIRECTORIES.remove(directory); }
            }
        }
    }
}
