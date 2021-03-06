package modules;

import controllers.AddFavoriteController;
import controllers.CreateNewGroupController;
import controllers.DeleteCategoryController;
import controllers.DeleteGroupController;
import controllers.EditCategoryController;
import controllers.EditGroupController;
import controllers.GetAllBatchesOfEventsController;
import controllers.GetBatchOfEventsController;
import controllers.GetCategoriesController;
import controllers.GetEventController;
import controllers.GetGroupController;
import controllers.GetUserDataController;
import controllers.GiveAppFeedbackController;
import controllers.LeaveGroupController;
import controllers.MarkAllEventsSeenController;
import controllers.MarkEventAsSeenController;
import controllers.NewCategoryController;
import controllers.NewEventController;
import controllers.OptUserInOutController;
import controllers.PendingEventResolutionController;
import controllers.PendingEventsScanningController;
import controllers.RegisterPushEndpointController;
import controllers.RejoinGroupController;
import controllers.ReportGroupController;
import controllers.ReportUserController;
import controllers.SetUserGroupMuteController;
import controllers.UnregisterPushEndpointController;
import controllers.UpdateSortSettingController;
import controllers.UpdateUserChoiceRatingsController;
import controllers.UpdateUserSettingsController;
import controllers.VoteForChoiceController;
import controllers.WarmingController;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = PocketPollModule.class)
public interface PocketPollComponent {
  void inject(NewCategoryController newCategoryController);
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
  void inject(RejoinGroupController rejoinGroupController);
  void inject(VoteForChoiceController voteForChoiceController);
  void inject(GetUserDataController getUserDataController);
  void inject(RegisterPushEndpointController registerPushEndpointController);
  void inject(UnregisterPushEndpointController unregisterPushEndpointController);
  void inject(MarkEventAsSeenController markEventAsSeenController);
  void inject(SetUserGroupMuteController setUserGroupMuteController);
  void inject(MarkAllEventsSeenController markAllEventsSeenController);
  void inject(GetBatchOfEventsController getBatchOfEventsController);
  void inject(NewEventController newEventController);
  void inject(PendingEventsScanningController pendingEventsScanningController);
  void inject(PendingEventResolutionController pendingEventResolutionController);
  void inject(GetEventController getEventController);
  void inject(AddFavoriteController addFavoriteController);
  void inject(GetAllBatchesOfEventsController getAllBatchesOfEventsController);
  void inject(ReportUserController reportUserController);
  void inject(ReportGroupController reportGroupController);
  void inject(GiveAppFeedbackController giveAppFeedbackController);
}
