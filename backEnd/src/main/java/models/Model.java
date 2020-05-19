package models;

import com.amazonaws.services.dynamodbv2.document.Item;

public interface Model {
  Item asItem();
}
