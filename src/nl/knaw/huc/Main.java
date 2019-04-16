package nl.knaw.huc;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

public class Main {

  public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

    // getProperties();

    System.out.println(readProperties());

    // write your code here
  }

  private static void getProperties() throws ParserConfigurationException, IOException, SAXException {
    File inputFile = new File("config.xml");
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(inputFile);
    doc.getDocumentElement().normalize();
    System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
    NodeList nList = doc.getElementsByTagName("resourcesync");
    for (int i = 0; i < nList.getLength(); i++) {
      Node nNode = nList.item(i);
      System.out.println("\nCurrent Element :" + nNode.getNodeName()+" - "+nNode.getTextContent());
    }
  }

  private static Properties readProperties() throws IOException {
    Properties prop = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream stream = new FileInputStream("config.properties");
    System.err.println(stream);
    InputStreamReader isr = new InputStreamReader(stream);
    prop.load(stream);
    return prop;
  }

}
