import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.xml.XmlMapper;

public class Example {

	private static final String RESOURCE_XML_TEMPLATE_DIR = "/xml/template/";
	private static final String RESOURCE_XML_SCHEMA_DIR = "/xml/schema/";

	private static final String TEMPLATE_BOOK = "book-1-0.yaml";
	private static final String XML_SCHEMA_BOOK = "book-1-0.xsd";

	private static final String XML_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";
	private static final String XML_ATTRIBUTE_XMLNS = "xmlns=\"%s\"";

	private static final Pattern RE_TARGET_NAMESPACE = Pattern.compile("targetNamespace=\"(.+?)\"");
	private static final String RE_DOT = "\\.";

	private static final String KEY_DOES_NOT_EXIST = "Key does not exist: %s";
	private static final String EMPTY_STRING = "";
	private static final String CR = System.lineSeparator();

	public static void main(String[] args) {

		Map<String, Object> templateData = new HashMap<>(Map.of( 
		     "Book.Author", "An author",
		     "Book.Title", "A title",
		     "Book.ISBN", "2-7654-1005-4"
		 ));
		
		templateData.put("Book.added", null);	// Delete tag: added
		
		Element rootElement = null;

		try {
			rootElement = new Example().buildXmlElement(TEMPLATE_BOOK, templateData, XML_SCHEMA_BOOK);
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		System.out.println("rootElement: " + rootElement);
	}
	
	@SuppressWarnings("unchecked")
	public Element buildXmlElement(String templateFileName, Map<String, Object> templateData, String xmlSchemaFileName)
			throws Exception {
				
		// Read Yaml source
		
		InputStream is = this.getClass().getResourceAsStream(RESOURCE_XML_TEMPLATE_DIR + templateFileName);
	    String yamlSrc = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
				.lines().collect(Collectors.joining(CR));

		// Read XSD source
		
		is = this.getClass().getResourceAsStream(RESOURCE_XML_SCHEMA_DIR + xmlSchemaFileName);
	    String xsdSrc = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
				.lines().collect(Collectors.joining(CR));

		Source xsdSource = new StreamSource(new StringReader(xsdSrc));

		// Get default namespace

	    Matcher matcher = RE_TARGET_NAMESPACE.matcher(xsdSrc);
	    matcher.find();
	    String xmlDefaultNamespace = matcher.group(1);

	    // Load Yaml source

		Yaml yaml = new Yaml();
		Map<String, Object> yamlAsMap = yaml.load(yamlSrc);
		
		// Set Yaml map values from templateData
		  
		for (Map.Entry<String, Object> entry : templateData.entrySet()) {
			String path = entry.getKey();
			Object value = entry.getValue();
			
			setValueByPath(yamlAsMap, path, value);
		}

		// Root tag = 1st element of map
		
		String rootTag = new ArrayList<String>(yamlAsMap.keySet()).get(0);
		yamlAsMap = (Map<String, Object>) new ArrayList<Object>(yamlAsMap.values()).get(0);

		// Convert map to XML

        XmlMapper xmlMapper = new XmlMapper();
        String xmlSrc = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(yamlAsMap);
        
        // Rename Root tag & add XML namespace

		xmlSrc = xmlSrc.replace(yamlAsMap.getClass().getSimpleName(), rootTag)
				.replace(String.format(XML_ATTRIBUTE_XMLNS, EMPTY_STRING),
						String.format(XML_ATTRIBUTE_XMLNS, xmlDefaultNamespace));

		InputSource xmlSource = new InputSource(new StringReader(xmlSrc));

		// Display before XML parsing

		System.out.println(yamlSrc);
		System.out.println(yamlAsMap);
		System.out.println(xmlSrc);
		System.out.println(xsdSrc);
	    System.out.println(xmlDefaultNamespace);

		// Parse XML source with XSD

		SchemaFactory sfactory = SchemaFactory.newInstance(XML_SCHEMA_LANGUAGE);
		Schema schema = sfactory.newSchema(xsdSource);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		//factory.setValidating(true);		Only for DTD
		//factory.setXIncludeAware(true);
		factory.setSchema(schema);
		
		DocumentBuilder parser = factory.newDocumentBuilder();
		parser.setErrorHandler(new ErrorHandler() {
			// Whatever the error level, then throw an Exception
			public void warning(SAXParseException exception) throws SAXException {
				throw exception;
			}
			public void fatalError(SAXParseException exception) throws SAXException {
				throw exception;
			}
			public void error(SAXParseException exception) throws SAXException {
				throw exception;
			}
		});
		
		Document xmlDoc = parser.parse(xmlSource);
		
		// Get root element

		Element rootElement = xmlDoc.getDocumentElement();
		
		// Display after XML parsing

		System.out.println(rootElement.getNodeName());
		System.out.println(xmlDoc.getChildNodes().item(0).getChildNodes().item(0).getNodeName());
		
		return rootElement;
	}

	@SuppressWarnings("unchecked")
	public void setValueByPath(Map<String, Object> map, String path, Object value) throws Exception {
		
		Map<String, Object> currentMap = map;
		String[] keys = path.split(RE_DOT);
		int n = keys.length;

		for (int i = 1; i <= n; i++) {
			String key = keys[i - 1];
			
			if (! currentMap.containsKey(key)) {
				throw new Exception(String.format(KEY_DOES_NOT_EXIST, key));
			}
			else if (i < n && currentMap.get(key) instanceof Map) {
				currentMap = (Map<String, Object>) currentMap.get(key);
			}
			else if (value == null && currentMap.get(key) instanceof Map) {
				currentMap.remove(key);
			}
			else {
				currentMap.put(key, value);
			}
		}
	}
}
