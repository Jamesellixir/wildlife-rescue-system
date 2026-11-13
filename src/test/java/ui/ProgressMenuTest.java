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
class ProgressMenuTest {
    @Test void assignsStartsAndCompletesRescue() {
        var m=new IncidentManager();var i=m.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
        String input="3\nT1\nTeam\nAlex\n1\n1\n\nNorth\n1\ny\n5\n"+i.id()+"\nT1\n6\n"+i.id()+"\n1\n6\n"+i.id()+"\n2\n0\n";
        var bytes=new ByteArrayOutputStream();
        try(var scanner=new Scanner(input);var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)){new ConsoleMenu(scanner,output,m).run();}
        var text=bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("status: In progress."));assertTrue(text.contains("status: Completed."));
        assertTrue(text.contains("One team capacity place has been released."));
        assertEquals(IncidentStatus.COMPLETED,m.findIncident(i.id()).orElseThrow().status());
    }
    @Test void rejectsInvalidAndUnknownIds() {
        var bytes=new ByteArrayOutputStream();var m=new IncidentManager();
        try(var scanner=new Scanner("6\nwrong\n6\n00000000-0000-0000-0000-000000000000\n0\n");var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)){new ConsoleMenu(scanner,output,m).run();}
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Invalid incident ID."));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("No incident found for that ID."));
    }
}
