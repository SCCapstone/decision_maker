package dbMaintenance.modules;

import dbMaintenance.cronJobs.handlers.DetachedRatingsRemovalHandler;
import dbMaintenance.handlers.AddCategoryCreatorToGroupHandler;
import dbMaintenance.handlers.KeyChoicesByLabelHandler;
import dbMaintenance.handlers.UnkeyUserRatingsByVersionHandler;
import dbMaintenance.managers.MaintenanceDbAccessManager;
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
  public UnkeyUserRatingsByVersionHandler provideKeyUserRatingsByVersionHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager) {
    return new UnkeyUserRatingsByVersionHandler(maintenanceDbAccessManager, this.metrics);
  }

  @Provides
  public AddCategoryCreatorToGroupHandler provideAddCategoryCreatorToGroupHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager) {
    return new AddCategoryCreatorToGroupHandler(maintenanceDbAccessManager, this.metrics);
  }

  @Provides
  public KeyChoicesByLabelHandler provideKeyChoicesByLabelHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager) {
    return new KeyChoicesByLabelHandler(maintenanceDbAccessManager, this.metrics);
  }

  @Provides
  public DetachedRatingsRemovalHandler provideDetachedRatingsRemovalHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager) {
    return new DetachedRatingsRemovalHandler(maintenanceDbAccessManager, this.metrics);
  }
}
