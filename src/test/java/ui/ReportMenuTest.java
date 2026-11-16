package ui;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import service.IncidentManager;
class ReportMenuTest {
    @Test void displaysEmptySummaryWhenExportIsUnavailable() {
        var bytes=new ByteArrayOutputStream();
        try(var scanner=new Scanner("7\n0\n");var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            new ConsoleMenu(scanner,output,new IncidentManager()).run();
        }
        String text=bytes.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Pending: 0"));assertTrue(text.contains("Assigned: 0"));
        assertTrue(text.contains("In progress: 0"));assertTrue(text.contains("Completed: 0"));
        assertTrue(text.contains("Active: 0"));assertTrue(text.contains("Total: 0"));
        assertTrue(text.contains("CSV export failed:"));assertFalse(text.contains("CSV report:"));
    }
}
