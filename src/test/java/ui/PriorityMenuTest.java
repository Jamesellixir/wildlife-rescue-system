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

class PriorityMenuTest {
    @Test void showsBreakdownAndListsCriticalBeforeEarlierLowIncident() {
        IncidentManager manager = new IncidentManager();
        manager.reportIncident(new Bird(Condition.DISPLACED, 1), "Low Park", "North", false,
                EnvironmentalRisk.LOW, Set.of(), Set.of(), "Alex", "contact");
        var urgent = manager.reportIncident(new Bird(Condition.DISPLACED, 1), "Danger Park", "North", true,
                EnvironmentalRisk.LOW, Set.of(), Set.of(), "Alex", "contact");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Scanner input = new Scanner("2\n" + urgent.id() + "\n0\n");
             PrintStream console = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            new ConsoleMenu(input, console, manager).run();
        }
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.indexOf("Danger Park") < text.indexOf("Low Park"));
        assertTrue(text.contains("Priority: Critical (score 50)"));
        assertTrue(text.contains("condition 10, immediate danger 40, animal count 0, environmental risk 0"));
    }
}
