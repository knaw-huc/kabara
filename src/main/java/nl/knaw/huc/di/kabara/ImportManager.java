package nl.knaw.huc.di.kabara;

import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportManager implements nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager {
  @Override
  public boolean isRdfTypeSupported(MediaType mediaType) {
    System.out.println(mediaType + " is Rdf Type Supported");
    return true;
  }

  @Override
  public Future<ImportStatus> addLog(String baseUri, String defaultGraph, String fileName,
                                     InputStream rdfInputStream,
                                     Optional<Charset> charset, MediaType mediaType) {
    System.out.println("addLog");
    BufferedReader in = new BufferedReader(new InputStreamReader(rdfInputStream));
    int teller = 0;
    System.out.println("teller: " + teller);
    try {
      PrintWriter out = createOutputFile();

      // sendToSparQl("CREATE GRAPH <http://timbuctoo.huygens.knaw.nl/datasets/clusius>",out);

      String line = in.readLine();
      while (line != null && (teller++ < 20)) {
        System.out.println("rdf: " + line);
        for (String part : line.split(" \\.")) {
          // System.out.println("part: " + part);
          Pattern p = Pattern.compile("^([+|-])(<[^>]+>) (<[^>]+>)(.*) (<[^>]+>)$");
          // System.out.println("pattern: " + p);
          Matcher m = p.matcher(part);
          String subject = "";
          String predicate = "";
          String object = "";
          String context = "";
          boolean add = false;
          boolean remove = false;
          if(m.matches()) {
            add = m.group(1).equals("+");
            remove = m.group(1).equals("-");
            subject = m.group(2);
            predicate = m.group(3);
            object = m.group(4).trim();
            context = m.group(5);
            // System.out.println("add/remove: " + m.group(1));
            // System.out.println("subject: " + subject);
            // System.out.println("predicate: " + predicate);
            // System.out.println("object: " + object);
            // System.out.println("context: " + context);
          } else {
            System.out.println("no match");
          }
          // List<String> sublist = parts.subList(parts.size() - 4, parts.size() - 1);

          // out.println(part + " .");
          // out.println(parts + " .");
          if (teller>9) {
            if (add)
              out.println("add:");
            else if (remove)
              out.println("remove:");
            out.println(
              "subject: " + subject + "\n  predicate: " + predicate + "\n  object: " + object + "\n  context: " +
                context + " .\n");
            String sparQlOutput = "INSERT DATA\n" +
              "{\n" +
              "    GRAPH " + context + " {\n" +
              "        " + subject + "\n" +
              "            " + predicate + "\n" +
              "            " + object + " .\n" +
              "    }\n" +
              "}\n";
            out.println(sparQlOutput);
            sendToSparQl(sparQlOutput, out);
          }
          // System.exit(1);
        }
        // out.println(line);
        line = in.readLine();
      }
      in.close();
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  private void sendToSparQl(String sparQlOutput, PrintWriter out) throws IOException {

    // zoiets moet een 'query' er uitzien.
    // http://localhost:8890/sparql/endpoint?query=CREATE%20GRAPH%20%3Chttp://timbuctoo.huygens.knaw.nl/datasets/clusius%3E

    HttpHost target = new HttpHost("localhost", 8890, "http");
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
      new AuthScope(target.getHostName(), target.getPort()),
      new UsernamePasswordCredentials("demo", "demo"));
    CloseableHttpClient httpclient = HttpClients.custom()
                                                .setDefaultCredentialsProvider(credsProvider)
                                                .build();
    try {

      // Create AuthCache instance
      AuthCache authCache = new BasicAuthCache();
      // Generate DIGEST scheme object, initialize it and add it to the local
      // auth cache
      DigestScheme digestAuth = new DigestScheme();
      // Suppose we already know the realm name
      digestAuth.overrideParamter("realm", "SPARQL");
      // Suppose we already know the expected nonce value
      digestAuth.overrideParamter("nonce", "whatever");
      authCache.put(target, digestAuth);

      // Add AuthCache to the execution context
      HttpClientContext localContext = HttpClientContext.create();
      localContext.setAuthCache(authCache);

      HttpPost httppost = new HttpPost("http://localhost:8890/sparql-auth/");
      // httppost.setHeader("Content-type", "text/html");

      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("format", "application/sparql-results+xml");
      builder.addTextBody("query", sparQlOutput);
      // new File("src/test/out/spq_test.txt"));
      // ContentType.create("text/plain", "UTF-8"), "src/test/out/spq_test.txt");
      //   ContentType.create("application/sparql-results+xml")
      HttpEntity entity = builder.build();

      httppost.setEntity(entity);
      out.println("Executing request " + httppost.getRequestLine() + " to target:\n  " + target);
      out.println("entity: " + EntityUtils.toString(httppost.getEntity()));

      for (int i = 0; i < 1; i++) {
        CloseableHttpResponse response = httpclient.execute(target, httppost, localContext);
        try {
          out.println("----------------------------------------");
          out.println("" + response.getStatusLine());
          PrintWriter htmlOut = new PrintWriter(
            new BufferedWriter(new FileWriter("src/test/out/response.html", false)), true);
          String line = EntityUtils.toString(response.getEntity());
          // while (line!=null) {
            htmlOut.println(line);
            // line = responsIn.readLine();
          // }
          htmlOut.close();

          // out.println(EntityUtils.toString(response.getEntity()));
        } finally {
          response.close();
        }
      }
    } finally {
      httpclient.close();
    }

    System.exit(1);


  }
  private PrintWriter createOutputFile() throws IOException {
    return new PrintWriter(
      new BufferedWriter(
        new FileWriter("src/test/out/result.txt",false)
      ),
      true);
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile");

  }

}
