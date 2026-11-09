# System design

This design follows the [approved project plan](../project-plan.md) and the CM2601 coursework brief. Method signatures show the intended responsibilities and may be refined to match the implementation.

| Diagram | What to check |
|---|---|
| [Use case](use-case.svg) | The three actors, eight use cases and two required included behaviours |
| [Activity: reporting](activity-reporting.svg) | Validation, priority and save confirmation |
| [Activity: matching and assignment](activity-assignment.svg) | No match, coordinator selection, eligibility recheck and save failure |
| [Activity: status updates](activity-status.svg) | Valid transitions, persistence and capacity release |
| [Activity: rescue report](activity-report.svg) | Empty dataset, totals and CSV export |
| [Class model](class-model.svg) | Encapsulation, animal inheritance, incident and team relationships, and service responsibilities |
| [Sequence: reporting](sequence-reporting.svg) | Console, manager, priority calculation and file save |
| [Sequence: assignment](sequence-assignment.svg) | Search, coordinator selection and assignment recheck |
| [Sequence: status update](sequence-status.svg) | Transition validation and persistence before confirmation |

[Use case descriptions](use-cases.md) give the actors, preconditions, normal flow, alternatives and results. Open the SVG links in a browser to zoom in.

## Traceability

The [requirements matrix](../requirements.md) lists planned acceptance checks. Implementation and test references will be added when the corresponding work is completed.

| Requirement | Design evidence | Planned check |
|---|---|---|
| FR01 Reporting | UC01, reporting activity and sequence | Required fields, unique ID, save failure |
| FR02 Team profiles | UC04, class model | Duplicate ID, capacity and availability |
| FR03 Priority | UC03, reporting activity and sequence, PriorityCalculator | Score bands, danger override and queue ties |
| FR04 Matching | UC05, assignment activity and sequence, TeamMatcher | Required capability, capacity and ranking |
| FR05 Assignment | UC06, assignment activity and sequence | No match, stale choice and final slot |
| FR06 Progress | UC07, status activity and sequence | Valid transitions, failed save and capacity release |
| FR07 CSV load | Class model, FileManager loadAll | Malformed CSV, duplicates and unknown team |
| FR08 CSV save | Reporting, assignment and status sequences | Save and reload, failed write |
| FR09 Rescue report | UC08, report activity, RescueReportService | Correct totals, empty records and export error |
| FR10 Concurrency | UC05 batch flow, IncidentManager matchPendingConcurrently | Two workers, equivalent results and shutdown |
| QR01 OOP design | Class model | Check encapsulation, inheritance and polymorphism against code |
| QR02 Errors and data safety | Activity and sequence alternatives | Check recovery and unchanged state after failed writes |
| QR03 Console interface | Use cases and ConsoleMenu | Run a complete rescue workflow |
| QR04 Packaging | Project plan | Run the extracted ZIP on Java 17 |
| QR05 Consistency | This design index and requirements matrix | Match the final code, tests and report to the design |

For the viva, follow a single report through the diagrams. A trapped bird is reported, scored and saved as pending. The coordinator searches for a team with bird-handling skills and climbing equipment, selects one, and the system checks capacity again before saving the assignment.
