package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class DatabaseSpec {

  private boolean pruneOnDelete;

  public boolean isPruneOnDelete() {
    return pruneOnDelete;
  }

  public DatabaseSpec setPruneOnDelete(boolean pruneOnDelete) {
    this.pruneOnDelete = pruneOnDelete;
    return this;
  }
}
