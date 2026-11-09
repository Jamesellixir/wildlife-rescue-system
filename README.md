# Wildlife Rescue System

A Java console application for recording wildlife rescue incidents, calculating urgency and assigning suitable rescue teams.

## Project status

Planning started on 8 November 2026. The UML design and use case descriptions were completed on 9 November. The Java project and build checks are ready for implementation.

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

The build compiles the project, runs the JUnit setup test and creates `target/wildlife-rescue-system.jar`. Run `java -jar target/wildlife-rescue-system.jar` to see the starter message.

The interface will use a numbered console menu. Sample records will use fictional names and contact details.
