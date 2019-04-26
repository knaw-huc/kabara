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
import java.net.URL;
import java.util.Date;
import java.util.Properties;

public class Main {

  public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

    Properties props = readProperties();
    System.out.println(props);

  //   nu timbuctoo benaderen:
  //  https://repository.huygens.knaw.nl/v5/resourcesync/sourceDescription.xml
    URL sourceDescr = new URL(props.getProperty("resourcesync"));
    BufferedReader in = new BufferedReader(
      new InputStreamReader(sourceDescr.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null)
      System.out.println(inputLine);
    in.close();

  }

  private static Properties readProperties() throws IOException {
    Properties prop = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream stream = new FileInputStream("config.properties");
    InputStreamReader isr = new InputStreamReader(stream);
    prop.load(stream);
    return prop;
  }

}
