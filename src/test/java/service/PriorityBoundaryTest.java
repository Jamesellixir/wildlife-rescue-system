package service;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import model.*;
import org.junit.jupiter.api.Test;
class PriorityBoundaryTest {
    @Test void checksAdjacentReachableThresholdScores() {
        var m=new IncidentManager();var calc=new PriorityCalculator();
        var belowMedium=m.reportIncident(new Bird(Condition.DISPLACED,1),"Park","North",false,EnvironmentalRisk.MEDIUM,Set.of(),Set.of(),"A","contact");
        var atMedium=m.reportIncident(new Bird(Condition.TRAPPED,1),"Park","North",false,EnvironmentalRisk.LOW,Set.of(),Set.of(),"A","contact");
        var belowHigh=m.reportIncident(new Bird(Condition.INJURED,1),"Park","North",false,EnvironmentalRisk.MEDIUM,Set.of(),Set.of(),"A","contact");
        var atHigh=m.reportIncident(new Bird(Condition.TRAPPED,1),"Park","North",false,EnvironmentalRisk.HIGH,Set.of(),Set.of(),"A","contact");
        assertEquals(20,calc.calculate(belowMedium).score());assertEquals(PriorityLevel.LOW,calc.calculate(belowMedium).level());
        assertEquals(25,calc.calculate(atMedium).score());assertEquals(PriorityLevel.MEDIUM,calc.calculate(atMedium).level());
        assertEquals(40,calc.calculate(belowHigh).score());assertEquals(PriorityLevel.MEDIUM,calc.calculate(belowHigh).level());
        assertEquals(45,calc.calculate(atHigh).score());assertEquals(PriorityLevel.HIGH,calc.calculate(atHigh).level());
    }
}
