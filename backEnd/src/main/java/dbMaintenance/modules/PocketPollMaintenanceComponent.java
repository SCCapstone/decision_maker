package dbMaintenance.modules;

import dbMaintenance.controllers.KeyUserRatingsByVersionController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollMaintenanceModule.class)
public interface PocketPollMaintenanceComponent {
  void inject(KeyUserRatingsByVersionController keyUserRatingsByVersionController);
}
