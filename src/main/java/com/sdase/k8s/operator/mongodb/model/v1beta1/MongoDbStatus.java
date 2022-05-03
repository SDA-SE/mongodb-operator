package com.sdase.k8s.operator.mongodb.model.v1beta1;

import io.fabric8.kubernetes.api.model.Condition;
import java.util.ArrayList;
import java.util.List;

public class MongoDbStatus {

  private List<Condition> conditions = new ArrayList<>();

  public List<Condition> getConditions() {
    return conditions;
  }

  public MongoDbStatus setConditions(List<Condition> conditions) {
    this.conditions = conditions;
    return this;
  }
}
