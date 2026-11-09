# Wildlife rescue use cases

The reporter submits an incident. The rescue coordinator registers teams, reviews the queue, assigns teams and generates reports. The rescue team views assigned work and records progress. The console models these roles through their tasks.

The eight use cases appear in the [use case diagram](use-case.svg). Team availability and concurrent matching are supporting flows of UC04 and UC05.

## UC01 Report Incident

**Actor** — Reporter, a member of the public or a staff member.

**Precondition** — The console is open and the data store is ready.

1. The reporter enters the animal category, condition, count, location, zone, danger, environmental risk, required skills and equipment, and contact details.
2. The system validates the fields and creates a unique incident ID.
3. The system performs UC03 and stages a Pending Assignment record.
4. The system saves the record and displays its ID, priority and score breakdown.

**Alternatives** — For invalid details, identify the field and allow correction. If saving fails, discard the staged record and report the error.

**Result** — A successful report adds one pending incident.

## UC02 View Incident

**Actors** — Rescue coordinator and rescue team.

**Precondition** — The actor opens the incident view.

1. The system lists incidents. Pending incidents appear in priority order.
2. The actor selects an incident ID. A rescue team uses this view to find its assigned incidents.
3. The system displays the details, priority, status and assigned team when present.

**Alternatives** — An empty list shows that there are no incidents. An unknown ID produces a message and allows another selection.

**Result** — The actor can read the incident without changing it.

## UC03 Assess Incident Priority

**Trigger** — UC01 invokes this calculation after validating the incident fields.

**Precondition** — Condition, count, danger and risk are valid.

1. The system calculates the contribution from each factor using the approved scoring table.
2. It classifies immediate danger as Critical. Otherwise, it uses the score thresholds.
3. It returns the score, classification and breakdown to the reporting flow.

**Alternatives** — If validation fails, UC01 asks the reporter to correct the input before scoring.

**Result** — The reporting flow receives a priority that can be explained and reproduced.

## UC04 Register Rescue Team

**Actor** — Rescue coordinator.

**Precondition** — The data store is ready.

1. The coordinator enters a team ID, name, members, animal categories, skills, equipment, base zone, availability and capacity.
2. The system checks the fields, confirms capacity is positive and rejects duplicate IDs.
3. The system saves the profile and displays the team's status.

**Alternatives** — Invalid fields or duplicate IDs prompt correction. A failed save leaves the profile list unchanged. In the team view, the coordinator may change availability. The system refuses to mark a team unavailable while it has active incidents and saves a permitted change before confirming it.

**Result** — A valid saved profile can be considered for matching.

## UC05 Search Available Teams

**Actor** — Rescue coordinator.

**Precondition** — The selected incident is pending.

1. The system filters teams by animal category, required skills, required equipment, availability and spare capacity.
2. It ranks eligible teams by matching zone, then lowest workload-to-capacity ratio, then team ID.
3. It displays the ranked candidates and explains why they qualify.

**Alternatives** — A missing or non-pending incident prompts another selection. If no team qualifies, the incident stays pending. The batch menu option runs matching for pending incidents on an immutable snapshot with two workers and displays results in priority order. A worker failure produces an error for the affected search.

**Result** — The coordinator receives suggestions. Searching does not reserve or assign a team.

## UC06 Assign Rescue Team

**Actor** — Rescue coordinator.

**Precondition** — A pending incident has been selected.

1. The system performs UC05.
2. The coordinator selects a candidate.
3. The manager rechecks pending status and current eligibility within the same synchronised operation that saves the assignment.
4. The system stages Assigned status and the team ID, saves them together, then confirms the assignment and displays the derived workload.

**Alternatives** — With no eligible team or no selection, the incident remains pending. If the incident changed or the team no longer qualifies, show the rejection reason. On save failure, discard the staged assignment.

**Result** — A successful assignment occupies one place in the selected team's capacity.

## UC07 Update Incident Status

**Actors** — Rescue team and rescue coordinator.

**Precondition** — The incident is Assigned or In Progress.

1. The actor enters the incident ID and requested status.
2. The system permits Assigned to In Progress or In Progress to Completed.
3. The system saves the staged status and confirms the change. Completion removes the incident from the active workload.

**Alternatives** — An unknown ID or invalid transition produces an explanation. A failed save retains the previous status and workload.

**Result** — The saved incident shows the new status. A completed incident remains available for reporting.

## UC08 Generate Rescue Report

**Actor** — Rescue coordinator.

**Precondition** — The application has loaded its incident records.

1. The coordinator requests a report.
2. The system groups records by status and displays totals for pending, assigned, in progress and completed incidents.
3. The system writes a CSV report and displays its path.

**Alternatives** — An empty dataset produces zero totals and a header-only export. If exporting fails, display the error and retain the console summary.

**Result** — The coordinator can review totals and, on success, open the exported CSV.
