package DbMaintenance.modules;

import DbMaintenance.Managers.MaintenanceDbAccessManager;
import DbMaintenance.handlers.KeyUserRatingsByVersionHandler;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import utilities.Metrics;

@Module
@RequiredArgsConstructor
public class PocketPollMaintenanceModule {

  private final Metrics metrics;

  @Provides
  @Singleton
  public MaintenanceDbAccessManager provideMaintenanceDbAccessManager() {
    return new MaintenanceDbAccessManager();
  }

  @Provides
  public KeyUserRatingsByVersionHandler provideKeyUserRatingsByVersionHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager) {
    return new KeyUserRatingsByVersionHandler(maintenanceDbAccessManager, this.metrics);
  }
}
