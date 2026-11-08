# Requirements and acceptance criteria

Source: CM2601 individual coursework brief, 2026/2027, Urban Wildlife Rescue Coordination System. The brief is retained separately from this public repository.

Status: planned. No requirement is marked implemented or tested yet.

## Functional requirements

| ID | Requirement | Acceptance evidence planned |
|---|---|---|
| FR01 | Report animal type, condition, location, count, danger, special requirements and reporter contact; generate a unique ID | Valid report and invalid-input tests; report use case, activity and sequence diagrams |
| FR02 | Register teams with members, animal categories, skills, equipment, status and maximum simultaneous incidents | Profile, duplicate-ID and availability tests; register-team use case and class diagram |
| FR03 | Automatically calculate and display Critical, High, Medium or Low priority | Scoring boundaries, immediate-danger override and queue-order tests; priority use case |
| FR04 | Match animal category, skills, equipment, availability and workload; consider location | Missing capability, unavailable/full team and ranking tests; matching activity and sequence diagrams |
| FR05 | Let the coordinator assign a suitable team; leave incidents pending when no team qualifies | Valid/rejected assignment and final-capacity concurrency tests; assignment use case |
| FR06 | Track Assigned, In Progress and Completed incidents and release capacity at completion | Valid/invalid transition and repeated-completion tests; status activity and sequence diagrams |
| FR07 | Load incident and team information from CSV | Complete-load validation, malformed CSV, duplicate ID and unknown-team tests |
| FR08 | Save incidents, assignments and status changes | Save/reload tests and failed-write preservation tests |
| FR09 | Generate completed and pending rescue reports | Status totals, empty-data and CSV-export tests; report activity diagram |
| FR10 | Demonstrate concurrent processing with threads | Two-worker matching, sequential-equivalence and shutdown tests |

## Quality and design requirements

| ID | Requirement | Acceptance evidence planned |
|---|---|---|
| QR01 | Demonstrate classes, objects, constructors, encapsulation, access control, inheritance, abstraction and polymorphism | Class diagram mapped to actual classes and tests; viva examples |
| QR02 | Handle invalid input and file failures clearly without accepting damaged state | Validation, persistence-failure and startup tests |
| QR03 | Provide a simple, integrated console interface with clear output | Manual complete-workflow demonstration and input recovery checks |
| QR04 | Package everything needed to run on a desktop with Java 17 or newer | Clean build and extracted-ZIP run outside the development directory |
| QR05 | Keep source, diagrams and report consistent | Requirements-to-code-to-test review before release |

## Required coursework evidence

- One use case diagram with descriptions, activity diagrams, one class diagram and sequence diagrams.
- Working Java application ZIP and a 1,500 to 2,000 word PDF report.
- Version-control screenshots covering development from start to finish with clear timestamps.
- Weekly in-class assessment work, tracked separately without claiming unobserved completion.
- Mandatory demonstration and physical viva; participation and understanding must be demonstrated by the student.
- Final ZIP and PDF filenames use only the RGU ID; internal identification follows the brief.
- Deadline stated in the brief: 8 December 2026, 5 p.m.; the brief does not specify the timezone.

## Design decisions

Priority weights, thresholds, zones, team ranking, status transitions and CSV contracts are project decisions defined in [the project plan](project-plan.md). They are not supplied wildlife-care standards.

The first version uses a local console menu, CSV storage and two matching workers. Accounts, a database, a web interface, mapping services and automatic dispatch are outside the agreed scope.

## Verification record

Add actual class and test references as each feature is implemented. Keep pending, implemented and verified states separate; a planned test is not a passing test.
