package nl.knaw.huc.di.kabara;

import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.PostBodyTask;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Set;

public class RunKabaraTask extends PostBodyTask {
    public  RunKabaraTask() {
        super("runKabara");
    }

    @Override
    public void execute(ImmutableMultimap<String, String> immutableMultimap, String postBody, PrintWriter printWriter) throws Exception {
        System.out.println("=== run Kabara ===");
        Set<String> keys = immutableMultimap.keySet();
        String configFile = "";
        try {
            configFile = immutableMultimap.get("config").toArray()[0].toString();
        } catch(Exception exc) {
            System.err.println("use -d config=filename_and_location (absolute path)");
            return;
        }
        for (String keyprint : keys) {
            System.out.println("Key = " + keyprint);
            Collection<String> values = immutableMultimap.get(keyprint);
            for(String value : values){
                System.out.println("Value= "+ value);
            }
        }
        Main.start(configFile);
    }
}
