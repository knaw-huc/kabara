package nl.knaw.huc.di.kabara;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.di.kabara.health.KabaraHealthCheck;
import nl.knaw.huc.di.kabara.resources.KabaraResource;

public class Kabara extends Application<KabaraConfiguration> {
    public static void main(String[] args) throws Exception {
        new Kabara().run(args);
    }

    @Override
    public String getName() {
        return "kabara";
    }

    @Override
    public void initialize(Bootstrap<KabaraConfiguration> bootstrap) {
        // nothing to do yet
    }

    @Override
    public void run(KabaraConfiguration configuration,
                    Environment environment) {
        final KabaraResource resource = new KabaraResource(
                configuration.getTemplate()
//                configuration.getDefaultName()
        );
        environment.admin().addTask(new RunKabaraTask());
//        Main.main(null);
        final KabaraHealthCheck healthCheck =
                new KabaraHealthCheck();
//                new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);
        // virtuoso afhankelijkheid !!!
        environment.jersey().register(resource);
    }

}
