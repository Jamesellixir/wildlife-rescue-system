# Wildlife Rescue System

A Java console application for recording wildlife rescue incidents, calculating urgency and assigning suitable rescue teams.

## Project status

The console supports incident reports, team registration, matching, assignment, progress updates and local CSV storage. Reports and concurrent matching are planned.

## Planned features

- Record incidents and calculate Critical, High, Medium or Low priority.
- Register teams and match their skills, equipment, location and available capacity.
- Track assignments and rescue progress.
- Load and save CSV data and export rescue reports.
- Search for teams for multiple incidents using worker threads.

## Documentation

- [Project plan](docs/project-plan.md)
- [Requirements and acceptance criteria](docs/requirements.md)
- [Development schedule](docs/schedule.md)
- [UML diagrams and use case descriptions](docs/design/README.md)

## Build and test

Install a Java 17 JDK and set JAVA_HOME to its installation folder. The Maven Wrapper downloads Maven 3.9.9 on the first run.

On Windows, run `.\mvnw.cmd clean verify`. On macOS or Linux, run `./mvnw clean verify`.

The build runs the JUnit tests and creates `target/wildlife-rescue-system.jar`. On first use, run `java -jar target/wildlife-rescue-system.jar --init` from the chosen working directory. This creates `data/teams.csv` and `data/incidents.csv`. Use `java -jar target/wildlife-rescue-system.jar` on later runs. Start only one instance for a data directory. If either data file is missing or damaged, the application reports an error without replacing it.

The console uses numbered menus. Use fictional names and contact details in examples.
