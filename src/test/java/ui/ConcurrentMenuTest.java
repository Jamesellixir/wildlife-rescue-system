package ui;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import model.*;
import org.junit.jupiter.api.Test;
import service.IncidentManager;
class ConcurrentMenuTest {
    @Test void showsWorkersAndDoesNotAssign() {
        var m=new IncidentManager();var i=m.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
        var bytes=new ByteArrayOutputStream();
        try(var scanner=new Scanner("3\nT1\nTeam\nAlex\n1\n1\n\nNorth\n1\ny\n8\n0\n");var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)){new ConsoleMenu(scanner,output,m).run();}
        String text=bytes.toString(StandardCharsets.UTF_8);assertTrue(text.contains("worker: matching-worker-"));
        assertTrue(text.contains("T1 | Team | same zone | 0 of 1 places occupied"));
        assertTrue(text.contains("Suggestions do not assign or reserve a team."));
        assertEquals(IncidentStatus.PENDING_ASSIGNMENT,m.findIncident(i.id()).orElseThrow().status());
    }
    @Test void reportsAnEmptyPendingQueue() {
        var bytes=new ByteArrayOutputStream();try(var scanner=new Scanner("8\n0\n");var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)){new ConsoleMenu(scanner,output,new IncidentManager()).run();}
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("No pending incidents."));
    }
}
