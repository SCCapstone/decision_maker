//package handlers;
//
//import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
//import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import managers.DbAccessManager;
//import models.Category;
//import models.User;
//import utilities.ErrorDescriptor;
//import utilities.Metrics;
//import utilities.RequestFields;
//import utilities.ResultStatus;
//
//public class DeleteCategoryHandler implements ApiRequestHandler {
//
//  public DeleteCategoryHandler(final DbAccessManager dbAccessManager,
//      final Map<String, Object> requestBody, final Metrics metrics) {
//    super(dbAccessManager, requestBody, metrics);
//  }
//
//  @Override
//  public ResultStatus handle() {
//    final String classMethod = "DeleteCategoryHandler.handle";
//
//    ResultStatus resultStatus = new ResultStatus();
//
//    final List<String> requiredKeys = Arrays
//        .asList(RequestFields.ACTIVE_USER, Category.CATEGORY_ID);
//
//    if (this.requestBody.keySet().containsAll(requiredKeys)) {
//      try {
//        final String activeUser = (String) this.requestBody.get((RequestFields.ACTIVE_USER));
//        final String categoryId = (String) this.requestBody.get(Category.CATEGORY_ID);
//
//        resultStatus = this.handle(activeUser, categoryId);
//      } catch (final Exception e) {
//        //something couldn't get parsed
//        this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
//        resultStatus.resultMessage = "Error: Invalid request.";
//      }
//    } else {
//      this.metrics
//          .log(new ErrorDescriptor<>(this.requestBody, classMethod,
//              "Required request keys not found"));
//      resultStatus.resultMessage = "Error: Required request keys not found.";
//    }
//
//    return resultStatus;
//  }
//
//  private ResultStatus handle(final String activeUser, final String categoryId) {
//    final String classMethod = "DeleteCategoryHandler.handle";
//    this.metrics.commonSetup(classMethod);
//
//    ResultStatus resultStatus = new ResultStatus();
//
//    try {
//      // Confirm that the username matches with the owner of the category before deleting it
//      final Category category = this.dbAccessManager.getCategory(categoryId);
//      if (activeUser.equals(category.getOwner())) {
//        this.removeCategoryFromGroups(category.getGroups().keySet(), categoryId);
//
//        //TODO These last two should probably be put into a ~transaction~
//        this.removeOwnedCategory(activeUser, categoryId);
//        this.dbAccessManager.deleteCategory(categoryId);
//
//        resultStatus = new ResultStatus(true, "Category deleted successfully!");
//      } else {
//        this.metrics.log(
//            new ErrorDescriptor<>(this.requestBody, classMethod,
//                "User is not the owner of the category"));
//        resultStatus.resultMessage = "Error: User is not the owner of the category.";
//      }
//    } catch (Exception e) {
//      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
//      resultStatus.resultMessage = "Error: Unable to parse request.";
//    }
//
//    this.metrics.commonClose(resultStatus.success);
//    return resultStatus;
//  }
//
//  /**
//   * This method handles removing one of a user's owned categories.
//   *
//   * @param username   The username of the user that owns the category that is being removed.
//   * @param categoryId The id of the owned category being removed.
//   * @return Standard result status object giving insight on whether the request was successful.
//   */
//  private ResultStatus removeOwnedCategory(final String username, final String categoryId) {
//    final String classMethod = "DeleteCategoryHandler.removeOwnedCategory";
//    this.metrics.commonSetup(classMethod);
//
//    ResultStatus resultStatus = new ResultStatus();
//
//    try {
//      final String updateExpression = "remove " + User.OWNED_CATEGORIES + ".#categoryId";
//      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
//
//      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
//          .withUpdateExpression(updateExpression)
//          .withNameMap(nameMap);
//
//      this.dbAccessManager.updateUser(username, updateItemSpec);
//      resultStatus = new ResultStatus(true, "Owned category removed successfully");
//    } catch (Exception e) {
//      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
//      resultStatus.resultMessage = "Exception in manager";
//    }
//
//    this.metrics.commonClose(resultStatus.success);
//    return resultStatus;
//  }
//
//  /**
//   * This function is called when a category is deleted and updates each item in the groups table
//   * that was linked to the category accordingly.
//   *
//   * @param groupIds   A set of group ids that need to have the category id removed from them.
//   * @param categoryId The catgory id to be removed.
//   * @return Standard result status object giving insight on whether the request was successful.
//   */
//  public ResultStatus removeCategoryFromGroups(final Set<String> groupIds,
//      final String categoryId) {
//    final String classMethod = "DeleteCategoryHandler.removeCategoryFromGroups";
//    this.metrics.commonSetup(classMethod);
//
//    ResultStatus resultStatus = new ResultStatus();
//
//    try {
//      final String updateExpression = "remove Categories.#categoryId";
//      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
//      UpdateItemSpec updateItemSpec;
//
//      for (final String groupId : groupIds) {
//        updateItemSpec = new UpdateItemSpec()
//            .withNameMap(nameMap)
//            .withUpdateExpression(updateExpression);
//        this.dbAccessManager.updateGroup(groupId, updateItemSpec);
//      }
//      resultStatus.success = true;
//    } catch (Exception e) {
//      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
//      resultStatus.resultMessage = "Error: Unable to parse request.";
//    }
//
//    this.metrics.commonClose(resultStatus.success);
//    return resultStatus;
//  }
//}
