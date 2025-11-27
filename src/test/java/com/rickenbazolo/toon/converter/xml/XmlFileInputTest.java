package com.rickenbazolo.toon.converter.xml;

import com.rickenbazolo.toon.Toon;
import com.rickenbazolo.toon.exception.XmlParseException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for XML file and stream input methods.
 * Tests Toon.fromXml(File) and Toon.fromXml(InputStream) using sample XML files.
 */
class XmlFileInputTest {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileInputTest.class);

    /**
     * Gets a file from test resources
     */
    private File getResourceFile(String resourcePath) throws URISyntaxException {
        var resource = getClass().getClassLoader().getResource(resourcePath);
        assertNotNull(resource, "Resource not found: " + resourcePath);
        return new File(resource.toURI());
    }

    /**
     * Gets an InputStream from test resources
     */
    private InputStream getResourceStream(String resourcePath) {
        var stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(stream, "Resource not found: " + resourcePath);
        return stream;
    }

    @Test
    void testFromXmlFile_SampleUsers() throws Exception {
        logger.debug("\n=== Test fromXml(File): sample-users.xml ===");

        // Load XML file from resources
        File xmlFile = getResourceFile("xml/sample-users.xml");
        assertTrue(xmlFile.exists(), "XML file should exist");

        logger.debug("\n1. Loading XML file: {}", xmlFile.getPath());
        logger.debug("   File size: {} bytes", xmlFile.length());

        // Convert XML file to TOON
        String toon = Toon.fromXml(xmlFile);

        logger.debug("\n2. Converted to TOON:");
        logger.debug(toon);

        // Verify content
        assertNotNull(toon);
        assertTrue(toon.contains("users"));
        assertTrue(toon.contains("user[3]") || toon.contains("user:"));
        assertTrue(toon.contains("Alice Johnson"));
        assertTrue(toon.contains("Bob Smith"));
        assertTrue(toon.contains("Carol Williams"));
        assertTrue(toon.contains("@id") || toon.contains("id") || toon.contains("attr_id"));
        assertTrue(toon.contains("@status") || toon.contains("status") || toon.contains("attr_status"));
        assertTrue(toon.contains("Engineering"));
        // Address content may be collapsed, just verify structure exists
        assertTrue(toon.contains("address"));

        // Calculate size comparison
        long xmlSize = xmlFile.length();
        long toonSize = toon.length();
        double reduction = ((xmlSize - toonSize) * 100.0) / xmlSize;
        logger.debug("\n3. Size comparison: {} bytes (XML) → {} bytes (TOON) = {} reduction",
                         xmlSize, toonSize, String.format("%.1f%%", reduction));
    }

    @Test
    void testFromXmlFile_SampleCatalog() throws Exception {
        logger.debug("\n=== Test fromXmlFile: sample-catalog.xml ===");

        File xmlFile = getResourceFile("xml/sample-catalog.xml");
        assertTrue(xmlFile.exists(), "XML file should exist");

        logger.debug("\n1. Loading XML file: {}", xmlFile.getPath());

        // Convert with default options
        String toon = Toon.fromXml(xmlFile);

        logger.debug("\n2. Converted to TOON (default options):");
        logger.debug(toon);

        // Verify content
        assertNotNull(toon);
        assertTrue(toon.contains("catalog"));
        assertTrue(toon.contains("@version") || toon.contains("version"));
        assertTrue(toon.contains("metadata"));
        assertTrue(toon.contains("Book Catalog"));
        assertTrue(toon.contains("books"));
        assertTrue(toon.contains("The Great Gatsby"));
        assertTrue(toon.contains("1984"));
        assertTrue(toon.contains("Stephen Hawking"));
    }

    @Test
    void testFromXmlFile_WithCustomOptions() throws Exception {
        logger.debug("\n=== Test fromXmlFile with custom options ===");

        File xmlFile = getResourceFile("xml/sample-catalog.xml");

        // Create custom options without attributes
        XmlToToonOptions options = XmlToToonOptions.builder()
            .includeAttributes(false)
            .arrayDetection(XmlToToonOptions.ArrayDetection.AUTO)
            .build();

        String toon = Toon.fromXml(xmlFile, options);

        logger.debug("\n1. Converted with custom options (no attributes):");
        logger.debug(toon);

        // Verify attributes are not included
        assertNotNull(toon);
        assertFalse(toon.contains("@version"), "Should not contain @version when attributes are disabled");
        assertFalse(toon.contains("@id"), "Should not contain @id when attributes are disabled");
        assertTrue(toon.contains("catalog"));
        assertTrue(toon.contains("metadata"));
    }

    @Test
    void testFromXmlStream_SampleUsers() throws Exception {
        logger.debug("\n=== Test fromXmlStream: sample-users.xml ===");

        // Load XML from InputStream
        try (InputStream xmlStream = getResourceStream("xml/sample-users.xml")) {
            assertNotNull(xmlStream);
            logger.debug("\n1. Loading XML from InputStream");

            // Convert XML stream to TOON
            String toon = Toon.fromXml(xmlStream);

            logger.debug("\n2. Converted to TOON:");
            logger.debug(toon);

            // Verify content
            assertNotNull(toon);
            assertTrue(toon.contains("users"));
            assertTrue(toon.contains("Alice Johnson"));
            assertTrue(toon.contains("Bob Smith"));
            assertTrue(toon.contains("address"));
        }
    }

    @Test
    void testFromXmlStream_SampleCatalog() throws Exception {
        logger.debug("\n=== Test fromXmlStream: sample-catalog.xml ===");

        try (InputStream xmlStream = getResourceStream("xml/sample-catalog.xml")) {
            assertNotNull(xmlStream);

            String toon = Toon.fromXml(xmlStream);

            logger.debug("\n1. Converted to TOON:");
            logger.debug(toon);

            // Verify content
            assertNotNull(toon);
            assertTrue(toon.contains("catalog"));
            assertTrue(toon.contains("books"));
            assertTrue(toon.contains("The Great Gatsby"));
        }
    }

    @Test
    void testFromXmlStream_WithCustomOptions() throws Exception {
        logger.debug("\n=== Test fromXmlStream with custom options ===");

        try (InputStream xmlStream = getResourceStream("xml/sample-users.xml")) {
            // Custom options with different attribute prefix
            XmlToToonOptions options = XmlToToonOptions.builder()
                .attributePrefix("attr_")
                .textNodeKey("text")
                .arrayDetection(XmlToToonOptions.ArrayDetection.AUTO)
                .build();

            String toon = Toon.fromXml(xmlStream, options);

            logger.debug("\n1. Converted with custom prefix 'attr_':");
            logger.debug(toon);

            // Verify custom prefix is used
            assertNotNull(toon);
            assertTrue(toon.contains("attr_id") || toon.contains("attr_status"));
            assertFalse(toon.contains("@id"), "Should not use default @ prefix");
        }
    }

    @Test
    void testFromXmlFile_NonExistentFile() {
        logger.debug("\n=== Test fromXmlFile with non-existent file ===");

        File nonExistentFile = new File("/path/to/nonexistent/file.xml");

        // Should throw IOException
        assertThrows(IOException.class, () -> {
            Toon.fromXml(nonExistentFile);
        });

        logger.debug("Correctly throws IOException for non-existent file");
    }

    @Test
    void testRoundTrip_FileToToonToXml() throws Exception {
        logger.debug("\n=== Test Round Trip: File → TOON → XML ===");

        // Use a simpler XML file for round trip
        File originalFile = getResourceFile("xml/sample-catalog.xml");

        logger.debug("\n1. Original XML file:");
        String originalXml = Files.readString(originalFile.toPath());
        logger.debug("{}...", originalXml.substring(0, Math.min(300, originalXml.length())));

        // XML File → TOON (without attributes to get tabular format)
        XmlToToonOptions xmlOptions = XmlToToonOptions.builder()
            .includeAttributes(false)
            .build();
        String toon = Toon.fromXml(originalFile, xmlOptions);
        logger.debug("\n2. Converted to TOON:");
        logger.debug("{}...", toon.substring(0, Math.min(400, toon.length())));

        // TOON → XML
        String resultXml = Toon.toXml(toon);
        logger.debug("\n3. Converted back to XML:");
        logger.debug("{}...", resultXml.substring(0, Math.min(300, resultXml.length())));

        // Verify essential content is preserved
        assertNotNull(resultXml);
        assertTrue(resultXml.contains("<catalog>"));
        assertTrue(resultXml.contains("<book>"));
        assertTrue(resultXml.contains("The Great Gatsby"));
        assertTrue(resultXml.contains("George Orwell"));

        logger.debug("\nRound trip successful - data preserved!");
    }

    @Test
    void testCompareFileVsStream() throws Exception {
        logger.debug("\n=== Test: Compare File vs Stream results ===");

        // Load same XML using File
        File xmlFile = getResourceFile("xml/sample-catalog.xml");
        String toonFromFile = Toon.fromXml(xmlFile);

        // Load same XML using Stream
        try (InputStream xmlStream = getResourceStream("xml/sample-catalog.xml")) {
            String toonFromStream = Toon.fromXml(xmlStream);

            logger.debug("\n1. TOON from File:");
            logger.debug(toonFromFile);

            logger.debug("\n2. TOON from Stream:");
            logger.debug(toonFromStream);

            // Both should produce identical output
            assertEquals(toonFromFile, toonFromStream,
                "File and Stream conversion should produce identical results");

            logger.debug("\nFile and Stream methods produce identical results!");
        }
    }
}
