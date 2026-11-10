package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import model.Bird;
import model.Condition;
import model.EnvironmentalRisk;
import model.Equipment;
import model.RescueIncident;
import model.Skill;
import org.junit.jupiter.api.Test;
import service.IncidentManager;

class ConsoleMenuTest {
    @Test
    void correctsInvalidFieldsAndRecordsOneReport() {
        IncidentManager manager = new IncidentManager();
        String output = run(manager, """
                1
                9
                1
                2
                0
                2

                Park Road, Colombo
                Central
                maybe
                y
                3
                99
                4
                1, 2
                Sample Reporter

                reporter@example.test
                0
                """);

        RescueIncident incident = manager.listIncidents().get(0);
        assertEquals(1, manager.listIncidents().size());
        assertEquals(2, incident.animal().count());
        assertEquals("Park Road, Colombo", incident.location());
        assertEquals(EnvironmentalRisk.HIGH, incident.risk());
        assertEquals(Set.of(Skill.CLIMBING), incident.requiredSkills());
        assertEquals(Set.of(Equipment.CLIMBING_EQUIPMENT, Equipment.ANIMAL_CARRIER),
                incident.requiredEquipment());
        assertTrue(incident.immediateDanger());
        assertTrue(output.contains("Location is required."));
        assertTrue(output.contains("Enter a positive whole number."));
        assertTrue(output.contains("Enter y or n."));
        assertTrue(output.contains("Enter only numbers shown in the list."));
        assertTrue(output.contains("Reporter contact is required."));
        assertTrue(output.contains("Incident recorded. ID: " + incident.id()));
    }

    @Test
    void listsReportsAndShowsDetailsForTheSelectedId() {
        IncidentManager manager = new IncidentManager();
        RescueIncident report = manager.reportIncident(new Bird(Condition.INJURED, 1), "Park Road",
                "Central", false, EnvironmentalRisk.LOW, Set.of(), Set.of(),
                "Sample Reporter", "reporter@example.test");

        String output = run(manager, "2\n" + report.id() + "\n0\n");

        assertTrue(output.contains(report.id() + " | Bird | Park Road | Pending assignment"));
        assertTrue(output.contains("Animal condition: Injured"));
        assertTrue(output.contains("Reporter contact: reporter@example.test"));
    }

    @Test
    void handlesEmptyListsInvalidIdsAndUnknownIds() {
        IncidentManager manager = new IncidentManager();
        assertTrue(run(manager, "2\n0\n").contains("No incidents recorded."));

        manager.reportIncident(new Bird(Condition.INJURED, 1), "Park Road", "Central",
                false, EnvironmentalRisk.LOW, Set.of(), Set.of(), "Sample Reporter", "reporter@example.test");
        String output = run(manager, "2\ninvalid\n2\n00000000-0000-0000-0000-000000000000\n0\n");

        assertTrue(output.contains("Invalid incident ID."));
        assertTrue(output.contains("No incident found for that ID."));
    }

    @Test
    void closesCleanlyIfInputEndsDuringAReport() {
        IncidentManager manager = new IncidentManager();

        String output = run(manager, "1\n1\n");

        assertTrue(output.contains("Input ended. Closing the console."));
        assertTrue(manager.listIncidents().isEmpty());
    }

    private String run(IncidentManager manager, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Scanner scanner = new Scanner(input);
             PrintStream console = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            new ConsoleMenu(scanner, console, manager).run();
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
