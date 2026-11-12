package ui;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import model.*;
import org.junit.jupiter.api.Test;
import service.IncidentManager;

class TeamMatchingMenuTest {
    @Test void findsSuitableTeamAndExplainsNoReservation() {
        IncidentManager manager=new IncidentManager();
        var incident=manager.reportIncident(new Bird(Condition.TRAPPED,1),"Roof","North",false,
                EnvironmentalRisk.LOW,Set.of(Skill.CLIMBING),Set.of(Equipment.CLIMBING_EQUIPMENT),"Alex","contact");
        String input="3\nT1\nNorth Team\nAlex\n1\n1,4\n1\nNorth\n2\ny\n5\n"+incident.id()+"\n0\n";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try (Scanner scanner=new Scanner(input); PrintStream output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            new ConsoleMenu(scanner,output,manager).run();
        }
        String text=bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("T1 | North Team | same zone | required skills and equipment available | 0 of 2 places occupied"));
        assertTrue(text.contains("Suggestions do not assign or reserve a team."));
        assertTrue(incident.status()==IncidentStatus.PENDING_ASSIGNMENT);
    }
}
