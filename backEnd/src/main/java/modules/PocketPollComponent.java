package modules;

import controllers.AddNewCategoryController;
import controllers.UpdateUserChoiceRatingsController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollModule.class)
public interface PocketPollComponent {
  void inject(AddNewCategoryController addNewCategoryController);
  void inject(UpdateUserChoiceRatingsController updateUserChoiceRatingsController);
}
