package nl.knaw.huc.di.kabara;

import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;

public class ImportManager implements nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager {
  @Override
  public boolean isRdfTypeSupported(MediaType mediaType) {
    System.out.println("isRdfTypeSupported");
    return true;
  }

  @Override
  public Future<ImportStatus> addLog(String baseUri, String defaultGraph, String fileName,
                                     InputStream rdfInputStream,
                                     Optional<Charset> charset, MediaType mediaType) {
    System.out.println("addLog");
    BufferedReader in = new BufferedReader(new InputStreamReader(rdfInputStream));
    try {
      System.out.println("rdf: " + in.readLine());
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void addFile(InputStream inputStream, String s, MediaType mediaType) {
    System.out.println("addFile");

  }
}
