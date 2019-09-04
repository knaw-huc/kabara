package nl.knaw.huc.di.kabara.health;

import com.codahale.metrics.health.HealthCheck;

public class KabaraHealthCheck extends HealthCheck {
  @Override

  protected Result check() throws Exception {
    return Result.healthy();
  }
}
