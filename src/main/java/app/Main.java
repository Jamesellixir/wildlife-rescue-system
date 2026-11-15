package app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Scanner;
import service.IncidentManager;
import storage.FileManager;
import ui.ConsoleMenu;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        boolean initialize = args.length == 1 && args[0].equals("--init");
        if (args.length > 0 && !initialize) {
            System.err.println("Usage: java -jar wildlife-rescue-system.jar [--init]");
            System.exit(1);
        }
        try (FileManager store = FileManager.open(Path.of("data"), initialize)) {
            var data = store.load();
            try (Scanner input = new Scanner(System.in)) {
                new ConsoleMenu(input, System.out, new IncidentManager(store, data)).run();
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            System.err.println("Cannot open rescue data: " + e.getMessage());
            System.exit(1);
        }
    }
}
