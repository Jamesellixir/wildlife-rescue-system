# Test coverage

The application tests use fictional records and temporary data directories. Run the suite with the Maven Wrapper as described in the [README](../README.md). GitHub Actions runs it on Windows and Linux. The build results are available in the repository Actions tab.

| Requirement | Implementation and test evidence |
|---|---|
| FR01 Incident reports | `IncidentManager`, `ConsoleMenuTest`, `CompleteWorkflowTest` and `RescueIncidentTest` |
| FR02 Team profiles | `TeamRegistry`, `TeamRegistryTest`, `TeamMenuTest` and `RescueTeamTest` |
| FR03 Priority | `PriorityCalculator`, `PriorityCalculatorTest`, `PriorityBoundaryTest` and `PriorityMenuTest` |
| FR04 Matching | `TeamMatcher`, `TeamMatcherTest` and `TeamMatchingMenuTest` |
| FR05 Assignment | `IncidentManager.assign`, `AssignmentTest`, `AssignmentMenuTest` and `CompleteWorkflowTest` |
| FR06 Progress | `IncidentManager.updateStatus`, `ProgressTest`, `ProgressMenuTest` and `CompleteWorkflowTest` |
| FR07 CSV loading | `FileManager.load`, `FileManagerTest` and `CompleteWorkflowTest` |
| FR08 CSV saving | `FileManager`, `FileManagerTest` and `IntegratedConsoleTest` |
| FR09 Rescue reports | `RescueReportService`, `ReportExportTest` and `ReportMenuTest` |
| FR10 Concurrent matching | `ConcurrentMatcher`, `ConcurrentMatcherTest` and `ConcurrentMenuTest` |

`CompleteWorkflowTest` runs the application in separate processes. It covers setup, invalid input, priority, matching, no suitable team, assignment, completion, report export, restart and damaged-file rejection. `FileManagerTest` also checks the second-writer lock and failed saves.

The released ZIP, final sample data and the final design-to-code review remain to be checked. Passing tests do not establish that the student can explain the system in the viva.
