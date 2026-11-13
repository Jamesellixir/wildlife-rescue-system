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
class AssignmentMenuTest {
    @Test void assignsSelectedSuitableTeam() {
        var manager=new IncidentManager();var incident=manager.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
        String input="3\nT1\nTeam\nAlex\n1\n1\n\nNorth\n1\ny\n5\n"+incident.id()+"\nT1\n0\n";
        var bytes=new ByteArrayOutputStream();
        try(var scanner=new Scanner(input);var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)){new ConsoleMenu(scanner,output,manager).run();}
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("assigned to team T1. Active workload: 1 of 1."));
        assertEquals(IncidentStatus.ASSIGNED,manager.findIncident(incident.id()).orElseThrow().status());
    }
}
