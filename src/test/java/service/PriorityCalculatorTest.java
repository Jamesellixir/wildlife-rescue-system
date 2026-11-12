package service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import model.*;
import org.junit.jupiter.api.Test;

class PriorityCalculatorTest {
    private final PriorityCalculator calculator = new PriorityCalculator();
    private RescueIncident incident(Condition condition, int count, boolean danger, EnvironmentalRisk risk, int id, String time) {
        return new RescueIncident(new UUID(0, id), Instant.parse(time), new Bird(condition, count),
                "Park", "North", danger, risk, Set.of(), Set.of(), "Reporter", "contact");
    }
    private RescueIncident i(Condition c, int count, boolean danger, EnvironmentalRisk risk) {
        return incident(c, count, danger, risk, 1, "2026-11-12T00:00:00Z");
    }
    @Test void conditionPointsAndCountBands() {
        assertEquals(30, calculator.calculate(i(Condition.INJURED, 1, false, EnvironmentalRisk.LOW)).conditionPoints());
        assertEquals(25, calculator.calculate(i(Condition.TRAPPED, 1, false, EnvironmentalRisk.LOW)).conditionPoints());
        assertEquals(15, calculator.calculate(i(Condition.ABANDONED, 1, false, EnvironmentalRisk.LOW)).conditionPoints());
        assertEquals(10, calculator.calculate(i(Condition.DISPLACED, 1, false, EnvironmentalRisk.LOW)).conditionPoints());
        for (int n : new int[]{1, 2, 4, 5}) {
            int expected = n == 1 ? 0 : n < 5 ? 5 : 10;
            assertEquals(expected, calculator.calculate(i(Condition.INJURED, n, false, EnvironmentalRisk.LOW)).countPoints());
        }
    }
    @Test void riskPointsThresholdsAndDangerOverride() {
        var low = calculator.calculate(i(Condition.DISPLACED, 1, false, EnvironmentalRisk.LOW));
        assertEquals(PriorityLevel.LOW, low.level()); assertEquals(10, low.score());
        var medium = calculator.calculate(i(Condition.ABANDONED, 1, false, EnvironmentalRisk.MEDIUM));
        assertEquals(10, medium.riskPoints()); assertEquals(25, medium.score()); assertEquals(PriorityLevel.MEDIUM, medium.level());
        var high = calculator.calculate(i(Condition.TRAPPED, 1, false, EnvironmentalRisk.HIGH));
        assertEquals(20, high.riskPoints()); assertEquals(45, high.score()); assertEquals(PriorityLevel.HIGH, high.level());
        var danger = calculator.calculate(i(Condition.DISPLACED, 1, true, EnvironmentalRisk.LOW));
        assertEquals(50, danger.score()); assertEquals(40, danger.dangerPoints()); assertEquals(PriorityLevel.CRITICAL, danger.level());
        var critical = calculator.calculate(i(Condition.INJURED, 1, true, EnvironmentalRisk.LOW));
        assertEquals(70, critical.score()); assertEquals(PriorityLevel.CRITICAL, critical.level());
    }
    @Test void ordersByLevelScoreTimeThenId() {
        var older=incident(Condition.INJURED,1,false,EnvironmentalRisk.HIGH,3,"2026-11-11T00:00:00Z");
        var first=incident(Condition.INJURED,1,false,EnvironmentalRisk.HIGH,1,"2026-11-12T00:00:00Z");
        var second=incident(Condition.INJURED,1,false,EnvironmentalRisk.HIGH,2,"2026-11-12T00:00:00Z");
        var urgent=incident(Condition.DISPLACED,1,true,EnvironmentalRisk.LOW,4,"2026-11-13T00:00:00Z");
        var lower=incident(Condition.TRAPPED,1,false,EnvironmentalRisk.HIGH,5,"2026-11-10T00:00:00Z");
        assertEquals(List.of(urgent,older,first,second,lower),List.of(second,lower,first,urgent,older).stream().sorted(calculator.order()).toList());
    }
}
