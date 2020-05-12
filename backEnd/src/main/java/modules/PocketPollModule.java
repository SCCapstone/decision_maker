package modules;

import dagger.Module;
import dagger.Provides;
import handlers.AddNewCategoryHandler;
import handlers.CreateNewGroupHandler;
import handlers.DeleteCategoryHandler;
import handlers.DeleteGroupHandler;
import handlers.EditCategoryHandler;
import handlers.EditGroupHandler;
import handlers.GetCategoriesHandler;
import handlers.GetGroupHandler;
import handlers.LeaveGroupHandler;
import handlers.OptUserInOutHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import handlers.UpdateUserSettingsHandler;
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
  public AddNewCategoryHandler provideAddNewCategoryHandler(final DbAccessManager dbAccessManager,
      final
      UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler) {
    return new AddNewCategoryHandler(dbAccessManager, updateUserChoiceRatingsHandler, this.metrics);
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
}
