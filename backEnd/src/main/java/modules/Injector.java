package modules;

import utilities.Metrics;

public class Injector {
  public static PocketPollComponent getInjector(final Metrics metrics) {
    return DaggerPocketPollComponent
        .builder()
        .pocketPollModule(new PocketPollModule(metrics))
        .build();
  }
}
