package com.rickenbazolo.toon.converter.xml;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.rickenbazolo.toon.core.ToonDecoder;
import com.rickenbazolo.toon.exception.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Converts TOON format to XML documents.
 *
 * <p>This converter uses the ToonDecoder to parse TOON strings into JsonNode,
 * then builds an XML DOM document from the JsonNode structure.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Configurable attribute prefix detection (default "@")</li>
 *   <li>Text content handling with configurable key (default "#text")</li>
 *   <li>Pretty-printing support</li>
 *   <li>Optional XML declaration</li>
 *   <li>Customizable root element name</li>
 * </ul>
 *
 * @since 0.2.0
 */
public class ToonToXmlConverter {

    private final ToonToXmlOptions options;

    /**
     * Creates a new ToonToXmlConverter with the specified options.
     *
     * @param options the options to use for TOON parsing and XML generation
     * @throws IllegalArgumentException if options is null
     */
    public ToonToXmlConverter(ToonToXmlOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("ToonToXmlOptions cannot be null");
        }
        this.options = options;
    }

    /**
     * Converts a TOON string to XML format.
     *
     * @param toonString the TOON string to convert
     * @return the XML representation of the TOON data
     * @throws XmlException if conversion fails
     */
    public String convert(String toonString) throws XmlException {
        try {
            // Decode TOON to JsonNode
            ToonDecoder decoder = new ToonDecoder(options.toonOptions());
            JsonNode jsonNode = decoder.decode(toonString);

            // Create XML Document
            Document doc = createDocument();

            // Build XML from JsonNode
            if (jsonNode.isObject() && jsonNode.size() == 1) {
                // Single root element - use the field name as root
                Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.properties().iterator();
                Map.Entry<String, JsonNode> entry = fields.next();
                Element rootElement = buildElement(doc, entry.getKey(), entry.getValue());
                doc.appendChild(rootElement);
            } else {
                // Multiple fields or other structure - use configured root name
                Element rootElement = buildElement(doc, options.rootElementName(), jsonNode);
                doc.appendChild(rootElement);
            }

            // Convert Document to String
            return documentToString(doc);

        } catch (Exception e) {
            throw new XmlException("Failed to convert TOON to XML: " + e.getMessage(), e);
        }
    }

    /**
     * Builds an XML Element from a JsonNode.
     *
     * @param doc the Document to create elements in
     * @param elementName the name of the element to create
     * @param node the JsonNode to convert
     * @return the created Element
     */
    private Element buildElement(Document doc, String elementName, JsonNode node) {
        Element element = doc.createElement(elementName);

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();

            String textContent = null;

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();

                // Check if it's an attribute (starts with prefix)
                if (options.attributePrefix() != null &&
                    !options.attributePrefix().isEmpty() &&
                    fieldName.startsWith(options.attributePrefix())) {
                    // It's an attribute
                    String attrName = fieldName.substring(options.attributePrefix().length());
                    element.setAttribute(attrName, getTextValue(fieldValue));
                }
                // Check if it's text content
                else if (fieldName.equals(options.textNodeKey())) {
                    // Store text content to add later (after attributes and before children)
                    textContent = getTextValue(fieldValue);
                }
                // It's a child element
                else {
                    if (fieldValue.isArray()) {
                        // Array of elements
                        ArrayNode arrayNode = (ArrayNode) fieldValue;
                        for (JsonNode item : arrayNode) {
                            Element childElement = buildElement(doc, fieldName, item);
                            element.appendChild(childElement);
                        }
                    } else {
                        // Single child element
                        Element childElement = buildElement(doc, fieldName, fieldValue);
                        element.appendChild(childElement);
                    }
                }
            }

            // Add text content if present
            if (textContent != null) {
                element.setTextContent(textContent);
            }

        } else if (node.isArray()) {
            // Arrays should be handled by the parent, but if we get here,
            // wrap items in the element
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode item : arrayNode) {
                Element childElement = buildElement(doc, "item", item);
                element.appendChild(childElement);
            }
        } else {
            // Primitive value - set as text content
            element.setTextContent(getTextValue(node));
        }

        return element;
    }

    /**
     * Extracts text value from a JsonNode.
     *
     * @param node the JsonNode
     * @return the text representation of the node
     */
    private String getTextValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            return node.asText();
        } else if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        } else if (node.isNull()) {
            return "";
        } else {
            return node.toString();
        }
    }

    /**
     * Creates a new XML Document.
     *
     * @return a new Document
     * @throws ParserConfigurationException if configuration fails
     */
    private Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Disable external entity processing to prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    /**
     * Converts a Document to a String.
     *
     * @param doc the Document to convert
     * @return the XML string representation
     * @throws TransformerException if transformation fails
     */
    private String documentToString(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        // Disable external entity processing to prevent XXE attacks
        transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");

        Transformer transformer = transformerFactory.newTransformer();

        // Configure output properties
        if (options.xmlDeclaration()) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        } else {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }

        if (options.prettyPrint()) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                        String.valueOf(options.indent()));
        } else {
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
        }

        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }
}
