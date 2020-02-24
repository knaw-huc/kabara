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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class RunKabara {

  static Log log = LogFactory.getLog("RunKabara");
  private static String synced;
  private static String user;
  private static String pass;
  private static String endpoint;
  private static String path;
  private static String base;
  private static String configfilename;
  private static int timeout;

  public RunKabara(String args) throws SaxonApiException {
    configfilename = args;
    XdmNode configs = Saxon.buildDocument(new StreamSource(configfilename));
    user = Saxon.xpath2string(configs, "/kabara/triplestore/user");
    pass = Saxon.xpath2string(configs, "/kabara/triplestore/pass");
    endpoint = Saxon.xpath2string(configs, "/kabara/triplestore/endpoint");
    path = Saxon.xpath2string(configs, "/kabara/triplestore/path");
    base = Saxon.xpath2string(configs, "/kabara/dataset/@href");
    synced = Saxon.xpath2string(configs, "/kabara/dataset/synced");
    timeout = Integer.parseInt(Saxon.xpath2string(configs, "/kabara/timbuctoo/timeout"));
    log.info("endpoint: " + endpoint);
    log.info("path: " + path);
    log.info("user: " + user);
    log.info("pass: " + pass);
    log.info("timeout: " + timeout);
  }

  public ResourceSyncImport.ResourceSyncReport start(String dataset)
      throws IOException, ParserConfigurationException, CantRetrieveFileException,
      CantDetermineDataSetException, JAXBException, URISyntaxException, InterruptedException,
      TransformerException {

    log.info("dataset: " + dataset);
    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd, YYYY HH:mm:ss z", Locale.ENGLISH);
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    df.setLenient(true);
    Date syncDate = null;
    boolean update = false;
    try {
      syncDate = sdf.parse(synced);
      update = true;
    } catch (ParseException pe) {
      syncDate = new Date();
    }

    CloseableHttpClient httpclient = HttpClients.createMinimal();
    ResourceSyncContext rsc = new ResourceSyncContext();
    Expedition expedition = new Expedition(httpclient, rsc);
    Expedition.createWellKnownUri(new URI(dataset));

    List<ResultIndex> result = expedition.explore(dataset, null);
    for (ResultIndex ri : result) {
      Map<URI, Result<?>> rm = ri.getResultMap();
    }

    HttpHost target = HttpHost.create(endpoint);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope(target.getHostName(), target.getPort()),
        new UsernamePasswordCredentials(user, pass));

    VirtuosoImportManager im = new VirtuosoImportManager(target, credsProvider, endpoint + "/" + path);
    if (!update) {
      im.createDb("CREATE GRAPH <" + base + ">;");
    }
    ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(httpclient, timeout), true);
    ResourceSyncImport.ResourceSyncReport resultRsi =
        rsi.filterAndImport(dataset, null, update, "", im, syncDate, dataset, base);

    String newSyncDate = sdf.format(new Date());
    makeNewConfigFile(dataset, newSyncDate);
    return resultRsi;
  }

  private void makeNewConfigFile(String dataset, String syncDate)
      throws ParserConfigurationException, TransformerException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
    Document doc = documentBuilder.newDocument();
    // root element
    Element rootElement = doc.createElement("kabara");
    doc.appendChild(rootElement);
    Element timbuctoo = doc.createElement("timbuctoo");
    rootElement.appendChild(timbuctoo);
    Element resourceSync = doc.createElement("resourcesync");
    timbuctoo.appendChild(resourceSync);
    resourceSync.appendChild(doc.createTextNode(dataset));

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
    StreamResult result = new StreamResult(new File(configfilename));
    transformer.transform(source, result);
  }

}
