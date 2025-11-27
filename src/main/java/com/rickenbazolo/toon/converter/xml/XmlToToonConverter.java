package com.rickenbazolo.toon.converter.xml;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.rickenbazolo.toon.core.ToonEncoder;
import com.rickenbazolo.toon.exception.XmlParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts XML documents to TOON format.
 *
 * <p>This converter uses DOM-based parsing to convert XML documents into an intermediate
 * JsonNode representation, which is then encoded to TOON format using the ToonEncoder.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Configurable attribute handling (with prefix or as regular fields)</li>
 *   <li>Automatic array detection for repeated elements</li>
 *   <li>Text content handling with configurable key</li>
 *   <li>Support for nested XML structures</li>
 * </ul>
 *
 * @since 0.2.0
 */
public class XmlToToonConverter {

    private final XmlToToonOptions xmlOptions;
    private final JsonMapper jsonMapper;

    /**
     * Creates a new XmlToToonConverter with the specified options.
     *
     * @param xmlOptions the options to use for XML parsing and TOON encoding
     * @throws IllegalArgumentException if xmlOptions is null
     */
    public XmlToToonConverter(XmlToToonOptions xmlOptions) {
        if (xmlOptions == null) {
            throw new IllegalArgumentException("XmlToToonOptions cannot be null");
        }
        this.xmlOptions = xmlOptions;
        this.jsonMapper = new JsonMapper();
    }

    /**
     * Converts an XML string to TOON format.
     *
     * @param xmlString the XML string to convert
     * @return the TOON representation of the XML
     * @throws XmlParseException if XML parsing fails
     */
    public String convert(String xmlString) throws XmlParseException {
        try {
            // Parse XML
            DocumentBuilder builder = createDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

            // Convert to JsonNode
            Element rootElement = doc.getDocumentElement();
            JsonNode jsonNode = buildJsonNode(rootElement);

            // Wrap in root element if needed
            ObjectNode rootNode = jsonMapper.createObjectNode();
            rootNode.set(getElementName(rootElement), jsonNode);

            // Encode to TOON
            ToonEncoder encoder = new ToonEncoder(xmlOptions.toonOptions());
            return encoder.encode(rootNode);

        } catch (XmlParseException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlParseException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a JsonNode from an XML Element.
     *
     * @param element the XML element to convert
     * @return the JsonNode representation of the element
     */
    private JsonNode buildJsonNode(Element element) {
        ObjectNode node = jsonMapper.createObjectNode();

        // Handle attributes
        if (xmlOptions.includeAttributes() && element.hasAttributes()) {
            handleAttributes(element, node);
        }

        // Handle child elements
        NodeList children = element.getChildNodes();
        Map<String, List<Element>> elementGroups = groupElements(children);

        for (Map.Entry<String, List<Element>> entry : elementGroups.entrySet()) {
            String name = entry.getKey();
            List<Element> elements = entry.getValue();

            if (shouldConvertToArray(elements)) {
                // Array detected
                ArrayNode array = jsonMapper.createArrayNode();
                for (Element child : elements) {
                    array.add(buildJsonNode(child));
                }
                node.set(name, array);
            } else if (elements.size() == 1) {
                // Single element
                node.set(name, buildJsonNode(elements.get(0)));
            }
        }

        // Handle text content
        String textContent = getTextContent(element);
        if (!textContent.isEmpty()) {
            handleTextContent(node, textContent);
        }

        // If node has only text content and no attributes/children, return the text as a simple value
        if (node.size() == 1 && node.has(xmlOptions.textNodeKey())) {
            return node.get(xmlOptions.textNodeKey());
        }

        return node;
    }

    /**
     * Determines if a list of elements should be converted to an array.
     *
     * @param elements the list of elements to check
     * @return true if the elements should be converted to an array
     */
    private boolean shouldConvertToArray(List<Element> elements) {
        return (xmlOptions.arrayDetection() == XmlToToonOptions.ArrayDetection.AUTO && elements.size() > 1)
                || (xmlOptions.arrayDetection() == XmlToToonOptions.ArrayDetection.ALWAYS && !elements.isEmpty());
    }

    /**
     * Handles XML attributes and adds them to the JsonNode.
     *
     * @param element the XML element with attributes
     * @param node the JsonNode to add attributes to
     */
    private void handleAttributes(Element element, ObjectNode node) {
        NamedNodeMap attributes = element.getAttributes();
        String attributePrefix = xmlOptions.attributePrefix();

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            // Add prefix if configured (and not empty)
            String fieldName = (attributePrefix != null && !attributePrefix.isEmpty())
                ? attributePrefix + attrName
                : attrName;

            node.put(fieldName, attrValue);
        }
    }

    /**
     * Groups child elements by their tag name.
     *
     * @param children the NodeList of child nodes
     * @return a map of element names to lists of elements
     */
    private Map<String, List<Element>> groupElements(NodeList children) {
        Map<String, List<Element>> groups = new LinkedHashMap<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                String name = getElementName(element);
                groups.computeIfAbsent(name, k -> new ArrayList<>()).add(element);
            }
        }

        return groups;
    }

    /**
     * Gets the name of an XML element.
     *
     * @param element the XML element
     * @return the element name (tag name)
     */
    private String getElementName(Element element) {
        return element.getTagName();
    }

    /**
     * Extracts text content from an XML element.
     *
     * @param element the XML element
     * @return the text content, or empty string if none
     */
    private String getTextContent(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE ||
                child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String content = child.getNodeValue();
                if (content != null) {
                    text.append(content.trim());
                }
            }
        }

        return text.toString();
    }

    /**
     * Handles text content and adds it to the JsonNode.
     *
     * @param node the JsonNode to add text content to
     * @param textContent the text content to add
     */
    private void handleTextContent(ObjectNode node, String textContent) {
        String textNodeKey = xmlOptions.textNodeKey();

        // If node is empty, just use the text as the value
        if (node.isEmpty()) {
            node.put(textNodeKey, textContent);
        } else {
            // Node has other fields (attributes or child elements), add text with key
            node.put(textNodeKey, textContent);
        }
    }

    /**
     * Creates a DocumentBuilder for parsing XML.
     *
     * @return a configured DocumentBuilder
     * @throws ParserConfigurationException if configuration fails
     */
    private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);

        // Disable external entity processing to prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder();
    }
}
