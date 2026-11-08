# Wildlife Rescue System: coursework grand plan

## 1. Goal and delivery approach

Build a small, reliable **Java console application** that meets the specification, demonstrates object-oriented programming clearly, and is straightforward to explain during the viva.

The target is the A-grade criteria: accurate requirements and UML, code that follows the design, integrated functionality, thorough validation and testing, clear documentation, and a confident demonstration. A grade cannot be guaranteed.

Requirements are based on the CM2601 individual coursework brief for 2026/2027.

Work follows this order:

| Phase | Target dates, November 2026 | Completion condition |
|---|---|---|
| Planning and design | 8–10 | Requirements, rules, UML, tests and implementation plan agree. |
| System implementation | 11–23 inclusive | All required features work together and release checks pass. |
| Report | 24–28 | The report describes the actual verified system and evidence. |
| Review and viva preparation | 29 onward | Submission checks pass; the student practises explaining the application. |

Implementation happens only when the student requests the scheduled part. Dates use **Asia/Colombo**. Missed work is rescheduled honestly, with actual commit timestamps.

## 2. System requirements and behaviour

### Inputs, processes and outputs

| Area | Inputs | Process | Outputs |
|---|---|---|---|
| Incident reporting | Animal category, condition, location, zone, animal count, immediate danger, environmental risk, required skills/equipment, reporter name/contact | Validate fields, generate ID, calculate priority, save incident | Incident ID, score, priority and confirmation |
| Team registration | Team ID/name, members, supported animals, skills, equipment, base zone, maximum workload | Validate details and reject duplicate IDs | Saved team profile |
| Team availability | Team ID and availability choice | Check current assignments before changing availability | Updated team status or explanation |
| Team matching | Pending incident ID | Filter unsuitable teams and rank suitable teams | Ordered candidates with reasons |
| Assignment | Incident ID and chosen team ID | Recheck suitability and capacity, then save assignment | Updated incident and team workload |
| Rescue progress | Incident ID and next status | Validate the transition and save | Updated status; released capacity when completed |
| File loading | Incident and team CSV files | Parse, validate and check relationships | Loaded records or precise error messages |
| Rescue reporting | Current records | Group incidents and calculate totals | Console summary and exported CSV |
| Concurrent matching | Current pending incidents | Search for suitable teams using two worker threads | Matching results for multiple incidents |

**Example:** a trapped bird on a rooftop needs bird-handling skills, climbing skills and climbing equipment. The system calculates its priority, lists suitable teams, and lets the coordinator assign one.

### Console menu

Use one application with these options:

1. Report an incident.
2. View incidents and incident details.
3. Register a rescue team.
4. View teams and change availability.
5. Find suitable teams and assign a team.
6. Update incident status.
7. Generate a rescue report.
8. Check all pending incidents concurrently.
0. Exit.

The reporter, coordinator and rescue team are operational roles. This is a local coursework simulation without login accounts or role-based security.

### Priority algorithm

The brief does not supply a formula. Use the following transparent **project rules**, documented as design decisions rather than wildlife-care standards.

| Factor | Points |
|---|---|
| Condition | Injured: 30; trapped: 25; abandoned: 15; displaced: 10 |
| Immediate danger | Yes: 40; no: 0 |
| Number of animals | One: 0; two to four: 5; five or more: 10 |
| Environmental risk | Low: 0; medium: 10; high: 20 |

Classification:

- **Critical:** immediate danger is present, or score is at least 70.
- **High:** score 45–69 without the Critical override.
- **Medium:** score 25–44.
- **Low:** score below 25.

Display the score breakdown so the result is easy to explain.

Order pending incidents by priority category, then score descending, then reporting time ascending. Use incident ID as the final tie-breaker.

### Team-matching algorithm

A team qualifies only when it:

- Handles the animal category.
- Has every required skill.
- Has every required equipment item.
- Is operationally available.
- Has capacity for another incident.

Rank eligible teams by:

1. Same zone as the incident.
2. Lowest workload-to-capacity ratio.
3. Team ID for a consistent tie-breaker.

A matching zone is a simple location preference; the application does not claim to calculate travel distance.

Show reasons such as “same zone, required equipment available, one of two places occupied.” The coordinator makes the assignment.

Process pending incidents in priority order. Matching suggestions do not reserve teams; assignment always checks the latest capacity.

### Status and capacity rules

Incident progression is:

`Pending Assignment → Assigned → In Progress → Completed`

- New incidents start pending.
- Assignment changes the incident to Assigned.
- Starting a rescue changes it to In Progress.
- Completion releases the team’s occupied capacity.
- Completed incidents remain available for reports and cannot be reopened.
- Invalid transitions are rejected.
- An unsuitable or unavailable assignment leaves the incident unchanged.

Team status is derived:

| Status | Rule |
|---|---|
| Available | Operationally available with no active incidents |
| Assigned | Has active incidents; further assignments are permitted below capacity |
| Unavailable | Manually unavailable |

A team with active incidents cannot be marked unavailable until those incidents are completed. Workload is calculated from Assigned and In Progress incidents, avoiding a second counter that could become inconsistent.

### Validation and file safety

- Animal categories: Bird, Reptile and Mammal.
- Conditions: Injured, Trapped, Displaced and Abandoned.
- Counts and capacity must be positive whole numbers.
- Required fields cannot be blank.
- Skills and equipment use fixed menu choices.
- IDs must be unique; generated incident IDs use UUIDs.
- CSV loading rejects invalid enums, malformed records, duplicate IDs, unknown assigned teams and assignments exceeding capacity.
- Invalid input produces an understandable message and allows another attempt.
- Missing working files are created only through an explicit first-run setup; damaged existing files are never replaced with empty data.
- Validate a complete load before accepting it. Report the file, record and field causing failure.
- Save through a temporary file and atomic replacement. A failed save must not report success or leave an accepted in-memory change.
- Only one running application may write to the data directory; a second instance receives a clear message.

## 3. Technical design and assessment evidence

### Technology and structure

Use **Java 17**, Maven, and conventional Java syntax. Keep the application within five packages:

| Package | Responsibility |
|---|---|
| `model` | Animals, incidents, teams and enums |
| `service` | Priority, matching, assignment, progress and reporting |
| `storage` | CSV parsing, validation and saving |
| `ui` | Console prompts, menus and formatted results |
| `app` | Startup and wiring the components together |

Use normal loops, named methods and explicit conditions. Avoid unnecessary frameworks, clever expressions and classes without a useful responsibility.

Dependencies:

- Apache Commons CSV **1.14.1** for CSV handling, including quoted commas and quotes. This is a published release compatible with the chosen Java version. [Apache release history](https://commons.apache.org/proper/commons-csv/changes.html)
- JUnit Jupiter **5.11.4**, used only for tests. [JUnit guide](https://docs.junit.org/5.11.4/user-guide/index.html)
- Maven Shade Plugin **3.6.2** to package a runnable JAR with its runtime dependencies. [Executable JAR documentation](https://maven.apache.org/plugins/maven-shade-plugin/examples/executable-jar.html)

Pin Maven 3.9.9 through its wrapper, compiler plugin 3.13.0, and Surefire 3.5.2. The final runnable application needs Java 17 or newer; Maven is needed for rebuilding, not normal use.

### Object-oriented design

- `RescueIncident` owns incident details, assignment and permitted status changes.
- `RescueTeam` owns its profile and availability.
- An abstract `Animal` class holds shared animal information.
- `Bird`, `Reptile` and `Mammal` override a method returning their required handling skill.
- `TeamMatcher` uses that method through the `Animal` type, demonstrating meaningful polymorphism.
- `PriorityCalculator` calculates scores without changing records.
- `IncidentManager` coordinates validated changes and persistence.
- `FileManager` handles CSV input/output.
- `RescueReportService` produces summaries and exports.

Use private fields, validating constructors and methods that protect valid state. Do not provide unrestricted setters for IDs, assignment or status.

Equipment and skills are enums because they have no independent behaviour in this scope. A coordinator is an actor using the application, so an empty `RescueCoordinator` class is unnecessary.

### Data contract

Use two UTF-8 CSV files with headers:

- `teams.csv`: ID, name, members, animal categories, skills, equipment, base zone, operational availability and capacity.
- `incidents.csv`: ID, reporting time, animal category, condition, animal count, location, zone, environmental risk, danger flag, required skills/equipment, reporter name/contact, status and assigned team ID.

Separate values within list fields using `|`; reject that character inside an individual list entry. Preserve ordinary commas and quotes through the CSV library.

Recalculate priority and team workload on loading. Assignment and incident status are stored together in the incident record, so assignment does not require synchronising changes across two files.

Use synthetic people and contact details in all public examples.

### Concurrency

Implement concurrent matching for pending incidents using a fixed pool of **two workers**:

- Workers read an immutable snapshot and return suggestions.
- Workers do not alter records or write files.
- Results appear in incident-priority order.
- Display worker names to make concurrency observable without artificial delays.
- Shut down workers cleanly and report failures.
- Assignment remains a synchronised check-and-save operation, preventing capacity from being claimed twice.

### UML and development evidence

Finish before assessed implementation:

- One use case diagram and descriptions for the eight specified use cases.
- Four activity diagrams: reporting, matching/assignment, status updates and report generation, including alternate/error flows.
- One class diagram with relevant fields, operations, inheritance and relationships.
- Three sequence diagrams: reporting/priority, matching/assignment and status update/save.

Review the existing original use case diagram against this plan and preserve earlier versions.

Maintain a requirements-to-evidence table linking each requirement to its diagram, implementation and tests. Record actual test results, screenshots and version-control evidence during development.

Use plain language in comments, commit titles, documentation and report prose. Comments should be occasional, one sentence long, and explain a reason that the code alone does not make clear.

## 4. Repository and dated implementation schedule

Repository: **`Jamesellixir/wildlife-rescue-system`**.

Use `main` for checked work and short-lived `codex/<feature>` branches for implementation. Merge completed features without squashing away the substantive commits.

Publish project files and synthetic examples only.

### Planned commits

Each row is one substantive commit, with relevant tests included alongside its feature.

| Date | Commit title | Work completed |
|---|---|---|
| 8 Nov | `Add the project plan and requirements` | Create repository, README, agreed requirements and schedule. |
| 9 Nov | `Add the system design diagrams` | Complete UML and use case descriptions. |
| 10 Nov | `Set up the Java project and test tools` | Configure Java, Maven, dependencies, test runner and build checks. |
| 11 Nov | `Add animals, incidents and rescue teams` | Implement domain classes, enums and their validation tests. |
| 12 Nov | `Add incident reporting` | Capture reports, generate IDs and display incident details. |
| 13 Nov | `Add rescue team registration` | Register teams, list profiles and manage availability. |
| 14 Nov | `Calculate incident priorities` | Implement scoring, explanations and queue ordering. |
| 15 Nov | `Find suitable rescue teams` | Implement filtering, ranking and rejection explanations. |
| 16 Nov | `Assign teams within their capacity` | Add assignment checks and capacity protection. |
| 17 Nov | `Track rescue progress` | Implement status transitions and capacity release. |
| 18 Nov | `Load and save rescue data` | Add CSV persistence, complete-load validation and safe writes. |
| 19 Nov | `Generate rescue reports` | Add status summaries and CSV exports. |
| 20 Nov | `Check pending incidents with worker threads` | Add concurrent matching and concurrency tests. |
| 21 Nov | `Complete the console menus` | Integrate every workflow, prompts and consistent output. |
| 22 Nov | `Test complete rescue workflows` | Run integration checks, repair discovered faults and update evidence. |
| 23 Nov | `Prepare the application for demonstration` | Verify clean build, packaged run, sample data, instructions and final UML. |

This provides **16 planned commits**, including thirteen implementation and verification stages from 11–23 November. Extra genuine fixes may add commits.

Until persistence is introduced, early milestones explicitly operate in memory. Basic prompts appear with each feature; 21 November completes the integrated interface.

Each milestone ends with:

- Actual checks and diff review.
- A plain explanation of the change and its algorithm.
- A small demonstration the student can run.
- The commit link and remaining limitations.

No scheduled work runs automatically. If a date slips, retain the dependency order and revise the remaining schedule.

## 5. Verification, report and completion

### Required tests

| Area | Acceptance scenarios |
|---|---|
| Reporting | Valid report; invalid animal; missing location/contact; zero or negative count; unique IDs |
| Priority | Every condition; danger override; count bands; threshold boundaries; deterministic ordering |
| Matching | Missing skill/equipment; wrong animal; unavailable/full team; same-zone preference; workload ties; no match |
| Assignment | Valid assignment; stale suggestion; already-assigned incident; competing requests for the final place |
| Progress | Every permitted transition; invalid transitions; completion releases capacity exactly once |
| CSV | Save/reload equivalence; commas/quotes; malformed rows; duplicates; missing team; invalid status; failed writes preserve accepted data |
| Concurrency | Multiple workers process pending incidents; results agree with sequential matching; errors handled; clean shutdown |
| Reporting | Correct completed/pending totals, active incidents distinguished, empty data and valid exports |
| Packaging | Clean build; extracted ZIP starts outside the development folder; restart preserves records; second writer refused |

Run `mvnw clean verify` for the release. Build checks run on pushes and pull requests. Test counts and results must come from execution.

The demonstration must cover reporting, priority, matching, assignment, completion, restart/reload, invalid input, no suitable team and concurrent matching.

### Report after system completion

Target approximately **1,800 words**, within the specified 1,500–2,000 range:

| Section | Approximate words |
|---|---:|
| Introduction, scope and timeline | 180 |
| Requirements, stakeholders and use case descriptions | 420 |
| Activity diagrams | 180 |
| Class design and OOP decisions | 300 |
| Sequence diagrams | 180 |
| Testing, evaluation and limitations | 540 |

Include references and an appendix containing readable version-control screenshots with timestamps. Check word counting against the brief, including headings, in-text citations and text-heavy tables.

Use the required Harvard referencing format and cite only sources actually consulted.

### Final deliverables

- **`<RGU-ID>.zip`**: source, tests, Maven configuration/wrapper, runnable JAR, synthetic sample CSV files, dependency notices and run instructions.
- **`<RGU-ID>.pdf`**: the checked report with readable diagrams and evidence.
- Usage/readiness notes and viva preparation material beside the submission files.

Resolve the registered RGU ID from approved identity records before packaging. Verify filenames, internal identification, report length, references, layout and execution from a clean extraction.

The application is complete only after checks pass. The coursework is ready for review only after the report and packaging checks pass. Submission remains the student’s action.
