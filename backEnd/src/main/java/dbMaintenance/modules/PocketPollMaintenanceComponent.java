package dbMaintenance.modules;

import dbMaintenance.controllers.AddCategoryCreatorToGroupController;
import dbMaintenance.controllers.KeyChoicesByLabelController;
import dbMaintenance.controllers.UnkeyUserRatingsByVersionController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollMaintenanceModule.class)
public interface PocketPollMaintenanceComponent {
  void inject(UnkeyUserRatingsByVersionController unkeyUserRatingsByVersionController);
  void inject(AddCategoryCreatorToGroupController addCategoryCreatorToGroupController);
  void inject(KeyChoicesByLabelController keyChoicesByLabelController);
}
