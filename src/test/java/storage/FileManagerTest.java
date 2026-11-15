package storage;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.IncidentManager;
import service.TeamRegistry;

class FileManagerTest {
    @TempDir Path temp;
    private RescueTeam team() {
        return new RescueTeam("T1", "North, \"Team\"", List.of("Alex, Example", "Sam"),
                Set.of(AnimalCategory.BIRD), Set.of(Skill.BIRD_HANDLING), Set.of(), "North", true, 1);
    }
    private RescueIncident report(IncidentManager m) {
        return m.reportIncident(new Bird(Condition.INJURED,1), "Park, \"North\"", "North", false,
                EnvironmentalRisk.LOW, Set.of(), Set.of(), "Example Reporter", "a@example.test");
    }
    @Test void restartPreservesQuotedFieldsAssignmentStatusAndCapacity() throws Exception {
        Path dir=temp.resolve("data");UUID id;
        try(var store=FileManager.open(dir,true)) {
            var manager=new IncidentManager(store,store.load());var teams=new TeamRegistry(manager);
            teams.register(team());var incident=report(manager);id=incident.id();manager.assign(id,"T1",teams);
            manager.updateStatus(id,IncidentStatus.IN_PROGRESS);
        }
        try(var store=FileManager.open(dir,false)) {
            var loaded=store.load();var manager=new IncidentManager(store,loaded);var teams=new TeamRegistry(manager);
            assertEquals("North, \"Team\"",teams.findTeam("T1").orElseThrow().name());
            assertEquals(List.of("Alex, Example","Sam"),teams.findTeam("T1").orElseThrow().members());
            var incident=manager.findIncident(id).orElseThrow();
            assertEquals("Park, \"North\"",incident.location());assertEquals(IncidentStatus.IN_PROGRESS,incident.status());
            assertEquals("T1",incident.assignedTeamId());assertEquals(1,teams.activeCount("T1"));
            manager.updateStatus(id,IncidentStatus.COMPLETED);assertEquals(0,teams.activeCount("T1"));
        }
        try(var store=FileManager.open(dir,false)) {
            assertEquals(IncidentStatus.COMPLETED,store.load().incidents().get(0).status());
        }
    }
    @Test void initializationIsExplicitAndNeverOverwritesExistingData() throws Exception {
        Path dir=temp.resolve("data");assertThrows(IOException.class,()->FileManager.open(dir,false));
        try(var store=FileManager.open(dir,true)){assertTrue(store.load().teams().isEmpty());}
        byte[] before=Files.readAllBytes(dir.resolve("teams.csv"));
        assertThrows(IOException.class,()->FileManager.open(dir,true));assertArrayEquals(before,Files.readAllBytes(dir.resolve("teams.csv")));
        Files.delete(dir.resolve("incidents.csv"));assertThrows(IOException.class,()->FileManager.open(dir,true));
        assertArrayEquals(before,Files.readAllBytes(dir.resolve("teams.csv")));assertFalse(Files.exists(dir.resolve("incidents.csv")));
    }
    @Test void exclusiveLockRefusesSecondInstance() throws Exception {
        Path dir=temp.resolve("data");
        try(var first=FileManager.open(dir,true)) {
            var ex=assertThrows(IOException.class,()->FileManager.open(dir,false));assertTrue(ex.getMessage().contains("Another application instance"));
            String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
            Process p=new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),"app.Main").directory(temp.toFile()).redirectErrorStream(true).start();
            assertTrue(p.waitFor(20,TimeUnit.SECONDS));String text=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
            assertNotEquals(0,p.exitValue());assertTrue(text.contains("Another application instance"),text);
        }
        try(var next=FileManager.open(dir,false)){assertNotNull(next.load());}
    }
    @Test void failedSaveDoesNotChangeAcceptedMemoryOrFiles() throws Exception {
        Path dir=temp.resolve("data");var store=FileManager.open(dir,true);var manager=new IncidentManager(store,store.load());var teams=new TeamRegistry(manager);
        teams.register(team());var incident=report(manager);byte[] before=Files.readAllBytes(dir.resolve("incidents.csv"));store.close();
        assertThrows(UncheckedIOException.class,()->manager.assign(incident.id(),"T1",teams));
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT,manager.findIncident(incident.id()).orElseThrow().status());
        assertEquals(0,teams.activeCount("T1"));assertArrayEquals(before,Files.readAllBytes(dir.resolve("incidents.csv")));
        assertThrows(UncheckedIOException.class,()->teams.changeAvailability("T1",false));assertTrue(teams.findTeam("T1").orElseThrow().operationallyAvailable());
        assertThrows(UncheckedIOException.class,()->report(manager));assertEquals(1,manager.listIncidents().size());
        assertThrows(UncheckedIOException.class,()->teams.register(new RescueTeam("T2","Team",List.of("Pat"),Set.of(AnimalCategory.BIRD),Set.of(Skill.BIRD_HANDLING),Set.of(),"North",true,1)));
        assertEquals(1,teams.listTeams().size());
    }
    @Test void rejectsMalformedRecordsDuplicatesAndUnknownTeamsWithoutRewriting() throws Exception {
        Path dir=temp.resolve("data");try(var s=FileManager.open(dir,true)) {var m=new IncidentManager(s,s.load());new TeamRegistry(m).register(team());report(m);}
        Path incidents=dir.resolve("incidents.csv");String valid=Files.readString(incidents);String[] lines=valid.split("\\R");assertTrue(lines.length>=2);
        String duplicate=valid+lines[1]+"\r\n";Files.writeString(incidents,duplicate);
        try(var s=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,s::load);assertTrue(ex.getMessage().contains("duplicate incident ID"));}
        assertEquals(duplicate,Files.readString(incidents));
        String bad=lines[0]+"\r\n00000000-0000-0000-0000-000000000002,2026-11-13T00:00:00Z,BIRD,INJURED,1,Park,North,LOW,false,,,Reporter,contact,ASSIGNED,MISSING\r\n";
        Files.writeString(incidents,bad);try(var s=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,s::load);assertTrue(ex.getMessage().contains("unknown team"));}
        Files.writeString(incidents,lines[0]+"\r\ninvalid,record\r\n");try(var s=FileManager.open(dir,false)){assertThrows(IllegalArgumentException.class,s::load);}
    }
    @Test void rejectsInvalidEnumsAndAssignmentOverCapacity() throws Exception {
        Path dir=temp.resolve("data");try(var store=FileManager.open(dir,true)) {var m=new IncidentManager(store,store.load());new TeamRegistry(m).register(team());}
        Path incidents=dir.resolve("incidents.csv");String header=Files.readString(incidents);
        String row1="00000000-0000-0000-0000-000000000001,2026-11-13T00:00:00Z,BIRD,INJURED,1,Park,North,LOW,false,,,Reporter,contact,ASSIGNED,T1\r\n";
        String row2=row1.replace("000000000001","000000000002");
        Files.writeString(incidents,header+row1+row2);
        try(var store=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,store::load);assertTrue(ex.getMessage().contains("capacity exceeded"));}
        Files.writeString(incidents,header+row1.replace(",BIRD,",",DRAGON,"));
        try(var store=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,store::load);assertTrue(ex.getMessage().contains("category"));}
        Files.writeString(incidents,header+row1.replace(",false,",",maybe,"));
        try(var store=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,store::load);assertTrue(ex.getMessage().contains("danger"));}
        Files.writeString(incidents,header+row1.replace(",ASSIGNED,T1",",PENDING_ASSIGNMENT,T1"));
        try(var store=FileManager.open(dir,false)){assertThrows(IllegalArgumentException.class,store::load);}
    }
    @Test void rejectsDuplicateTeamIdsAndInvalidHeaders() throws Exception {
        Path dir=temp.resolve("data");try(var store=FileManager.open(dir,true)) {var m=new IncidentManager(store,store.load());new TeamRegistry(m).register(team());}
        Path teams=dir.resolve("teams.csv");String valid=Files.readString(teams);String[] lines=valid.split("\\R");
        Files.writeString(teams,valid+lines[1]+"\r\n");
        try(var store=FileManager.open(dir,false)){var ex=assertThrows(IllegalArgumentException.class,store::load);assertTrue(ex.getMessage().contains("duplicate team ID"));}
        Files.writeString(teams,"wrong,header\r\n");
        try(var store=FileManager.open(dir,false)){var ex=assertThrows(IOException.class,store::load);assertTrue(ex.getMessage().contains("invalid header"));}
    }
}
