package nl.knaw.huc.di.kabara;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import nl.knaw.huygens.timbuctoo.remote.rs.discover.Expedition;
import nl.knaw.huygens.timbuctoo.remote.rs.discover.Result;
import nl.knaw.huygens.timbuctoo.remote.rs.discover.ResultIndex;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncFileLoader;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import nl.knaw.huygens.timbuctoo.remote.rs.download.exceptions.CantRetrieveFileException;
import nl.knaw.huygens.timbuctoo.remote.rs.exceptions.CantDetermineDataSetException;
import nl.knaw.huygens.timbuctoo.remote.rs.xml.ResourceSyncContext;
import nl.mpi.tla.util.Saxon;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  public static void main(String[] args)
    throws IOException, SAXException, ParserConfigurationException, SaxonApiException, CantRetrieveFileException,
    CantDetermineDataSetException, JAXBException, URISyntaxException, InterruptedException,
    TransformerException {

    XdmNode configs = Saxon.buildDocument(new StreamSource(args[0]));

    Logger log = Logger.getLogger("Main");
    log.getParent().setLevel(Level.OFF);
    log.setLevel(Level.OFF);

    String resourceSync = Saxon.xpath2string(configs, "/kabara/timbuctoo/resourcesync");
    System.out.println("resourceSync: "+resourceSync);
    int timeout = Integer.parseInt(Saxon.xpath2string(configs, "/kabara/timbuctoo/timeout"));

    String user = Saxon.xpath2string(configs, "/kabara/triplestore/user");
    String pass = Saxon.xpath2string(configs, "/kabara/triplestore/pass");
    String endpoint = Saxon.xpath2string(configs, "/kabara/triplestore/endpoint");
    String dataset = Saxon.xpath2string(configs, "/kabara/dataset/@id");
    String base = Saxon.xpath2string(configs, "/kabara/dataset/@href");
    String synced = Saxon.xpath2string(configs, "/kabara/dataset/synced");
    System.out.println("endpoint: " + endpoint);
    System.out.println("user: " + user);
    System.out.println("pass: " + pass);
    System.out.println("dataset: "+ dataset);

    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd, YYYY HH:mm:ss z", Locale.ENGLISH);
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    df.setLenient(true);
    Date syncDate = null;
    String newSyncDate = null;
    boolean update = false;
    try {
      syncDate = sdf.parse(synced);
      update = true;
    } catch (ParseException pe) {
      syncDate = new Date();
    }

    CloseableHttpClient httpclient = HttpClients.createMinimal();
    ResourceSyncContext rsc = new ResourceSyncContext();
    Expedition expedition = new Expedition(httpclient,rsc);
    Expedition.createWellKnownUri(new URI(resourceSync));

    System.err.println("get result");
    List<ResultIndex> result = expedition.explore(resourceSync, null);
    System.err.println("na get result");
    for(ResultIndex ri : result) {
      Map<URI, Result<?>> rm = ri.getResultMap();
      System.err.println("results: " + rm);
      for(URI rmk : rm.keySet()) {

        Result<?> a = rm.get(rmk);
        System.err.println(rmk);
        System.err.println(a);
      }
      System.err.flush();
    }
    // System.exit(1);

    // extract hostname, port, scheme from endpoint
    String[] partsOfEndpoint = endpoint.split(":?/");
    String hostname = partsOfEndpoint[2].split(":")[0];
    int port = Integer.parseInt(partsOfEndpoint[2].split(":")[1]);
    String scheme = partsOfEndpoint[0];

    HttpHost target = new HttpHost(hostname, port, scheme);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
      new AuthScope(target.getHostName(), target.getPort()),
      new UsernamePasswordCredentials(user, pass));

    ImportManager im = new ImportManager(target, credsProvider, endpoint);
    if(!update) {
      im.createDb("CREATE GRAPH <" + base + ">;");
    }
    ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(httpclient, timeout), true);
    String capabilityListUri = resourceSync;
      // "http://localhost:8080/v5/resourcesync/u33707283d426f900d4d33707283d426f900d4d0d/clusius/capabilitylist.xml";
    ResourceSyncImport.ResourceSyncReport result_rsi =
      rsi.filterAndImport(capabilityListUri, null, update, "", im, syncDate, base, base);


    // System.out.println("result_rsi: "+result_rsi.importedFiles);

    // for(ResultIndex ri : result) {
    //   System.out.println("count: "+ri.getCount());
    //   Map<URI, Result<?>> rm = ri.getResultMap();
    //   System.out.println(rm);
    //   System.out.println(rm.keySet());
    //   System.out.println(rm.values());
    //   Iterator<URI> iter = rm.keySet().iterator();
    //   while(iter.hasNext()) {
    //     System.out.println(iter.next());
    //   }
    //   // iterate Map
    // }

    newSyncDate = sdf.format(new Date());
    makeNewConfigFile(resourceSync, endpoint, user, pass, dataset, base, newSyncDate, args[0]);

    System.exit(0);

  }

  private static void makeNewConfigFile(String resourceSync, String endpoint, String user,
                                        String pass, String dataset, String base,
                                        String syncDate, String configFile)
    throws ParserConfigurationException, TransformerException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.newDocument();
    // root element
    Element rootElement = doc.createElement("kabara");
    doc.appendChild(rootElement);
    Element timbuctoo = doc.createElement("timbuctoo");
    rootElement.appendChild(timbuctoo);
    Element resourcesync = doc.createElement("resourcesync");
    timbuctoo.appendChild(resourcesync);
    resourcesync.appendChild(doc.createTextNode(resourceSync));

    Element tripleStore = doc.createElement("triplestore");
    rootElement.appendChild(tripleStore);
    Element endPoint = doc.createElement("endpoint");
    tripleStore.appendChild(endPoint);
    endPoint.appendChild(doc.createTextNode(endpoint));
    Element userE = doc.createElement("user");
    userE.appendChild((doc.createTextNode(user)));
    tripleStore.appendChild(userE);
    Element passE = doc.createElement("pass");
    passE.appendChild((doc.createTextNode(pass)));
    tripleStore.appendChild(passE);

    Element datasetE = doc.createElement("dataset");
    Attr attrType = doc.createAttribute("id");
    attrType.setValue(dataset);
    datasetE.setAttributeNode(attrType);
    attrType = doc.createAttribute("href");
    attrType.setValue(base);
    datasetE.setAttributeNode(attrType);
    rootElement.appendChild(datasetE);
    Element syncedE = doc.createElement("synced");
    datasetE.appendChild(syncedE);
    syncedE.appendChild(doc.createTextNode(syncDate.toString()));

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = null;
    transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new File(configFile));
    transformer.transform(source, result);
  }

}
