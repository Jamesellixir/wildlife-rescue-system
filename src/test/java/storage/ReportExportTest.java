package storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import model.*;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.*;

class ReportExportTest {
    @TempDir Path temp;
    @Test void exportsHeaderOnlyForEmptyDataAndQuotesIncidentValues() throws Exception {
        try(var store=FileManager.open(temp.resolve("data"),true)) {
            var manager=new IncidentManager(store,store.load());var reports=new RescueReportService();
            Path output=manager.exportReport(reports.rows(manager.listIncidents()));
            try(var reader=Files.newBufferedReader(output,StandardCharsets.UTF_8);
                var csv=CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                assertEquals(11,csv.getHeaderNames().size());assertTrue(csv.getRecords().isEmpty());
            }
            var incident=manager.reportIncident(new Bird(Condition.INJURED,1),"Park, \"North\"","North",false,
                    EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
            manager.exportReport(reports.rows(manager.listIncidents()));
            try(var reader=Files.newBufferedReader(output,StandardCharsets.UTF_8);
                var csv=CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                var rows=csv.getRecords();assertEquals(1,rows.size());assertEquals(incident.id().toString(),rows.get(0).get("incident_id"));
                assertEquals("Park, \"North\"",rows.get(0).get("location"));assertEquals("MEDIUM",rows.get(0).get("priority"));
                assertEquals("PENDING_ASSIGNMENT",rows.get(0).get("status"));
            }
        }
    }
    @Test void exportFailureLeavesSourceFilesAndConsoleSummaryIntact() throws Exception {
        Path dir=temp.resolve("data");try(var store=FileManager.open(dir,true)) {
            var manager=new IncidentManager(store,store.load());var report=new RescueReportService();
            byte[] before=Files.readAllBytes(dir.resolve("incidents.csv"));Files.createDirectory(dir.resolve("rescue-report.csv"));
            assertThrows(java.io.UncheckedIOException.class,()->manager.exportReport(report.rows(manager.listIncidents())));
            assertArrayEquals(before,Files.readAllBytes(dir.resolve("incidents.csv")));assertTrue(Files.isDirectory(dir.resolve("rescue-report.csv")));
            var bytes=new java.io.ByteArrayOutputStream();
            try(var scanner=new java.util.Scanner("7\n0\n");var output=new java.io.PrintStream(bytes,true,StandardCharsets.UTF_8)) {
                new ui.ConsoleMenu(scanner,output,manager).run();
            }
            String text=bytes.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("Total: 0"));assertTrue(text.contains("CSV export failed:"));assertFalse(text.contains("CSV report:"));
        }
    }
    @Test void exportsEachStatusAndAssignedTeamWithoutChangingRecords() throws Exception {
        try(var store=FileManager.open(temp.resolve("data"),true)) {
            var manager=new IncidentManager(store,store.load());
            var first=manager.reportIncident(new Bird(Condition.TRAPPED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
            var assigned=first.withAssignment("T1");var progress=assigned.withStatus(IncidentStatus.IN_PROGRESS);
            var completed=progress.withStatus(IncidentStatus.COMPLETED);
            Path output=manager.exportReport(new RescueReportService().rows(List.of(first,assigned,progress,completed)));
            try(var reader=Files.newBufferedReader(output,StandardCharsets.UTF_8);
                var csv=CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                var rows=csv.getRecords();assertEquals(4,rows.size());
                assertEquals(List.of("PENDING_ASSIGNMENT","ASSIGNED","IN_PROGRESS","COMPLETED"),rows.stream().map(r->r.get("status")).toList());
                assertEquals("",rows.get(0).get("assigned_team_id"));assertEquals("T1",rows.get(3).get("assigned_team_id"));
            }
            assertEquals(IncidentStatus.PENDING_ASSIGNMENT,manager.findIncident(first.id()).orElseThrow().status());
        }
    }
}
