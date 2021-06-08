package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDb;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbEventHandler implements ResourceEventHandler<MongoDb> {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbEventHandler.class);

  @Override
  public void onAdd(MongoDb obj) {
    LOG.info("MongoDb {}/{} added.", obj.getMetadata().getNamespace(), obj.getMetadata().getName());
  }

  @Override
  public void onUpdate(MongoDb oldObj, MongoDb newObj) {
    LOG.info(
        "MongoDb {}/{} updated.",
        oldObj.getMetadata().getNamespace(),
        oldObj.getMetadata().getName());
  }

  @Override
  public void onDelete(MongoDb obj, boolean deletedFinalStateUnknown) {
    LOG.info(
        "MongoDb {}/{} deleted.", obj.getMetadata().getNamespace(), obj.getMetadata().getName());
  }
}
