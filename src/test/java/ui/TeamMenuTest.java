package ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import service.IncidentManager;

class TeamMenuTest {
    @Test
    void registersListsChangesAvailabilityAndRejectsDuplicate() {
        String input = "3\nT1\nNorth\nAlex, Sam\n1\n1\n1\nCentral\n2\ny\n"
                + "3\nT1\n4\nT1\nn\n4\n\n0\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
             PrintStream console = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            new ConsoleMenu(scanner, console, new IncidentManager()).run();
        }
        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Team registered: T1"));
        assertTrue(result.contains("A team with this ID already exists."));
        assertTrue(result.contains("T1 | North | Available | 0/2 active"));
        assertTrue(result.contains("T1 | North | Unavailable | 0/2 active"));
        assertTrue(result.contains("Members: Alex, Sam"));
    }
}
