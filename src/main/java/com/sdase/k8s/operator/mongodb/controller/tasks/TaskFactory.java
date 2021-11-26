package com.sdase.k8s.operator.mongodb.controller.tasks;

import com.mongodb.ConnectionString;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.ConnectionStringUtil;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.NamingUtil;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.PasswordUtil;
import com.sdase.k8s.operator.mongodb.model.v1beta1.DatabaseSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

public class TaskFactory {

  private final Function<MongoDbCustomResource, String> usernameCreator;
  private final Function<MongoDbCustomResource, String> passwordCreator;
  private final Function<MongoDbCustomResource, String> databaseNameCreator;

  public static TaskFactory defaultFactory() {
    return new TaskFactory(
        NamingUtil::fromNamespaceAndName,
        mdbCr -> PasswordUtil.createPassword(),
        NamingUtil::fromNamespaceAndName);
  }

  public static TaskFactory customFactory(
      Function<MongoDbCustomResource, String> usernameCreator,
      Function<MongoDbCustomResource, String> passwordCreator,
      Function<MongoDbCustomResource, String> databaseNameCreator) {
    return new TaskFactory(usernameCreator, passwordCreator, databaseNameCreator);
  }

  private TaskFactory(
      Function<MongoDbCustomResource, String> usernameCreator,
      Function<MongoDbCustomResource, String> passwordCreator,
      Function<MongoDbCustomResource, String> databaseNameCreator) {
    this.usernameCreator = usernameCreator;
    this.passwordCreator = passwordCreator;
    this.databaseNameCreator = databaseNameCreator;
  }

  public CreateDatabaseTask newCreateTask(
      MongoDbCustomResource mongoDbCustomResource, ConnectionString connectionString) {
    var dbName = databaseNameCreator.apply(mongoDbCustomResource);
    var username = usernameCreator.apply(mongoDbCustomResource);
    var password = passwordCreator.apply(mongoDbCustomResource);
    var options =
        Optional.ofNullable(mongoDbCustomResource.getSpec().getDatabase())
            .map(DatabaseSpec::getConnectionStringOptions)
            .filter(StringUtils::isNotBlank)
            .orElse(null);
    var newConnectionString =
        ConnectionStringUtil.createConnectionString(
            dbName, username, password, options, connectionString);

    return new CreateDatabaseTask(
        mongoDbCustomResource, dbName, username, password, newConnectionString);
  }

  public DeleteDatabaseTask newDeleteTask(MongoDbCustomResource mongoDbCustomResource) {
    return new DeleteDatabaseTask(
        mongoDbCustomResource,
        databaseNameCreator.apply(mongoDbCustomResource),
        usernameCreator.apply(mongoDbCustomResource),
        mongoDbCustomResource.getSpec().getDatabase().isPruneAfterDelete());
  }
}
