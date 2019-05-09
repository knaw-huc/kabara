package nl.knaw.huc.di.kabara;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import nl.knaw.huygens.timbuctoo.remote.rs.discover.Expedition;
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
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  public static void main(String[] args)
    throws IOException, SAXException, ParserConfigurationException, SaxonApiException, CantRetrieveFileException,
    CantDetermineDataSetException, JAXBException, URISyntaxException, InterruptedException, ParseException {

    XdmNode configs = Saxon.buildDocument(new StreamSource(args[0]));

    Logger log = Logger.getLogger("Main");
    log.getParent().setLevel(Level.OFF);
    log.setLevel(Level.OFF);

    String resourceSync = Saxon.xpath2string(configs, "/kabara/timbuctoo/resourcesync");
    System.out.println("resourceSync: "+resourceSync);

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

    CloseableHttpClient httpclient = HttpClients.createMinimal();
    // System.out.println(httpclient.getConnectionManager().getClass());
    ResourceSyncContext rsc = new ResourceSyncContext();
    Expedition expedition = new Expedition(httpclient,rsc);
    List<ResultIndex> result = expedition.explore(resourceSync, null);

    PrintWriter out = new PrintWriter(
      new BufferedWriter(
        new FileWriter("src/test/out/result.txt",false)
      ),
      true);

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

    ImportManager im = new ImportManager(target, credsProvider);
    ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(httpclient), true);
    String capabilityKistUri =
      "http://localhost:8080/v5/resourcesync/u33707283d426f900d4d33707283d426f900d4d0d/clusius/capabilitylist.xml";
    ResourceSyncImport.ResourceSyncReport result_rsi =
      rsi.filterAndImport(capabilityKistUri, null, false, "", im,null, base, base);
        //new SimpleDateFormat().parse(synced));

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

    System.exit(1);

    //   nu timbuctoo benaderen:
    //  https://repository.huygens.knaw.nl/v5/resourcesync/sourceDescription.xml
    URL sourceDescr = new URL(resourceSync);
    BufferedReader in = new BufferedReader(
      new InputStreamReader(sourceDescr.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      String[] parts = inputLine.substring(1,inputLine.length()-1).split("><");
      System.out.println(String.join(">\n<", parts));
    }
    in.close();
    System.exit(1);

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
