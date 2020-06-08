package dbMaintenance.modules;

import dbMaintenance.controllers.AddCategoryCreatorToGroupController;
import dbMaintenance.controllers.AddDurationsToUserSettingsController;
import dbMaintenance.controllers.AddOwnedGroupsCountController;
import dbMaintenance.controllers.KeyChoicesByLabelController;
import dbMaintenance.controllers.UnkeyUserRatingsByVersionController;
import dagger.Component;
import dbMaintenance.cronJobs.controllers.DetachedRatingsRemovalController;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollMaintenanceModule.class)
public interface PocketPollMaintenanceComponent {
  void inject(UnkeyUserRatingsByVersionController unkeyUserRatingsByVersionController);
  void inject(AddCategoryCreatorToGroupController addCategoryCreatorToGroupController);
  void inject(KeyChoicesByLabelController keyChoicesByLabelController);
  void inject(DetachedRatingsRemovalController detachedRatingsRemovalController);
  void inject(AddDurationsToUserSettingsController addDurationsToUserSettingsController);
  void inject(AddOwnedGroupsCountController addOwnedGroupsCountController);
}
