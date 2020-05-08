package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import handlers.AddNewCategoryHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import managers.DbAccessManager;
import utilities.Metrics;

public class PocketPollModule extends AbstractModule {

  public static Metrics metrics;

//  protected void configure() {
//    install(new FactoryModuleBuilder().implement(ApiRequestHandler.class, AddNewCategoryHandler.class)
//        .build(AddNewCategoryFactory.class));
//
//    install(new FactoryModuleBuilder().implement(ApiRequestHandler.class, UpdateUserChoiceRatingsHandler.class)
//        .build(UpdateUserChoiceRatingsFactory.class));
//  }

  @Provides
  @Singleton
  static DbAccessManager provideDbAccessManager() {
    return new DbAccessManager();
  }

  @Provides
  static AddNewCategoryHandler provideAddNewCategoryHandler(final DbAccessManager dbAccessManager, final
      UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler) {
    return new AddNewCategoryHandler(dbAccessManager, updateUserChoiceRatingsHandler, metrics);
  }

  @Provides
  static UpdateUserChoiceRatingsHandler provideUpdateUserChoiceRatingsHandler(final DbAccessManager dbAccessManager) {
    return new UpdateUserChoiceRatingsHandler(dbAccessManager, metrics);
  }
}
