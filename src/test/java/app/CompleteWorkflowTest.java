package app;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompleteWorkflowTest {
    @TempDir Path temp;
    private int runNumber;
    private record Run(int code, String output) { }
    private Run run(String input, String... args) throws Exception {
        String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
        var command=new ArrayList<>(List.of(java,"-cp",System.getProperty("java.class.path"),"app.Main"));
        command.addAll(List.of(args));
        Path log=temp.resolve("run-"+(++runNumber)+".txt");
        Process process=new ProcessBuilder(command).directory(temp.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            try(var stdin=process.getOutputStream()){stdin.write(input.getBytes(StandardCharsets.UTF_8));}
            assertTrue(process.waitFor(30,TimeUnit.SECONDS),"Application did not finish");
            return new Run(process.exitValue(),Files.readString(log));
        } finally {if(process.isAlive()){process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS);}}
    }
    @Test void realProcessesCompleteWorkflowAndRejectDamagedData() throws Exception {
        String initial="3\nT1\nNorth Team\nAlex\n1\n1\n\nNorth\n1\ny\n"
                +"1\n9\n1\n1\n0\n1\n\nBird Park\nNorth\ny\n1\n\n\nAlex\na@example.test\n"
                +"1\n2\n1\n1\nRiver\nNorth\nn\n1\n\n\nSam\ns@example.test\n0\n";
        Run first=run(initial,"--init");assertEquals(0,first.code(),first.output());
        assertTrue(first.output().contains("Team registered: T1"));
        assertTrue(first.output().contains("Enter a number from 1 to 3."));assertTrue(first.output().contains("Enter a positive whole number."));
        assertTrue(first.output().contains("Location is required."));assertTrue(first.output().contains("Priority: Critical (score 70)"));
        var matcher=Pattern.compile("Incident recorded\\. ID: ([0-9a-f-]{36})").matcher(first.output());
        assertTrue(matcher.find());String bird=matcher.group(1);assertTrue(matcher.find());String reptile=matcher.group(1);assertNotEquals(bird,reptile);
        String secondInput="8\n5\n"+reptile+"\n5\n"+bird+"\nT1\n6\n"+bird+"\n1\n6\n"+bird+"\n2\n7\n0\n";
        Run second=run(secondInput);assertEquals(0,second.code(),second.output());
        assertTrue(second.output().contains("worker: matching-worker-"));assertTrue(second.output().contains("No suitable teams found."));
        assertTrue(second.output().contains("assigned to team T1. Active workload: 1 of 1."));
        assertTrue(second.output().contains("status: In progress."));assertTrue(second.output().contains("status: Completed."));
        assertTrue(second.output().contains("Pending: 1"));assertTrue(second.output().contains("Completed: 1"));assertTrue(second.output().contains("Active: 0"));
        Path report=temp.resolve("data/rescue-report.csv");
        try(var reader=Files.newBufferedReader(report);var csv=CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            var rows=csv.getRecords();assertEquals(2,rows.size());
            assertEquals("COMPLETED",rows.stream().filter(r->r.get("incident_id").equals(bird)).findFirst().orElseThrow().get("status"));
            assertEquals("PENDING_ASSIGNMENT",rows.stream().filter(r->r.get("incident_id").equals(reptile)).findFirst().orElseThrow().get("status"));
        }
        Run third=run("2\n"+bird+"\n4\n\n6\n"+bird+"\n1\n0\n");assertEquals(0,third.code(),third.output());
        assertTrue(third.output().contains("Status: Completed"));assertTrue(third.output().contains("T1 | North Team | Available | 0/1 active"));
        assertTrue(third.output().contains("Cannot change incident status from COMPLETED"));
        Path incidents=temp.resolve("data/incidents.csv");String clean=Files.readString(incidents);
        Files.writeString(incidents,"broken,row\r\n",StandardOpenOption.APPEND);
        byte[] damaged=Files.readAllBytes(incidents);Run invalid=run("");assertNotEquals(0,invalid.code());
        assertTrue(invalid.output().contains("incidents.csv"),invalid.output());assertArrayEquals(damaged,Files.readAllBytes(incidents));
        assertTrue(clean.contains("COMPLETED"));Files.writeString(incidents,clean.replace("COMPLETED","INVALID_STATUS"));
        byte[] wrongStatus=Files.readAllBytes(incidents);Run invalidStatus=run("");assertNotEquals(0,invalidStatus.code());
        assertTrue(invalidStatus.output().contains("status"),invalidStatus.output());assertArrayEquals(wrongStatus,Files.readAllBytes(incidents));
    }
}
