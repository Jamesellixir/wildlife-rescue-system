package ui;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import model.Animal;
import model.AnimalCategory;
import model.Bird;
import model.Condition;
import model.EnvironmentalRisk;
import model.Equipment;
import model.Mammal;
import model.Reptile;
import model.RescueIncident;
import model.Skill;
import service.IncidentManager;

public final class ConsoleMenu {
    private final Scanner input;
    private final PrintStream output;
    private final IncidentManager manager;

    public ConsoleMenu(Scanner input, PrintStream output, IncidentManager manager) {
        this.input = input;
        this.output = output;
        this.manager = manager;
    }

    public void run() {
        output.println("Wildlife Rescue System");
        output.println("Reports are kept for this session.");
        try {
            while (true) {
                output.println("\n1. Report an incident");
                output.println("2. View incidents");
                output.println("0. Exit");
                switch (readLine("Choice")) {
                    case "1" -> reportIncident();
                    case "2" -> viewIncidents();
                    case "0" -> {
                        return;
                    }
                    default -> output.println("Choose 0, 1 or 2.");
                }
            }
        } catch (NoSuchElementException endOfInput) {
            output.println("\nInput ended. Closing the console.");
        }
    }

    private void reportIncident() {
        AnimalCategory category = readChoice("Animal category", AnimalCategory.values());
        Condition condition = readChoice("Animal condition", Condition.values());
        int count = readNumber("Animal count", 1, Integer.MAX_VALUE);
        Animal animal = switch (category) {
            case BIRD -> new Bird(condition, count);
            case REPTILE -> new Reptile(condition, count);
            case MAMMAL -> new Mammal(condition, count);
        };
        String location = readRequired("Location");
        String zone = readRequired("Zone");
        boolean immediateDanger = readYesNo("Immediate danger (y/n)");
        EnvironmentalRisk risk = readChoice("Environmental risk", EnvironmentalRisk.values());
        Set<Skill> skills = readMultiple("Additional required skills", Skill.class);
        Set<Equipment> equipment = readMultiple("Required equipment", Equipment.class);
        String reporterName = readRequired("Reporter name");
        String reporterContact = readRequired("Reporter contact");

        RescueIncident incident = manager.reportIncident(animal, location, zone, immediateDanger,
                risk, skills, equipment, reporterName, reporterContact);
        output.println("Incident recorded. ID: " + incident.id());
        output.println("Status: " + label(incident.status()));
    }

    private void viewIncidents() {
        var incidents = manager.listIncidents();
        if (incidents.isEmpty()) {
            output.println("No incidents recorded.");
            return;
        }

        for (RescueIncident incident : incidents) {
            output.printf("%s | %s | %s | %s%n", incident.id(), label(incident.animal().category()),
                    incident.location(), label(incident.status()));
        }
        String enteredId = readLine("Incident ID for details (Enter to return)");
        if (enteredId.isEmpty()) {
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(enteredId);
        } catch (IllegalArgumentException invalidId) {
            output.println("Invalid incident ID.");
            return;
        }

        manager.findIncident(id).ifPresentOrElse(this::showDetails,
                () -> output.println("No incident found for that ID."));
    }

    private void showDetails(RescueIncident incident) {
        Animal animal = incident.animal();
        output.println("Incident ID: " + incident.id());
        output.println("Reported at: " + incident.reportedAt());
        output.println("Animal category: " + label(animal.category()));
        output.println("Animal condition: " + label(animal.condition()));
        output.println("Animal count: " + animal.count());
        output.println("Location: " + incident.location());
        output.println("Zone: " + incident.zone());
        output.println("Immediate danger: " + (incident.immediateDanger() ? "Yes" : "No"));
        output.println("Environmental risk: " + label(incident.risk()));
        output.println("Animal handling skill: " + label(animal.requiredHandlingSkill()));
        output.println("Additional skills: " + labels(incident.requiredSkills()));
        output.println("Equipment: " + labels(incident.requiredEquipment()));
        output.println("Reporter name: " + incident.reporterName());
        output.println("Reporter contact: " + incident.reporterContact());
        output.println("Status: " + label(incident.status()));
    }

    private String readLine(String prompt) {
        output.print(prompt + ": ");
        return input.nextLine().strip();
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            output.println(prompt + " is required.");
        }
    }

    private int readNumber(String prompt, int minimum, int maximum) {
        while (true) {
            String inputText = readLine(prompt);
            try {
                int number = Integer.parseInt(inputText);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException invalidNumber) {
                // The prompt below covers non-numeric and out-of-range input.
            }
            if (maximum == Integer.MAX_VALUE) {
                output.println("Enter a positive whole number.");
            } else {
                output.printf("Enter a number from %d to %d.%n", minimum, maximum);
            }
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt);
            if (answer.equalsIgnoreCase("y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n")) {
                return false;
            }
            output.println("Enter y or n.");
        }
    }

    private <E extends Enum<E>> E readChoice(String prompt, E[] choices) {
        showChoices(prompt, choices);
        return choices[readNumber("Choice", 1, choices.length) - 1];
    }

    private <E extends Enum<E>> Set<E> readMultiple(String prompt, Class<E> type) {
        E[] choices = type.getEnumConstants();
        showChoices(prompt, choices);
        while (true) {
            String answer = readLine("Comma-separated numbers (Enter for none)");
            Set<E> selected = EnumSet.noneOf(type);
            if (answer.isEmpty()) {
                return selected;
            }
            try {
                for (String entry : answer.split(",", -1)) {
                    int index = Integer.parseInt(entry.strip()) - 1;
                    if (index < 0 || index >= choices.length) {
                        throw new NumberFormatException();
                    }
                    selected.add(choices[index]);
                }
                return selected;
            } catch (NumberFormatException invalidChoice) {
                output.println("Enter only numbers shown in the list.");
            }
        }
    }

    private void showChoices(String prompt, Enum<?>[] choices) {
        output.println(prompt);
        for (int i = 0; i < choices.length; i++) {
            output.printf("%d. %s%n", i + 1, label(choices[i]));
        }
    }

    private String label(Enum<?> value) {
        String words = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private String labels(Set<? extends Enum<?>> values) {
        if (values.isEmpty()) {
            return "None";
        }
        return String.join(", ", values.stream().map(this::label).sorted().toList());
    }
}
