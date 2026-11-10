package app;

import java.util.Scanner;
import service.IncidentManager;
import ui.ConsoleMenu;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            new ConsoleMenu(input, System.out, new IncidentManager()).run();
        }
    }
}
