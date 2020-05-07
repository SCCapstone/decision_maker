package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import factories.AddNewCategoryFactory;
import factories.UpdateUserChoiceRatingsFactory;
import handlers.AddNewCategoryHandler;
import handlers.ApiRequestHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import managers.DbAccessManager;

public class PocketPollModule extends AbstractModule {

  protected void configure() {
    install(new FactoryModuleBuilder().implement(ApiRequestHandler.class, AddNewCategoryHandler.class)
        .build(AddNewCategoryFactory.class));

    install(new FactoryModuleBuilder().implement(ApiRequestHandler.class, UpdateUserChoiceRatingsHandler.class)
        .build(UpdateUserChoiceRatingsFactory.class));
  }

  @Provides
  @Singleton
  static DbAccessManager provideDbAccessManager() {
    return new DbAccessManager();
  }
}
