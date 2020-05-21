package dbMaintenance.modules;

import dbMaintenance.controllers.AddCategoryCreatorToGroupController;
import dbMaintenance.controllers.KeyUserRatingsByVersionController;
import dagger.Component;
import dbMaintenance.cronJobs.controllers.DetachedRatingsRemovalController;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollMaintenanceModule.class)
public interface PocketPollMaintenanceComponent {
  void inject(KeyUserRatingsByVersionController keyUserRatingsByVersionController);
  void inject(AddCategoryCreatorToGroupController addCategoryCreatorToGroupController);
  void inject(DetachedRatingsRemovalController detachedRatingsRemovalController);
}
