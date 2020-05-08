package modules;

import dagger.Module;
import dagger.Provides;
import handlers.AddNewCategoryHandler;
import handlers.UpdateUserChoiceRatingsHandler;
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
  public AddNewCategoryHandler provideAddNewCategoryHandler(final DbAccessManager dbAccessManager, final
      UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler) {
    return new AddNewCategoryHandler(dbAccessManager, updateUserChoiceRatingsHandler, this.metrics);
  }

  @Provides
  public UpdateUserChoiceRatingsHandler provideUpdateUserChoiceRatingsHandler(final DbAccessManager dbAccessManager) {
    return new UpdateUserChoiceRatingsHandler(dbAccessManager, this.metrics);
  }
}
