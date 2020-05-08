package modules;

import controllers.AddNewCategoryController;
import controllers.DeleteCategoryController;
import controllers.EditCategoryController;
import controllers.GetCategoriesController;
import controllers.UpdateUserChoiceRatingsController;
import controllers.WarmingController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollModule.class)
public interface PocketPollComponent {
  void inject(AddNewCategoryController addNewCategoryController);
  void inject(EditCategoryController editCategoryController);
  void inject(GetCategoriesController getCategoriesController);
  void inject(DeleteCategoryController deleteCategoryController);
  void inject(WarmingController warmingController);
  void inject(UpdateUserChoiceRatingsController updateUserChoiceRatingsController);
}
