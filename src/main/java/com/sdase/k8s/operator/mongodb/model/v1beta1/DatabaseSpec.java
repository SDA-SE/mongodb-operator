package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class DatabaseSpec {

  private boolean pruneAfterDelete;

  private String connectionStringOptions;

  public boolean isPruneAfterDelete() {
    return pruneAfterDelete;
  }

  public DatabaseSpec setPruneAfterDelete(boolean pruneAfterDelete) {
    this.pruneAfterDelete = pruneAfterDelete;
    return this;
  }

  public String getConnectionStringOptions() {
    return connectionStringOptions;
  }

  public DatabaseSpec setConnectionStringOptions(String connectionStringOptions) {
    this.connectionStringOptions = connectionStringOptions;
    return this;
  }
}
