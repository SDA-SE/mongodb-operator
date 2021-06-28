package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class DatabaseSpec {

  private boolean pruneAfterDelete;

  public boolean isPruneAfterDelete() {
    return pruneAfterDelete;
  }

  public DatabaseSpec setPruneAfterDelete(boolean pruneAfterDelete) {
    this.pruneAfterDelete = pruneAfterDelete;
    return this;
  }
}
