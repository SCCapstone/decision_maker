package modules;

import controllers.AddNewCategoryController;
import controllers.CreateNewGroupController;
import controllers.DeleteCategoryController;
import controllers.DeleteGroupController;
import controllers.EditCategoryController;
import controllers.EditGroupController;
import controllers.GetCategoriesController;
import controllers.GetGroupController;
import controllers.LeaveGroupController;
import controllers.OptUserInOutController;
import controllers.UpdateSortSettingController;
import controllers.UpdateUserChoiceRatingsController;
import controllers.UpdateUserSettingsController;
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
  void inject(DeleteGroupController deleteGroupController);
  void inject(GetGroupController getGroupController);
  void inject(UpdateUserSettingsController updateUserSettingsController);
  void inject(CreateNewGroupController createNewGroupController);
  void inject(UpdateSortSettingController updateSortSettingController);
  void inject(EditGroupController editGroupController);
  void inject(OptUserInOutController optUserInOutController);
  void inject(LeaveGroupController leaveGroupController);
}
