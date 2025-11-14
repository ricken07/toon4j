package com.rickenbazolo.toon.converter.xml;

import com.rickenbazolo.toon.Toon;
import com.rickenbazolo.toon.exception.XmlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ToonToXmlConverter.
 */
class ToonToXmlConverterTest {

    @Test
    void testSimpleToonToXml() throws XmlException {
        String toon = """
                user:
                  id: 123
                  name: Alice
                  email: alice@example.com
                """;

        String xml = Toon.toXml(toon);

        assertNotNull(xml);
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<id>123</id>"));
        assertTrue(xml.contains("<name>Alice</name>"));
        assertTrue(xml.contains("<email>alice@example.com</email>"));
        assertTrue(xml.contains("</user>"));
        System.out.println("Simple TOON to XML:");
        System.out.println(xml);
    }

    @Test
    void testToonWithAttributesToXml() throws XmlException {
        String toon = """
                product:
                  "@id": P001
                  "@category": electronics
                  name: Laptop
                  price: 999.99
                """;

        String xml = Toon.toXml(toon);

        assertNotNull(xml);
        assertTrue(xml.contains("<product"));
        assertTrue(xml.contains("id=\"P001\""));
        assertTrue(xml.contains("category=\"electronics\""));
        assertTrue(xml.contains("<name>Laptop</name>"));
        System.out.println("\nTOON with Attributes to XML:");
        System.out.println(xml);
    }

    @Test
    void testToonWithArrayToXml() throws XmlException {
        String toon = """
                catalog:
                  book[2]{id,title,year}:
                  1,The Great Gatsby,1925
                  2,1984,1949
                """;

        String xml = Toon.toXml(toon);

        assertNotNull(xml);
        assertTrue(xml.contains("<catalog>"));
        assertTrue(xml.contains("<book>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("<title>The Great Gatsby</title>"));
        assertTrue(xml.contains("<year>1925</year>"));
        System.out.println("\nTOON with Array to XML:");
        System.out.println(xml);
    }

    @Test
    void testToonWithTextContentToXml() throws XmlException {
        String toon = """
                price:
                  "@currency": USD
                  "#text": 999.99
                """;

        String xml = Toon.toXml(toon);

        assertNotNull(xml);
        assertTrue(xml.contains("<price"));
        assertTrue(xml.contains("currency=\"USD\""));
        assertTrue(xml.contains("999.99"));
        System.out.println("\nTOON with Text Content to XML:");
        System.out.println(xml);
    }

    @Test
    void testToonToXmlWithoutDeclaration() throws XmlException {
        String toon = """
                user:
                  name: Bob
                """;

        ToonToXmlOptions options = ToonToXmlOptions.builder()
            .xmlDeclaration(false)
            .build();

        String xml = Toon.toXml(toon, options);

        assertNotNull(xml);
        assertFalse(xml.contains("<?xml"));
        assertTrue(xml.contains("<user>"));
        System.out.println("\nTOON to XML without Declaration:");
        System.out.println(xml);
    }

    @Test
    void testToonToXmlNotPrettyPrint() throws XmlException {
        String toon = """
                user:
                  id: 1
                  name: Alice
                """;

        ToonToXmlOptions options = ToonToXmlOptions.builder()
            .prettyPrint(false)
            .xmlDeclaration(false)
            .build();

        String xml = Toon.toXml(toon, options);

        assertNotNull(xml);
        assertFalse(xml.contains("\n  "));  // No indentation
        System.out.println("\nTOON to XML Not Pretty Print:");
        System.out.println(xml);
    }

    @Test
    void testToonToXmlWithCustomRootName() throws XmlException {
        String toon = """
                id: 123
                name: Alice
                """;

        ToonToXmlOptions options = ToonToXmlOptions.builder()
            .rootElementName("person")
            .build();

        String xml = Toon.toXml(toon, options);

        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("</person>"));
        System.out.println("\nTOON to XML with Custom Root Name:");
        System.out.println(xml);
    }

    @Test
    void testRoundTripXmlToToonToXml() throws Exception {
        String originalXml = "<user><id>123</id><name>Alice</name></user>";

        // XML -> TOON
        String toon = Toon.fromXml(originalXml);
        System.out.println("\nRound Trip - Original XML:");
        System.out.println(originalXml);
        System.out.println("\nRound Trip - TOON:");
        System.out.println(toon);

        // TOON -> XML
        String resultXml = Toon.toXml(toon);
        System.out.println("\nRound Trip - Result XML:");
        System.out.println(resultXml);

        assertNotNull(resultXml);
        assertTrue(resultXml.contains("<user>"));
        assertTrue(resultXml.contains("<id>123</id>"));
        assertTrue(resultXml.contains("<name>Alice</name>"));
    }

    @Test
    void testComplexNestedStructure() throws XmlException {
        String toon = """
                company:
                  name: TechCorp
                  location:
                    city: San Francisco
                    country: USA
                  employees[2]{id,name,role}:
                  1,Alice,Engineer
                  2,Bob,Manager
                """;

        String xml = Toon.toXml(toon);

        assertNotNull(xml);
        assertTrue(xml.contains("<company>"));
        assertTrue(xml.contains("<location>"));
        assertTrue(xml.contains("<city>San Francisco</city>"));
        assertTrue(xml.contains("<employees>"));
        System.out.println("\nComplex Nested Structure TOON to XML:");
        System.out.println(xml);
    }

    @Test
    void testToonToXmlWithCustomAttributePrefix() throws XmlException {
        String toon = """
                product:
                  _attr_id: P001
                  name: Laptop
                """;

        ToonToXmlOptions options = ToonToXmlOptions.builder()
            .attributePrefix("_attr_")
            .build();

        String xml = Toon.toXml(toon, options);

        assertNotNull(xml);
        assertTrue(xml.contains("id=\"P001\""));
        assertTrue(xml.contains("<name>Laptop</name>"));
        System.out.println("\nTOON to XML with Custom Attribute Prefix:");
        System.out.println(xml);
    }
}
