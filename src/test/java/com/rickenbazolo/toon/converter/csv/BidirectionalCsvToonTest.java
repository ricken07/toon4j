package com.rickenbazolo.toon.converter.csv;

import com.rickenbazolo.toon.Toon;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for bidirectional CSV ↔ TOON conversions.
 *
 * @author Ricken Bazolo
 */
class BidirectionalCsvToonTest {

    @Test
    void testRoundTripBasic() throws IOException {
        String originalCsv = "id,name,age\n1,Alice,30\n2,Bob,25\n3,Carol,35";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        // Verify all data is preserved (columns may be reordered alphabetically)
        assertThat(resultCsv).contains("id").contains("name").contains("age");
        assertThat(resultCsv).contains("Alice").contains("30");
        assertThat(resultCsv).contains("Bob").contains("25");
        assertThat(resultCsv).contains("Carol").contains("35");
    }

    @Test
    void testRoundTripWithTypes() throws IOException {
        String originalCsv = "id,name,price,available\n1,Laptop,999.99,true\n2,Mouse,24.99,false";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        // Verify all data and types are preserved (columns may be reordered)
        assertThat(resultCsv).contains("id").contains("name").contains("price").contains("available");
        assertThat(resultCsv).contains("Laptop").contains("999.99").contains("true");
        assertThat(resultCsv).contains("Mouse").contains("24.99").contains("false");
    }

    @Test
    void testRoundTripWithSemicolon() throws IOException {
        String originalCsv = "id;name;country\n1;Alice;France\n2;Bob;Germany";

        CsvToToonOptions csvToToonOptions = CsvToToonOptions.builder()
            .delimiter(';')
            .build();
        ToonToCsvOptions toonToCsvOptions = ToonToCsvOptions.builder()
            .delimiter(';')
            .build();

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv, csvToToonOptions);
        String resultCsv = Toon.toCsv(toon, toonToCsvOptions);

        // Columns may be reordered (alphabetically)
        assertThat(resultCsv).contains("Alice").contains("France");
        assertThat(resultCsv).contains("Bob").contains("Germany");
    }

    @Test
    void testRoundTripWithQuotedFields() throws IOException {
        String originalCsv = "id,name,description\n1,Product A,\"Text with, comma\"\n2,Product B,Normal text";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        assertThat(resultCsv).contains("Product A");
        assertThat(resultCsv).contains("Text with, comma");
        assertThat(resultCsv).contains("Product B");
        assertThat(resultCsv).contains("Normal text");
    }

    @Test
    void testRoundTripWithNulls() throws IOException {
        String originalCsv = "id,name,email\n1,Alice,alice@example.com\n2,Bob,\n3,Carol,carol@example.com";

        CsvToToonOptions csvOptions = CsvToToonOptions.builder()
            .emptyValueHandling(CsvToToonOptions.EmptyValueHandling.NULL)
            .build();
        ToonToCsvOptions toonOptions = ToonToCsvOptions.builder()
            .nullValue("")
            .build();

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv, csvOptions);
        String resultCsv = Toon.toCsv(toon, toonOptions);

        // Columns may be reordered (alphabetically: email, id, name)
        assertThat(resultCsv).contains("Alice").contains("alice@example.com");
        assertThat(resultCsv).contains("Carol").contains("carol@example.com");
    }

    @Test
    void testRoundTripSingleColumn() throws IOException {
        String originalCsv = "name\nAlice\nBob\nCarol";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        // Verify data is preserved
        assertThat(resultCsv).contains("name");
        assertThat(resultCsv).contains("Alice");
        assertThat(resultCsv).contains("Bob");
        assertThat(resultCsv).contains("Carol");
    }

    @Test
    void testRoundTripLargeDataset() throws IOException {
        StringBuilder originalCsv = new StringBuilder("id,value,description\n");
        for (int i = 1; i <= 100; i++) {
            originalCsv.append(i).append(",").append(i * 10).append(",Description ").append(i).append("\n");
        }

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv.toString());
        String resultCsv = Toon.toCsv(toon);

        // Verify sample rows (columns may be reordered)
        assertThat(resultCsv).contains("id").contains("value").contains("description");
        assertThat(resultCsv).contains("Description 1");
        assertThat(resultCsv).contains("Description 50");
        assertThat(resultCsv).contains("Description 100");
    }

    @Test
    void testToonToCsvToToon() throws IOException {
        // Use alphabetically sorted columns with data wrapper
        String originalToon = "data[3]{age,id,name}:\n30,1,Alice\n25,2,Bob\n35,3,Carol";

        // TOON → CSV → TOON
        String csv = Toon.toCsv(originalToon);
        String resultToon = Toon.fromCsv(csv);

        // Verify data is preserved
        assertThat(resultToon).contains("{age,id,name}");
        assertThat(resultToon).contains("Alice");
        assertThat(resultToon).contains("Bob");
        assertThat(resultToon).contains("Carol");
    }

    @Test
    void testRoundTripMixedTypes() throws IOException {
        String originalCsv = "id,name,score,passed,grade\n1,Alice,95.5,true,A\n2,Bob,72.3,true,C\n3,Carol,45.8,false,F";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        // Verify data is preserved (columns may be reordered)
        assertThat(resultCsv).contains("Alice").contains("95.5").contains("true").contains("A");
        assertThat(resultCsv).contains("Bob").contains("72.3").contains("C");
        assertThat(resultCsv).contains("Carol").contains("45.8").contains("false").contains("F");
    }

    @Test
    void testRoundTripWithSpecialCharacters() throws IOException {
        String originalCsv = "id,text\n1,\"Hello \"\"World\"\"\"\n2,Line1";

        // CSV → TOON → CSV
        String toon = Toon.fromCsv(originalCsv);
        String resultCsv = Toon.toCsv(toon);

        assertThat(resultCsv).contains("id,text");
    }

    @Test
    void testDataIntegrity() throws IOException {

        String originalCsv = """
                product_id,product_name,price,in_stock,category
                101,Laptop Computer,1299.99,true,Electronics
                102,Wireless Mouse,29.99,true,Accessories
                103,USB Cable,9.99,false,Accessories
                104,Monitor 27 inch,399.99,true,Electronics
                105,Keyboard Mechanical,149.99,true,Accessories
                """;

        // CSV → TOON
        String toon = Toon.fromCsv(originalCsv);

        // Verify TOON contains array notation (columns sorted alphabetically)
        assertThat(toon).contains("[5]");
        assertThat(toon).contains("{category,in_stock,price,product_id,product_name}");

        // TOON → CSV
        String resultCsv = Toon.toCsv(toon);

        // Verify all data is preserved (columns may be reordered)
        assertThat(resultCsv).contains("Laptop Computer").contains("1299.99").contains("Electronics");
        assertThat(resultCsv).contains("Wireless Mouse").contains("29.99").contains("Accessories");
        assertThat(resultCsv).contains("USB Cable").contains("9.99");
        assertThat(resultCsv).contains("Monitor 27 inch").contains("399.99");
        assertThat(resultCsv).contains("Keyboard Mechanical").contains("149.99");
    }

    @Test
    void testToonIntermediateFormat() throws IOException {
        String originalCsv = "id,name,score\n1,Alice,95\n2,Bob,87\n3,Carol,92";

        // Convert CSV to TOON
        String toon = Toon.fromCsv(originalCsv);

        // TOON should contain wrapped array notation (columns sorted alphabetically)
        assertThat(toon).contains("data[3]");
        assertThat(toon).contains("{id,name,score}");

        // Convert back to CSV
        String resultCsv = Toon.toCsv(toon);

        // Data should be preserved
        assertThat(resultCsv).contains("Alice").contains("95");
        assertThat(resultCsv).contains("Bob").contains("87");
        assertThat(resultCsv).contains("Carol").contains("92");
    }
}
