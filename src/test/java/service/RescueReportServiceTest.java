package service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import model.*;
import org.junit.jupiter.api.Test;

class RescueReportServiceTest {
    private RescueIncident incident(int id) {
        return new RescueIncident(new UUID(0,id),Instant.EPOCH,new Bird(Condition.INJURED,1),
                "Park, \"North\"","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"Alex","contact");
    }
    @Test void countsAllStatusesSeparatelyAndComputesActive() {
        var pending=incident(1);var assigned=incident(2).withAssignment("T1");
        var progress=incident(3).withAssignment("T1").withStatus(IncidentStatus.IN_PROGRESS);
        var completed=incident(4).withAssignment("T1").withStatus(IncidentStatus.IN_PROGRESS).withStatus(IncidentStatus.COMPLETED);
        var summary=new RescueReportService().summarize(List.of(pending,assigned,progress,completed));
        for(var status:IncidentStatus.values()) assertEquals(1,summary.count(status));
        assertEquals(2,summary.active());assertEquals(4,summary.total());
    }
    @Test void emptyDataGivesZeroesAndNoRows() {
        var reports=new RescueReportService();var summary=reports.summarize(List.of());
        for(var status:IncidentStatus.values()) assertEquals(0,summary.count(status));
        assertEquals(0,summary.active());assertEquals(0,summary.total());assertTrue(reports.rows(List.of()).isEmpty());
    }
    @Test void rowsContainCorrectPriorityAndPreserveQuotedLocation() {
        var incident=incident(1);var row=new RescueReportService().rows(List.of(incident)).get(0);
        assertEquals(incident.id().toString(),row.get(0));assertEquals("Park, \"North\"",row.get(5));
        assertEquals("MEDIUM",row.get(7));assertEquals("30",row.get(8));assertEquals("PENDING_ASSIGNMENT",row.get(9));assertEquals("",row.get(10));
    }
}
