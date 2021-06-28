package com.sdase.k8s.operator.mongodb.db.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbService {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbService.class);

  private final MongoClient mongoClient;

  public MongoDbService(String mongoDbConnectionString) {
    mongoClient = MongoClients.create(mongoDbConnectionString);
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
    return Double.compare(response.get("ok", Double.class), 1.0d) == 0;
  }
}
