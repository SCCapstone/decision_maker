package DbMaintenance.modules;

import utilities.Metrics;

public class MaintenanceInjector {
  public static PocketPollMaintenanceComponent getInjector(final Metrics metrics) {
    return DaggerPocketPollMaintenanceComponent
        .builder()
        .pocketPollMaintenanceModule(new PocketPollMaintenanceModule(metrics))
        .build();
  }
}
