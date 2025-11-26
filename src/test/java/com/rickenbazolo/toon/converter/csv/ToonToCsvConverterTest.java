package com.rickenbazolo.toon.converter.csv;

import com.rickenbazolo.toon.exception.CsvException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ToonToCsvConverter}.
 *
 * @author Ricken Bazolo
 */
class ToonToCsvConverterTest {

    @Test
    void testBasicConversion() throws IOException {
        // Note: TOON encoder sorts columns alphabetically
        String toon = "data[2]{age,id,name}:\n30,1,Alice\n25,2,Bob";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        String csv = converter.convert(toon);

        assertThat(csv).contains("age,id,name");
        assertThat(csv).contains("30,1,Alice");
        assertThat(csv).contains("25,2,Bob");
    }

    @Test
    void testArrayExtraction() throws IOException {
        // Columns are sorted alphabetically
        String toon = "users[2]{id,name}:\n  1,Alice\n  2,Bob";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        String csv = converter.convert(toon);

        assertThat(csv).contains("id,name");
        assertThat(csv).contains("1,Alice");
        assertThat(csv).contains("2,Bob");
    }

    @Test
    void testHeaderInclusion() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .includeHeader(true)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        String[] lines = csv.split("\\r?\\n");
        assertThat(lines[0]).contains("id").contains("name");
    }

    @Test
    void testHeaderExclusion() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .includeHeader(false)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).doesNotContain("id,name");
        assertThat(csv).contains("1,Alice");
        assertThat(csv).contains("2,Bob");
    }

    @Test
    void testCustomDelimiterTab() throws IOException {
        // Columns sorted alphabetically: age, id, name
        String toon = "data[2]{age,id,name}:\n30,1,Alice\n25,2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .delimiter('\t')
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("\t");
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("Bob");
    }

    @Test
    void testCustomDelimiterPipe() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .delimiter('|')
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("id|name");
        assertThat(csv).contains("1|Alice");
    }

    @Test
    void testNullValueHandling() throws IOException {
        // Columns sorted: email, id, name
        String toon = "data[2]{email,id,name}:\nalice@example.com,1,Alice\nnull,2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .nullValue("N/A")
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("Alice");
        assertThat(csv).contains("N/A");
        assertThat(csv).contains("Bob");
    }

    @Test
    void testQuoteModeAll() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .quoteMode(ToonToCsvOptions.QuoteMode.ALL)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("\"id\",\"name\"");
        assertThat(csv).contains("\"1\",\"Alice\"");
    }

    @Test
    void testQuoteModeMinimal() throws IOException {
        // Columns sorted: description, id, name
        String toon = "data[2]{description,id,name}:\nNormal text,1,Alice\n\"Text with, comma\",2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .quoteMode(ToonToCsvOptions.QuoteMode.MINIMAL)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        // Fields with commas should be quoted
        assertThat(csv).contains("Text with, comma");
    }

    @Test
    void testAutoDetectArray() throws IOException {
        String toon = "status: success\nusers[2]{id,name}:\n  1,Alice\n  2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .autoDetectArray(true)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("id,name");
        assertThat(csv).contains("1,Alice");
    }

    @Test
    void testFileOutput() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        Path tempFile = Files.createTempFile("test", ".csv");

        try {
            ToonToCsvConverter converter = new ToonToCsvConverter();
            converter.convert(toon, tempFile.toFile());

            String csv = Files.readString(tempFile);
            assertThat(csv).contains("id,name");
            assertThat(csv).contains("1,Alice");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testNullToonStringThrowsException() {
        ToonToCsvConverter converter = new ToonToCsvConverter();

        assertThatThrownBy(() -> converter.convert((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TOON string cannot be null");
    }

    @Test
    void testNullOptionsThrowsException() {
        assertThatThrownBy(() -> new ToonToCsvConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ToonToCsvOptions cannot be null");
    }

    @Test
    void testNoArrayFoundThrowsException() {
        String toon = "name: Alice\nage: 30";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        assertThatThrownBy(() -> converter.convert(toon))
            .isInstanceOf(CsvException.class)
            .hasMessageContaining("no array found");
    }

    @Test
    void testPrimitiveArray() throws IOException {
        // Primitive array in list format (requires "- " prefix)
        String toon = "data[3]:\n  - Alice\n  - Bob\n  - Carol";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        String csv = converter.convert(toon);

        assertThat(csv).contains("value");
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("Bob");
        assertThat(csv).contains("Carol");
    }

    @Test
    void testEmptyArray() throws IOException {
        String toon = "data[]";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        String csv = converter.convert(toon);

        assertThat(csv).isEmpty();
    }

    @Test
    void testSingleRow() throws IOException {
        String toon = "data[1]{id,name}:\n1,Alice";
        ToonToCsvConverter converter = new ToonToCsvConverter();

        String csv = converter.convert(toon);

        assertThat(csv).contains("id");
        assertThat(csv).contains("name");
        assertThat(csv).contains("Alice");
    }

    @Test
    void testLineEndings() throws IOException {
        String toon = "data[2]{id,name}:\n1,Alice\n2,Bob";
        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .lineEnding("\r\n")
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);

        String csv = converter.convert(toon);

        assertThat(csv).contains("\r\n");
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("Bob");
    }

    @Test
    void testFileInputToonToCSV() throws IOException {
        // Create a temporary TOON file
        Path tempToonFile = Files.createTempFile("test", ".toon");
        String toonContent = "data[3]{age,city,name}:\n28,NYC,Alice\n35,LA,Bob\n42,Chicago,Carol";
        Files.writeString(tempToonFile, toonContent);

        try {
            ToonToCsvConverter converter = new ToonToCsvConverter();
            // Read TOON from file and convert to CSV
            String toon = Files.readString(tempToonFile);
            String csv = converter.convert(toon);

            assertThat(csv).contains("age,city,name");
            assertThat(csv).contains("Alice");
            assertThat(csv).contains("Bob");
            assertThat(csv).contains("Carol");
        } finally {
            Files.deleteIfExists(tempToonFile);
        }
    }

    @Test
    void testFileToFileConversion() throws IOException {
        // Create temporary TOON input file
        Path tempToonFile = Files.createTempFile("input", ".toon");
        String toonContent = "data[2]{department,employee_id,salary}:\nEngineering,E001,85000\nSales,E002,65000";
        Files.writeString(tempToonFile, toonContent);

        // Create temporary CSV output file
        Path tempCsvFile = Files.createTempFile("output", ".csv");

        try {
            // Read TOON from file
            String toon = Files.readString(tempToonFile);

            ToonToCsvConverter converter = new ToonToCsvConverter();
            // Write CSV to file
            converter.convert(toon, tempCsvFile.toFile());

            // Verify output
            String csv = Files.readString(tempCsvFile);
            assertThat(csv).contains("department,employee_id,salary");
            assertThat(csv).contains("Engineering");
            assertThat(csv).contains("Sales");
            assertThat(csv).contains("85000");
        } finally {
            Files.deleteIfExists(tempToonFile);
            Files.deleteIfExists(tempCsvFile);
        }
    }

    @Test
    void testRealisticProductCatalog() throws IOException {
        String toon = """
                data[5]{available,category,price,product_id,product_name}:
                true,Electronics,999.99,P001,Laptop 15 inch
                true,Electronics,249.99,P002,Wireless Mouse
                false,Accessories,19.99,P003,USB-C Cable
                true,Electronics,1499.99,P004,4K Monitor
                true,Accessories,89.99,P005,Mechanical Keyboard
                """;

        ToonToCsvConverter converter = new ToonToCsvConverter();
        String csv = converter.convert(toon);

        // Verify header
        assertThat(csv).contains("available,category,price,product_id,product_name");

        // Verify products
        assertThat(csv).contains("Laptop 15 inch");
        assertThat(csv).contains("999.99");
        assertThat(csv).contains("Wireless Mouse");
        assertThat(csv).contains("USB-C Cable");
        assertThat(csv).contains("4K Monitor");
        assertThat(csv).contains("Mechanical Keyboard");

        // Verify boolean values
        assertThat(csv).contains("true");
        assertThat(csv).contains("false");
    }

    @Test
    void testRealisticEmployeeData() throws IOException {
        String toon = """
                data[4]{department,email,hire_date,name,salary}:
                Engineering,alice.smith@company.com,2020-03-15,Alice Smith,95000
                Sales,bob.jones@company.com,2019-07-22,Bob Jones,75000
                Marketing,carol.white@company.com,2021-01-10,Carol White,68000
                Engineering,david.brown@company.com,2018-11-05,David Brown,105000
                """;

        ToonToCsvConverter converter = new ToonToCsvConverter();
        String csv = converter.convert(toon);

        // Verify all employees
        assertThat(csv).contains("Alice Smith");
        assertThat(csv).contains("Bob Jones");
        assertThat(csv).contains("Carol White");
        assertThat(csv).contains("David Brown");

        // Verify departments
        assertThat(csv).contains("Engineering");
        assertThat(csv).contains("Sales");
        assertThat(csv).contains("Marketing");

        // Verify dates and salaries
        assertThat(csv).contains("2020-03-15");
        assertThat(csv).contains("95000");
    }

    @Test
    void testRealisticTransactionLog() throws IOException {
        String toon = """
                data[6]{amount,customer_id,payment_method,status,timestamp,transaction_id}:
                149.99,C1001,credit_card,completed,2025-11-25T10:30:00,TXN001
                299.50,C1002,paypal,completed,2025-11-25T10:35:12,TXN002
                89.99,C1003,debit_card,pending,2025-11-25T10:40:05,TXN003
                450.00,C1001,bank_transfer,completed,2025-11-25T10:45:30,TXN004
                25.99,C1004,credit_card,failed,2025-11-25T10:50:15,TXN005
                199.99,C1002,credit_card,completed,2025-11-25T10:55:00,TXN006
                """;

        ToonToCsvConverter converter = new ToonToCsvConverter();
        String csv = converter.convert(toon);

        // Verify transactions
        assertThat(csv).contains("TXN001");
        assertThat(csv).contains("TXN006");

        // Verify payment methods
        assertThat(csv).contains("credit_card");
        assertThat(csv).contains("paypal");
        assertThat(csv).contains("bank_transfer");

        // Verify statuses
        assertThat(csv).contains("completed");
        assertThat(csv).contains("pending");
        assertThat(csv).contains("failed");

        // Verify amounts
        assertThat(csv).contains("149.99");
        assertThat(csv).contains("450.0"); // Numeric formatting removes trailing zeros
    }

    @Test
    void testRealisticInventoryData() throws IOException {

        String toon = """
                data[5]{last_updated,location,quantity,sku,warehouse_id}:
                2025-11-20,A-12-3,150,SKU-1001,WH-EAST
                2025-11-22,B-05-7,0,SKU-1002,WH-WEST
                2025-11-25,A-15-2,75,SKU-1003,WH-EAST
                2025-11-23,C-08-1,200,SKU-1004,WH-CENTRAL
                2025-11-24,B-11-5,5,SKU-1005,WH-WEST
                """;

        ToonToCsvOptions options = ToonToCsvOptions.builder()
            .includeHeader(true)
            .build();
        ToonToCsvConverter converter = new ToonToCsvConverter(options);
        String csv = converter.convert(toon);

        // Verify SKUs
        assertThat(csv).contains("SKU-1001");
        assertThat(csv).contains("SKU-1005");

        // Verify warehouses
        assertThat(csv).contains("WH-EAST");
        assertThat(csv).contains("WH-WEST");
        assertThat(csv).contains("WH-CENTRAL");

        // Verify locations
        assertThat(csv).contains("A-12-3");
        assertThat(csv).contains("C-08-1");

        // Verify quantities including zero stock
        assertThat(csv).contains("150");
        assertThat(csv).contains("0");
    }
}
