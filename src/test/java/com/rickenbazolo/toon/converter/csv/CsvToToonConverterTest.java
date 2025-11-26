package com.rickenbazolo.toon.converter.csv;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CsvToToonConverter}.
 *
 * @author Ricken Bazolo
 */
class CsvToToonConverterTest {

    @Test
    void testBasicConversion() throws IOException {
        String csv = "id,name,age\n1,Alice,30\n2,Bob,25";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // TOON encoder wraps arrays and sorts columns alphabetically
        assertThat(toon).contains("data[2]");
        assertThat(toon).contains("{age,id,name}");
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("Bob");
    }

    @Test
    void testWithHeader() throws IOException {
        String csv = "product,price,available\nLaptop,999.99,true\nMouse,24.99,false";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // Columns sorted alphabetically
        assertThat(toon).contains("{available,price,product}");
        assertThat(toon).contains("Laptop");
        assertThat(toon).contains("999.99");
        assertThat(toon).contains("true");
    }

    @Test
    void testWithoutHeader() throws IOException {
        String csv = "1,Alice,30\n2,Bob,25";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .hasHeader(false)
            .customHeaders(List.of("id", "name", "age"))
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns sorted alphabetically
        assertThat(toon).contains("{age,id,name}");
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("Bob");
    }

    @Test
    void testCustomDelimiterSemicolon() throws IOException {
        String csv = "id;name;country\n1;Alice;France\n2;Bob;Germany";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .delimiter(';')
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns sorted: country, id, name
        assertThat(toon).contains("{country,id,name}");
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("France");
    }

    @Test
    void testCustomDelimiterTab() throws IOException {
        String csv = "id\tname\tage\n1\tAlice\t30\n2\tBob\t25";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .delimiter('\t')
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns sorted alphabetically
        assertThat(toon).contains("{age,id,name}");
        assertThat(toon).contains("Alice");
    }

    @Test
    void testTypeInference() throws IOException {
        String csv = "id,name,age,active\n1,Alice,30,true\n2,Bob,25,false";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // Numbers and booleans should be inferred
        // Columns sorted: active, age, id, name
        assertThat(toon).contains("{active,age,id,name}");
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("Bob");
    }

    @Test
    void testTypeInferenceDisabled() throws IOException {
        String csv = "id,active\n1,true\n2,false";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .typeInference(false)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // With type inference disabled, all values should be quoted strings
        // Columns sorted: active, id
        assertThat(toon).contains("{active,id}");
        assertThat(toon).contains("\"true\"");
        assertThat(toon).contains("\"1\"");
    }

    @Test
    void testEmptyValueHandlingNull() throws IOException {
        String csv = "id,name,email\n1,Alice,alice@example.com\n2,Bob,";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .emptyValueHandling(CsvToToonOptions.EmptyValueHandling.NULL)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns sorted: email, id, name
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("null");
        assertThat(toon).contains("Bob");
    }

    @Test
    void testEmptyValueHandlingEmptyString() throws IOException {
        String csv = "id,name,email\n1,Alice,\n2,Bob,bob@example.com";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .emptyValueHandling(CsvToToonOptions.EmptyValueHandling.EMPTY_STRING)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        assertThat(toon).contains("Alice");
    }

    @Test
    void testQuotedFields() throws IOException {
        String csv = "id,name,description\n1,\"Product A\",\"Contains, comma\"\n2,\"Product B\",Normal";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        assertThat(toon).contains("Product A");
        assertThat(toon).contains("Contains, comma");
        assertThat(toon).contains("Product B");
    }

    @Test
    void testEscapedQuotes() throws IOException {
        String csv = "id,name,quote\n1,Alice,\"She said \"\"Hello\"\"\"\n2,Bob,\"Normal text\"";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        assertThat(toon).contains("Alice");
        assertThat(toon).contains("Bob");
    }

    @Test
    void testWhitespaceTrimming() throws IOException {
        String csv = "id,name,city\n1,  Alice  ,  NYC  \n2,  Bob  ,  LA  ";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .trimWhitespace(true)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns sorted: city, id, name
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("NYC");
        assertThat(toon).contains("Bob");
        assertThat(toon).contains("LA");
    }

    @Test
    void testNullValueHandling() throws IOException {
        String csv = "id,name,email\n1,Alice,N/A\n2,Bob,bob@example.com";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .nullValue("N/A")
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns alphabetically sorted: email, id, name
        assertThat(toon).contains("Alice").contains("null");
        assertThat(toon).contains("Bob").contains("bob@example.com");
    }

    @Test
    void testCustomHeaders() throws IOException {
        String csv = "1,Alice,30\n2,Bob,25";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .hasHeader(false)
            .customHeaders(List.of("user_id", "full_name", "user_age"))
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        // Columns alphabetically sorted: full_name, user_age, user_id
        assertThat(toon).contains("{full_name,user_age,user_id}");
        assertThat(toon).contains("Alice").contains("30").contains("1");
    }

    @Test
    void testEmptyLineSkipping() throws IOException {
        String csv = "id,name\n1,Alice\n\n2,Bob\n\n";
        CsvToToonOptions options = CsvToToonOptions.builder()
            .skipEmptyLines(true)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csv);

        assertThat(toon).contains("data[2]");
        assertThat(toon).contains("1,Alice");
        assertThat(toon).contains("2,Bob");
    }

    @Test
    void testNumberInference() throws IOException {
        String csv = "int,float,scientific\n42,3.14,1.5e10";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // Columns sorted: float, int, scientific
        assertThat(toon).contains("42");
        assertThat(toon).contains("3.14");
    }

    @Test
    void testBooleanInference() throws IOException {
        String csv = "flag1,flag2,flag3,flag4\ntrue,false,TRUE,FALSE";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        assertThat(toon).contains("true,false,true,false");
    }

    @Test
    void testFileInput() throws IOException {
        // Create a temporary CSV file
        Path tempFile = Files.createTempFile("test", ".csv");
        String csv = "id,name,age\n1,Alice,30\n2,Bob,25";
        Files.writeString(tempFile, csv);

        try {
            CsvToToonConverter converter = new CsvToToonConverter();
            String toon = converter.convert(tempFile.toFile());

            // Columns alphabetically sorted: age, id, name
            assertThat(toon).contains("{age,id,name}");
            assertThat(toon).contains("Alice").contains("30").contains("1");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testStreamInput() throws IOException {
        String csv = "id,name\n1,Alice\n2,Bob";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(new ByteArrayInputStream(csv.getBytes()));

        assertThat(toon).contains("id,name");
        assertThat(toon).contains("1,Alice");
    }

    @Test
    void testNullCsvStringThrowsException() {
        CsvToToonConverter converter = new CsvToToonConverter();

        assertThatThrownBy(() -> converter.convert((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSV string cannot be null");
    }

    @Test
    void testNullFileThrowsException() {
        CsvToToonConverter converter = new CsvToToonConverter();

        assertThatThrownBy(() -> converter.convert((File) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSV file cannot be null");
    }

    @Test
    void testNullOptionsThrowsException() {
        assertThatThrownBy(() -> new CsvToToonConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CsvToToonOptions cannot be null");
    }

    @Test
    void testSingleColumn() throws IOException {
        String csv = "name\nAlice\nBob\nCarol";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        assertThat(toon).contains("name");
        assertThat(toon).contains("Alice");
        assertThat(toon).contains("Bob");
        assertThat(toon).contains("Carol");
    }

    @Test
    void testEmptyCsv() throws IOException {
        String csv = "";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // Empty CSV produces wrapped empty array
        assertThat(toon).contains("data[0]");
    }

    @Test
    void testOnlyHeaders() throws IOException {
        String csv = "id,name,age";
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csv);

        // Empty array with column spec
        assertThat(toon).contains("data[0]");
    }

    @Test
    void testLargeCsv() throws IOException {
        StringBuilder csv = new StringBuilder("id,name,value\n");
        for (int i = 0; i < 1000; i++) {
            csv.append(i).append(",Name").append(i).append(",").append(i * 100).append("\n");
        }

        CsvToToonConverter converter = new CsvToToonConverter();
        String toon = converter.convert(csv.toString());

        assertThat(toon).contains("data[1000]");
        assertThat(toon).contains("id,name,value");
    }

    // =========================================================================
    // File Input Tests with Demo Resources
    // =========================================================================

    @Test
    void testFileInputFromResourcesProducts() throws IOException {
        File csvFile = new File("src/test/resources/csv/products.csv");
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csvFile);

        // Verify array size
        assertThat(toon).contains("data[7]");

        // Verify columns (alphabetically sorted: category, in_stock, price, product_id, product_name)
        assertThat(toon).contains("{category,in_stock,price,product_id,product_name}");

        // Verify some products
        assertThat(toon).contains("Laptop 15 inch");
        assertThat(toon).contains("Mechanical Keyboard");
        assertThat(toon).contains("Webcam HD");

        // Verify categories
        assertThat(toon).contains("Electronics");
        assertThat(toon).contains("Accessories");
    }

    @Test
    void testFileInputFromResourcesEmployees() throws IOException {
        File csvFile = new File("src/test/resources/csv/employees.csv");
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csvFile);

        // Verify array size
        assertThat(toon).contains("data[6]");

        // Verify employees
        assertThat(toon).contains("Alice Smith");
        assertThat(toon).contains("Bob Jones");
        assertThat(toon).contains("Eve Davis");

        // Verify departments
        assertThat(toon).contains("Engineering");
        assertThat(toon).contains("Sales");
        assertThat(toon).contains("HR");

        // Verify email addresses
        assertThat(toon).contains("alice.smith@company.com");
    }

    @Test
    void testFileInputFromResourcesTransactions() throws IOException {
        File csvFile = new File("src/test/resources/csv/transactions.csv");
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csvFile);

        // Verify array size
        assertThat(toon).contains("data[8]");

        // Verify transaction IDs
        assertThat(toon).contains("TXN001");
        assertThat(toon).contains("TXN008");

        // Verify payment methods
        assertThat(toon).contains("credit_card");
        assertThat(toon).contains("paypal");
        assertThat(toon).contains("bank_transfer");

        // Verify statuses
        assertThat(toon).contains("completed");
        assertThat(toon).contains("pending");
        assertThat(toon).contains("failed");
    }

    @Test
    void testFileInputWithSpecialCharacters() throws IOException {
        File csvFile = new File("src/test/resources/csv/special_chars.csv");
        CsvToToonConverter converter = new CsvToToonConverter();

        String toon = converter.convert(csvFile);

        // Verify array size
        assertThat(toon).contains("data[4]");

        // Verify special character handling
        assertThat(toon).contains("Product A");
        assertThat(toon).contains("Product C");

        // Verify descriptions are present
        assertThat(toon).contains("Simple description");
    }

    @Test
    void testFileInputWithSemicolonDelimiter() throws IOException {
        File csvFile = new File("src/test/resources/csv/european_format.csv");
        CsvToToonOptions options = CsvToToonOptions.builder()
            .delimiter(';')
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csvFile);

        // Verify array size
        assertThat(toon).contains("data[5]");

        // Verify columns (alphabetically sorted: city, country, id, name)
        assertThat(toon).contains("{city,country,id,name}");

        // Verify cities
        assertThat(toon).contains("Paris");
        assertThat(toon).contains("Berlin");
        assertThat(toon).contains("Amsterdam");

        // Verify countries
        assertThat(toon).contains("France");
        assertThat(toon).contains("Germany");
        assertThat(toon).contains("Netherlands");
    }

    // =========================================================================
    // InputStream Input Tests with Demo Resources
    // =========================================================================

    @Test
    void testInputStreamFromResourcesProducts() throws IOException {
        try (InputStream inputStream = new FileInputStream("src/test/resources/csv/products.csv")) {
            CsvToToonConverter converter = new CsvToToonConverter();
            String toon = converter.convert(inputStream);

            // Verify array size
            assertThat(toon).contains("data[7]");

            // Verify products
            assertThat(toon).contains("Laptop 15 inch");
            assertThat(toon).contains("USB-C Cable");
            assertThat(toon).contains("4K Monitor 27 inch");
        }
    }

    @Test
    void testInputStreamFromResourcesEmployees() throws IOException {
        try (InputStream inputStream = new FileInputStream("src/test/resources/csv/employees.csv")) {
            CsvToToonConverter converter = new CsvToToonConverter();
            String toon = converter.convert(inputStream);

            // Verify array size
            assertThat(toon).contains("data[6]");

            // Verify employees
            assertThat(toon).contains("Carol White");
            assertThat(toon).contains("David Brown");
            assertThat(toon).contains("Frank Miller");

            // Verify status
            assertThat(toon).contains("active");
            assertThat(toon).contains("on_leave");
        }
    }

    @Test
    void testInputStreamFromResourcesTransactions() throws IOException {
        try (InputStream inputStream = new FileInputStream("src/test/resources/csv/transactions.csv")) {
            CsvToToonConverter converter = new CsvToToonConverter();
            String toon = converter.convert(inputStream);

            // Verify array size
            assertThat(toon).contains("data[8]");

            // Verify customer IDs
            assertThat(toon).contains("C1001");
            assertThat(toon).contains("C1005");

            // Verify amounts
            assertThat(toon).contains("149.99");
            assertThat(toon).contains("750");
        }
    }

    @Test
    void testInputStreamWithSemicolonDelimiter() throws IOException {
        try (InputStream inputStream = new FileInputStream("src/test/resources/csv/european_format.csv")) {
            CsvToToonOptions options = CsvToToonOptions.builder()
                .delimiter(';')
                .build();
            CsvToToonConverter converter = new CsvToToonConverter(options);
            String toon = converter.convert(inputStream);

            // Verify array size
            assertThat(toon).contains("data[5]");

            // Verify names
            assertThat(toon).contains("Alice");
            assertThat(toon).contains("Eve");

            // Verify cities
            assertThat(toon).contains("Rome");
            assertThat(toon).contains("Madrid");
        }
    }

    @Test
    void testInputStreamWithTypeInference() throws IOException {
        try (InputStream inputStream = new FileInputStream("src/test/resources/csv/products.csv")) {
            CsvToToonOptions options = CsvToToonOptions.builder()
                .typeInference(true)
                .build();
            CsvToToonConverter converter = new CsvToToonConverter(options);
            String toon = converter.convert(inputStream);

            // Verify boolean values are properly inferred
            assertThat(toon).contains("true");
            assertThat(toon).contains("false");

            // Verify numeric values
            assertThat(toon).contains("999.99");
            assertThat(toon).contains("24.99");
        }
    }

    @Test
    void testByteArrayInputStream() throws IOException {
        String csvContent = "id,name,status\n1,Alice,active\n2,Bob,inactive\n3,Carol,active";
        byte[] csvBytes = csvContent.getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(csvBytes)) {
            CsvToToonConverter converter = new CsvToToonConverter();
            String toon = converter.convert(inputStream);

            // Verify array size
            assertThat(toon).contains("data[3]");

            // Verify names
            assertThat(toon).contains("Alice");
            assertThat(toon).contains("Bob");
            assertThat(toon).contains("Carol");

            // Verify statuses
            assertThat(toon).contains("active");
            assertThat(toon).contains("inactive");
        }
    }

    // =========================================================================
    // Integration Tests: File and InputStream with Options
    // =========================================================================

    @Test
    void testFileWithCustomHeadersAndTypeInference() throws IOException {
        File csvFile = new File("src/test/resources/csv/products.csv");
        CsvToToonOptions options = CsvToToonOptions.builder()
            .typeInference(true)
            .trimWhitespace(true)
            .build();
        CsvToToonConverter converter = new CsvToToonConverter(options);

        String toon = converter.convert(csvFile);

        // Verify product count
        assertThat(toon).contains("data[7]");

        // Verify product names and prices
        assertThat(toon).contains("Wireless Mouse");
        assertThat(toon).contains("89.99");
    }

    @Test
    void testInputStreamWithEmptyValueHandling() throws IOException {
        String csvContent = "id,name,email\n1,Alice,alice@test.com\n2,Bob,\n3,Carol,carol@test.com";
        byte[] csvBytes = csvContent.getBytes();

        CsvToToonOptions options = CsvToToonOptions.builder()
            .emptyValueHandling(CsvToToonOptions.EmptyValueHandling.NULL)
            .build();

        try (InputStream inputStream = new ByteArrayInputStream(csvBytes)) {
            CsvToToonConverter converter = new CsvToToonConverter(options);
            String toon = converter.convert(inputStream);

            // Verify null handling
            assertThat(toon).contains("null");
            assertThat(toon).contains("Bob");
            assertThat(toon).contains("carol@test.com");
        }
    }
}
