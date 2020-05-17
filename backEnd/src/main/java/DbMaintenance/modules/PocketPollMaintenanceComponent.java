package DbMaintenance.modules;

import DbMaintenance.controllers.KeyUserRatingsByVersionController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollMaintenanceModule.class)
public interface PocketPollMaintenanceComponent {
  void inject(KeyUserRatingsByVersionController keyUserRatingsByVersionController);
}
