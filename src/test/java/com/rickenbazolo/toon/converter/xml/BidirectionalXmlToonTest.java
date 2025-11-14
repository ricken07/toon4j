package com.rickenbazolo.toon.converter.xml;

import com.rickenbazolo.toon.Toon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bidirectional test demonstrating XML ↔ TOON conversion.
 */
class BidirectionalXmlToonTest {

    @Test
    void testBidirectionalConversionSimple() throws Exception {
        // Original XML
        String originalXml = """
                <user>
                    <id>123</id>
                    <name>Alice Smith</name>
                    <email>alice@example.com</email>
                    <active>true</active>
                </user>
                """;

        System.out.println("=== Simple Bidirectional Conversion ===");
        System.out.println("\n1. Original XML:");
        System.out.println(originalXml);

        // XML → TOON
        String toon = Toon.fromXml(originalXml);
        System.out.println("\n2. Converted to TOON:");
        System.out.println(toon);

        // TOON → XML
        String resultXml = Toon.toXml(toon);
        System.out.println("\n3. Converted back to XML:");
        System.out.println(resultXml);

        // Verify content
        assertTrue(resultXml.contains("<user>"));
        assertTrue(resultXml.contains("<id>123</id>"));
        assertTrue(resultXml.contains("<name>Alice Smith</name>"));
        assertTrue(resultXml.contains("<email>alice@example.com</email>"));
        assertTrue(resultXml.contains("<active>true</active>"));
    }

    @Test
    void testBidirectionalConversionWithAttributes() throws Exception {
        // Original XML with attributes
        String originalXml = """
                <catalog version="2.0" lang="en">
                    <book id="B001" category="fiction">
                        <title>The Great Gatsby</title>
                        <author>F. Scott Fitzgerald</author>
                        <year>1925</year>
                    </book>
                    <book id="B002" category="dystopian">
                        <title>1984</title>
                        <author>George Orwell</author>
                        <year>1949</year>
                    </book>
                </catalog>
                """;

        System.out.println("\n=== Bidirectional Conversion with Attributes ===");
        System.out.println("\n1. Original XML:");
        System.out.println(originalXml);

        // XML → TOON
        String toon = Toon.fromXml(originalXml);
        System.out.println("\n2. Converted to TOON:");
        System.out.println(toon);

        // Calculate token savings
        int xmlLength = originalXml.length();
        int toonLength = toon.length();
        double savings = ((xmlLength - toonLength) * 100.0) / xmlLength;
        System.out.printf("\n   Token Savings: %d chars → %d chars (%.1f%% reduction)\n",
                         xmlLength, toonLength, savings);

        // TOON → XML
        String resultXml = Toon.toXml(toon);
        System.out.println("\n3. Converted back to XML:");
        System.out.println(resultXml);

        // Verify content
        assertTrue(resultXml.contains("version=\"2.0\""));
        assertTrue(resultXml.contains("id=\"B001\""));
        assertTrue(resultXml.contains("<title>The Great Gatsby</title>"));
    }

    @Test
    void testBidirectionalConversionRssFeed() throws Exception {
        // RSS Feed example
        String rssXml = """
                <rss version="2.0">
                    <channel>
                        <title>Tech News Daily</title>
                        <link>https://technews.example.com</link>
                        <description>Latest technology news</description>
                        <item>
                            <title>AI Breakthrough in Natural Language Processing</title>
                            <link>https://technews.example.com/ai-breakthrough</link>
                            <pubDate>2025-11-11</pubDate>
                            <description>Researchers achieve new milestone in AI</description>
                        </item>
                        <item>
                            <title>New Framework Simplifies Web Development</title>
                            <link>https://technews.example.com/new-framework</link>
                            <pubDate>2025-11-10</pubDate>
                            <description>Developers celebrate new productivity tools</description>
                        </item>
                        <item>
                            <title>Cloud Computing Trends for 2025</title>
                            <link>https://technews.example.com/cloud-trends</link>
                            <pubDate>2025-11-09</pubDate>
                            <description>Industry experts share predictions</description>
                        </item>
                    </channel>
                </rss>
                """;

        System.out.println("\n=== RSS Feed Bidirectional Conversion ===");
        System.out.println("\n1. Original RSS XML:");
        System.out.println(rssXml);

        // XML → TOON
        String toon = Toon.fromXml(rssXml);
        System.out.println("\n2. Converted to TOON:");
        System.out.println(toon);

        // Calculate significant token savings
        int xmlLength = rssXml.length();
        int toonLength = toon.length();
        double savings = ((xmlLength - toonLength) * 100.0) / xmlLength;
        System.out.printf("\n   ✨ Token Savings: %d chars → %d chars (%.1f%% reduction)\n",
                         xmlLength, toonLength, savings);
        System.out.println("   💡 Perfect for LLM context optimization!");

        // TOON → XML
        String resultXml = Toon.toXml(toon);
        System.out.println("\n3. Converted back to XML:");
        System.out.println(resultXml);

        // Verify RSS structure preserved
        assertTrue(resultXml.contains("<rss"));
        assertTrue(resultXml.contains("version=\"2.0\""));
        assertTrue(resultXml.contains("<channel>"));
        assertTrue(resultXml.contains("<item>"));
        assertTrue(resultXml.contains("<title>AI Breakthrough"));
    }

    @Test
    void testCustomOptionsRoundTrip() throws Exception {
        String originalXml = """
                <product id="P001" category="electronics">
                    <name>Laptop Pro</name>
                    <specs>High performance laptop</specs>
                    <price>1499.99</price>
                </product>
                """;

        System.out.println("\n=== Custom Options Round Trip ===");
        System.out.println("\n1. Original XML:");
        System.out.println(originalXml);

        // XML → TOON with custom options
        XmlToToonOptions xmlToToonOpts = XmlToToonOptions.builder()
            .attributePrefix("_")
            .textNodeKey("value")
            .build();

        String toon = Toon.fromXml(originalXml, xmlToToonOpts);
        System.out.println("\n2. TOON (with custom attribute prefix '_'):");
        System.out.println(toon);

        // TOON → XML with matching custom options
        ToonToXmlOptions toonToXmlOpts = ToonToXmlOptions.builder()
            .attributePrefix("_")
            .textNodeKey("value")
            .prettyPrint(true)
            .build();

        String resultXml = Toon.toXml(toon, toonToXmlOpts);
        System.out.println("\n3. Result XML:");
        System.out.println(resultXml);

        // Verify attributes converted correctly
        assertTrue(resultXml.contains("id=\"P001\""));
        assertTrue(resultXml.contains("category=\"electronics\""));
        assertTrue(resultXml.contains("<name>Laptop Pro</name>"));
    }

    @Test
    void testNestedStructurePreservation() throws Exception {
        String nestedXml = """
                <company>
                    <name>TechCorp Inc.</name>
                    <headquarters>
                        <address>
                            <street>123 Innovation Drive</street>
                            <city>San Francisco</city>
                            <state>CA</state>
                            <zip>94102</zip>
                        </address>
                    </headquarters>
                    <departments>
                        <department id="ENG">
                            <name>Engineering</name>
                            <head>Alice Johnson</head>
                        </department>
                        <department id="SAL">
                            <name>Sales</name>
                            <head>Bob Smith</head>
                        </department>
                    </departments>
                </company>
                """;

        System.out.println("\n=== Nested Structure Preservation ===");
        System.out.println("\n1. Original XML (nested structure):");
        System.out.println(nestedXml);

        String toon = Toon.fromXml(nestedXml);
        System.out.println("\n2. TOON representation:");
        System.out.println(toon);

        String resultXml = Toon.toXml(toon);
        System.out.println("\n3. Reconstructed XML:");
        System.out.println(resultXml);

        // Verify nested structure
        assertTrue(resultXml.contains("<company>"));
        assertTrue(resultXml.contains("<headquarters>"));
        assertTrue(resultXml.contains("<address>"));
        assertTrue(resultXml.contains("<street>123 Innovation Drive</street>"));
        assertTrue(resultXml.contains("<departments>"));
        assertTrue(resultXml.contains("<department"));
    }
}
