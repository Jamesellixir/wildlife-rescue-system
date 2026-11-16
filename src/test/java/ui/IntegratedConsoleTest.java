package ui;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;
import model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.IncidentManager;
import storage.FileManager;

class IntegratedConsoleTest {
    @TempDir Path temp;
    @Test void menusConnectReportingAssignmentProgressExportAndConcurrentMatching() throws Exception {
        Path data=temp.resolve("data");java.util.UUID id;
        try(var store=FileManager.open(data,true)) {
            var m=new IncidentManager(store,store.load());
            var incident=m.reportIncident(new Bird(Condition.INJURED,1),"Known Park","North",false,
                    EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");id=incident.id();
            String input="1\n1\n1\n1\nSecond Park\nNorth\nn\n1\n\n\nSam\ncontact\n"
                    +"3\nT1\nTeam\nAlex\n1\n1\n\nNorth\n1\ny\n"
                    +"2\n"+id+"\n4\n\n8\n5\n"+id+"\nT1\n"
                    +"6\n"+id+"\n1\n7\n6\n"+id+"\n2\n7\n8\n0\n";
            var bytes=new ByteArrayOutputStream();
            try(var scanner=new Scanner(input);var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
                new ConsoleMenu(scanner,output,m).run();
            }
            String text=bytes.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("Incident recorded. ID:"));assertTrue(text.contains("Team registered: T1"));
            assertTrue(text.contains("Incident ID: "+id));assertTrue(text.contains("Priority: Medium (30)"));
            assertTrue(text.contains("worker: matching-worker-"));assertTrue(text.contains("Pending incidents (priority order):"));
            assertTrue(text.contains("assigned to team T1. Active workload: 1 of 1."));
            assertTrue(text.contains("Current status: Assigned"));assertTrue(text.contains("status: In progress."));
            assertTrue(text.contains("status: Completed."));assertTrue(text.contains("One team capacity place has been released."));
            assertTrue(text.contains("Active: 1"));assertTrue(text.contains("Completed: 1"));
            assertTrue(text.contains("CSV report:"));assertTrue(text.contains("Goodbye."));
            assertEquals(IncidentStatus.COMPLETED,m.findIncident(id).orElseThrow().status());
            assertEquals(0,m.teamRegistry().activeCount("T1"));assertTrue(Files.exists(data.resolve("rescue-report.csv")));
        }
        try(var store=FileManager.open(data,false)) {
            var loaded=store.load();assertEquals(2,loaded.incidents().size());
            assertEquals(IncidentStatus.COMPLETED,loaded.incidents().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow().status());
            assertEquals(1,loaded.teams().size());
        }
    }
    @Test void menuHandlesInvalidChoiceAndEmptyAndCancelledActions() {
        var bytes=new ByteArrayOutputStream();
        try(var scanner=new Scanner("9\n5\n6\n\n8\n0\n");var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            new ConsoleMenu(scanner,output,new IncidentManager()).run();
        }
        var text=bytes.toString(StandardCharsets.UTF_8);assertTrue(text.contains("Choose a number from 0 to 8."));
        assertTrue(text.contains("No pending incidents."));assertTrue(text.contains("Goodbye."));
    }
}
