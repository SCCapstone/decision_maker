package modules;

import dagger.Module;
import dagger.Provides;
import handlers.GetBatchOfEventsHandler;
import handlers.NewCategoryHandler;
import handlers.CreateNewGroupHandler;
import handlers.DeleteCategoryHandler;
import handlers.DeleteGroupHandler;
import handlers.EditCategoryHandler;
import handlers.EditGroupHandler;
import handlers.GetCategoriesHandler;
import handlers.GetGroupHandler;
import handlers.GetUserDataHandler;
import handlers.LeaveGroupHandler;
import handlers.MarkAllEventsSeenHandler;
import handlers.MarkEventAsSeenHandler;
import handlers.NewEventHandler;
import handlers.OptUserInOutHandler;
import handlers.RegisterPushEndpointHandler;
import handlers.RejoinGroupHandler;
import handlers.SetUserGroupMuteHandler;
import handlers.UnregisterPushEndpointHandler;
import handlers.UpdateSortSettingHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import handlers.UpdateUserSettingsHandler;
import handlers.VoteForChoiceHandler;
import handlers.WarmingHandler;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import utilities.Metrics;

@Module
@RequiredArgsConstructor
public class PocketPollModule {

  private final Metrics metrics;

  @Provides
  @Singleton
  public DbAccessManager provideDbAccessManager() {
    return new DbAccessManager();
  }

  @Provides
  @Singleton
  public S3AccessManager provideS3AccessManager() {
    return new S3AccessManager();
  }

  @Provides
  @Singleton
  public SnsAccessManager provideSnsAccessManager() {
    return new SnsAccessManager();
  }

  @Provides
  public NewCategoryHandler provideAddNewCategoryHandler(final DbAccessManager dbAccessManager,
      final
      UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler) {
    return new NewCategoryHandler(dbAccessManager, updateUserChoiceRatingsHandler, this.metrics);
  }

  @Provides
  public EditCategoryHandler provideEditCategoryHandler(final DbAccessManager dbAccessManager, final
  UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler) {
    return new EditCategoryHandler(dbAccessManager, updateUserChoiceRatingsHandler, this.metrics);
  }

  @Provides
  public GetCategoriesHandler provideGetCategoriesHandler(final DbAccessManager dbAccessManager) {
    return new GetCategoriesHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public DeleteCategoryHandler provideDeleteCategoryHandler(final DbAccessManager dbAccessManager) {
    return new DeleteCategoryHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public WarmingHandler provideWarmingHandler(final DbAccessManager dbAccessManager) {
    return new WarmingHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public UpdateUserChoiceRatingsHandler provideUpdateUserChoiceRatingsHandler(
      final DbAccessManager dbAccessManager) {
    return new UpdateUserChoiceRatingsHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public DeleteGroupHandler provideDeleteGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager) {
    return new DeleteGroupHandler(dbAccessManager, s3AccessManager, snsAccessManager, this.metrics);
  }

  @Provides
  public GetGroupHandler provideGetGroupHandler(final DbAccessManager dbAccessManager) {
    return new GetGroupHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public UpdateUserSettingsHandler provideUpdateUserSettingsHandler(
      final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager) {
    return new UpdateUserSettingsHandler(dbAccessManager, s3AccessManager, this.metrics);
  }

  @Provides
  public UpdateSortSettingHandler provideUpdateSortSettingHandler(
      final DbAccessManager dbAccessManager) {
    return new UpdateSortSettingHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public CreateNewGroupHandler provideCreateNewGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager) {
    return new CreateNewGroupHandler(dbAccessManager, s3AccessManager, snsAccessManager,
        this.metrics);
  }

  @Provides
  public EditGroupHandler provideEditGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager) {
    return new EditGroupHandler(dbAccessManager, s3AccessManager, snsAccessManager, this.metrics);
  }

  @Provides
  public OptUserInOutHandler provideOptUserInOutHandler(final DbAccessManager dbAccessManager) {
    return new OptUserInOutHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public LeaveGroupHandler provideLeaveGroupHandler(final DbAccessManager dbAccessManager) {
    return new LeaveGroupHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public RejoinGroupHandler provideRejoinGroupHandler(final DbAccessManager dbAccessManager) {
    return new RejoinGroupHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public VoteForChoiceHandler provideVoteForChoiceHandler(final DbAccessManager dbAccessManager) {
    return new VoteForChoiceHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public GetUserDataHandler provideGetUserDataHandler(final DbAccessManager dbAccessManager) {
    return new GetUserDataHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public RegisterPushEndpointHandler provideRegisterPushEndpointHandler(
      final DbAccessManager dbAccessManager, final SnsAccessManager snsAccessManager,
      final UnregisterPushEndpointHandler unregisterPushEndpointHandler) {
    return new RegisterPushEndpointHandler(dbAccessManager, snsAccessManager,
        unregisterPushEndpointHandler, this.metrics);
  }

  @Provides
  public UnregisterPushEndpointHandler provideUnregisterPushEndpointHandler(
      final DbAccessManager dbAccessManager, final SnsAccessManager snsAccessManager) {
    return new UnregisterPushEndpointHandler(dbAccessManager, snsAccessManager, this.metrics);
  }

  @Provides
  public MarkEventAsSeenHandler provideMarkEventAsSeenHandler(
      final DbAccessManager dbAccessManager) {
    return new MarkEventAsSeenHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public SetUserGroupMuteHandler provideSetUserGroupMuteHandler(
      final DbAccessManager dbAccessManager) {
    return new SetUserGroupMuteHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public MarkAllEventsSeenHandler provideMarkAllEventsSeenHandler(
      final DbAccessManager dbAccessManager) {
    return new MarkAllEventsSeenHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public GetBatchOfEventsHandler provideGetBatchOfEventsHandler(
      final DbAccessManager dbAccessManager) {
    return new GetBatchOfEventsHandler(dbAccessManager, this.metrics);
  }

  @Provides
  public NewEventHandler provideNewEventHandler(final DbAccessManager dbAccessManager) {
    return new NewEventHandler(dbAccessManager, this.metrics);
  }
}
