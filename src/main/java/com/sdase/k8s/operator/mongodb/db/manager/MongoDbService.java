package com.sdase.k8s.operator.mongodb.db.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import javax.net.ssl.SSLContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbService {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbService.class);

  private final MongoClient mongoClient;

  public MongoDbService(String mongoDbConnectionString) {
    mongoClient = MongoClients.create(mongoDbConnectionString);
  }

  public MongoDbService(String mongoDbConnectionString, SSLContext sslContext) {
    var mongoClientSettings =
        MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mongoDbConnectionString))
            .applyToSslSettings(builder -> builder.context(sslContext))
            .build();
    mongoClient = MongoClients.create(mongoClientSettings);
  }

  /**
   * @param databaseName the name of the database where the user is stored
   * @param username the username of the user to check
   * @return if a user with the given {@code username} exists in the database with the given {@code
   *     databaseName}
   */
  public boolean userExists(String databaseName, String username) {
    var command = new BasicDBObject("usersInfo", Map.of("user", username, "db", databaseName));
    var result = mongoClient.getDatabase(databaseName).runCommand(command);
    if (result.get("users") instanceof Collection) {
      var users = (Collection<?>) result.get("users");
      return !users.isEmpty();
    }
    return false;
  }

  /**
   * Deletes the given user that has access to the given database.
   *
   * @param databaseName the databaseName where the user has access to
   * @param username the name of the user who should be deleted
   * @return if the user is gone
   */
  public boolean dropDatabaseUser(String databaseName, String username) {
    try {
      var dropUserCommand = new BasicDBObject("dropUser", username);
      var document = mongoClient.getDatabase(databaseName).runCommand(dropUserCommand);
      return isOk(document);
    } catch (MongoCommandException e) {
      return !userExists(databaseName, username);
    }
  }

  /**
   * Creates a new database and a matching user with {@code readWrite} access to it.
   *
   * <p>Technically only the user is created as MongoDB will create the database automatically when
   * documents inserted into collections of the database.
   *
   * @param databaseAndUserName the name of the new database and the new user
   * @param password the plain text password of the new user
   * @return if the user is created. The user may not be created if
   *     <ul>
   *       <li>the user already exists
   *       <li>the {@code createUser} command fails
   *     </ul>
   */
  public boolean createDatabaseWithUser(String databaseAndUserName, String password) {
    return createDatabaseWithUser(databaseAndUserName, databaseAndUserName, password);
  }

  /**
   * Creates a new database and a matching user with {@code readWrite} access to it.
   *
   * <p>Technically only the user is created as MongoDB will create the database automatically when
   * documents inserted into collections of the database.
   *
   * @param databaseName the name of the new database
   * @param username the name of the new user
   * @param password the plain text password of the new user
   * @return if the user is created. The user may not be created if
   *     <ul>
   *       <li>the user already exists
   *       <li>the {@code createUser} command fails
   *     </ul>
   */
  public boolean createDatabaseWithUser(String databaseName, String username, String password) {
    try {
      LOG.info("createDatabaseWithUser: {}@{}: check if database exists", username, databaseName);
      if (userExists(databaseName, username)) {
        LOG.info(
            "createDatabaseWithUser: {}@{}: skipping, database exists", username, databaseName);
        return false;
      }
      return createUser(databaseName, username, password);
    } catch (Exception e) {
      LOG.error("createDatabaseWithUser: {}@{}: failed", username, databaseName, e);
      return false;
    }
  }

  /**
   * Drops the database and all it's content.
   *
   * @param databaseName the name of the database to drop
   * @return if the database is gone
   */
  public boolean dropDatabase(String databaseName) {
    try {
      mongoClient.getDatabase(databaseName).drop();
      return !databaseExists(databaseName);
    } catch (MongoCommandException e) {
      return !databaseExists(databaseName);
    }
  }

  private boolean databaseExists(String databaseName) {
    MongoIterable<String> existingDatabases = mongoClient.listDatabaseNames();
    return StreamSupport.stream(existingDatabases.spliterator(), false)
        .anyMatch(databaseName::equals);
  }

  private boolean createUser(String databaseName, String username, String password) {
    LOG.info("createUser: {}@{}: creating user with readWrite access", username, databaseName);
    var createUserCommand =
        new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append(
                "roles",
                List.of(new BasicDBObject("role", "readWrite").append("db", databaseName)));
    Document response = mongoClient.getDatabase(databaseName).runCommand(createUserCommand);
    var created = isOk(response);
    LOG.info("createUser: {}@{}: created: {}", username, databaseName, created);
    return created;
  }

  private boolean isOk(Document response) {
    var okValue = response.get("ok", Object.class);
    if (okValue instanceof Double) {
      return Double.compare((Double) okValue, 1.0D) == 0;
    } else if (okValue instanceof Integer) {
      return (Integer) okValue == 1;
    } else {
      return false;
    }
  }
}
