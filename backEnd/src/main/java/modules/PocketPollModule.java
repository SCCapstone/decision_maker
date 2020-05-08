package modules;

import controllers.EditCategoryController;
import dagger.Module;
import dagger.Provides;
import handlers.AddNewCategoryHandler;
import handlers.DeleteCategoryHandler;
import handlers.EditCategoryHandler;
import handlers.GetCategoriesHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import handlers.WarmingHandler;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import managers.DbAccessManager;
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
}
