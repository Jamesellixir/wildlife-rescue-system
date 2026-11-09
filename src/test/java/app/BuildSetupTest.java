package app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;

class BuildSetupTest {
    @Test
    void readsAQuotedLocationWithAComma() throws IOException {
        String row = "BIRD,\"Park Road, Colombo\"\r\n";

        try (CSVParser parser = CSVParser.parse(row, CSVFormat.RFC4180)) {
            assertEquals("Park Road, Colombo", parser.getRecords().get(0).get(1));
        }
    }
}
