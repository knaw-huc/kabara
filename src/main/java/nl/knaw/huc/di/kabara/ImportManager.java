package nl.knaw.huc.di.kabara;

import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;

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
      String line = in.readLine();
      while (line != null && (teller++ < 10)) {
        System.out.println("rdf: " + line);
        for (String part : line.split(" \\.")) {
          System.out.println("part: " + part);
          Pattern p = Pattern.compile("^([+|-])(<[^>]+>) (<[^>]+>)(.*) (<[^>]+>)$");
          System.out.println("pattern: " + p);
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
            object = m.group(4);
            context = m.group(5);
            System.out.println("add/remove: " + m.group(1));
            System.out.println("subject: " + m.group(2));
            System.out.println("predicate: " + m.group(3));
            System.out.println("object: " + m.group(4));
            System.out.println("context: " + m.group(5));
          } else {
            System.out.println("no match");
            Pattern q = Pattern.compile(".");
            System.out.println("pattern: " + q);
            System.out.println("match? : " + q.matcher(part).matches());
          }
          // List<String> sublist = parts.subList(parts.size() - 4, parts.size() - 1);

          out.write(part + " .\n");
          // out.write(parts + " .\n");
          if (add)
            out.write("add:\n");
          else if (remove)
            out.write("remove:\n");
          out.write("subject: "+ subject + "\n  predicate: " + predicate + "\n  object: " + object + "\n  context: " + context + " .\n\n");
          out.flush();
          System.exit(1);
        }

        // out.write(line + "\n");
        // out.flush();
        line = in.readLine();
      }
      in.close();
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  private PrintWriter createOutputFile() throws IOException {
    return new PrintWriter(new BufferedWriter(new FileWriter("src/test/out/result.txt",false)));
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile");

  }

}
