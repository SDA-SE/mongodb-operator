package com.sdase.k8s.operator.mongodb.db.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMongoDbTest {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractMongoDbTest.class);

  /**
   * {@value} is the name of the environment variable that may hold a <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">MongoDB Connection
   * String</a> of the database used in tests instead of starting a dedicated instance.
   */
  private static final String OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME =
      "TEST_MONGODB_CONNECTION_STRING";

  private static TransitionWalker.ReachedState<RunningMongodProcess> runningInstance;

  private static MongoClient mongo;

  private static String connectionString;
  private static boolean useExternalDb;

  private static Map<String, String> createdDatabases;

  protected static void startDb() {

    createdDatabases = new HashMap<>();

    var mongoDbUrlOverride = System.getenv(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME);
    useExternalDb = StringUtils.isNotBlank(mongoDbUrlOverride);

    if (useExternalDb) {
      LOG.info("Test configured to execute with external database.");
      connectionString = mongoDbUrlOverride;
      mongo = new MongoClient(connectionString);
    } else {
      LOG.info("Test will start local MongoDB database.");
      runningInstance =
          Mongod.instance().withMongodArguments(configureMongoDb()).start(Version.Main.V4_0);
      var serverAddress = runningInstance.asState().value().getServerAddress();
      var host = serverAddress.getHost();
      var port = serverAddress.getPort();

      var username = "test-user";
      var password = UUID.randomUUID().toString();

      connectionString = String.format("mongodb://%s:%s@%s:%d", username, password, host, port);
      mongo = new MongoClient(String.format("%s:%d", host, port)); // "no user" is admin in local db
      createDatabaseUser(username, password);
    }
  }

  protected static void removeDatabase(String databaseName) {
    mongo.getDatabase(databaseName).drop();
  }

  protected static void removeDatabases() {
    createdDatabases.values().stream()
        .filter(dbName -> !"admin".equals(dbName))
        .filter(dbName -> !"config".equals(dbName))
        .forEach(AbstractMongoDbTest::removeDatabase);
  }

  protected static void stopDb() {
    removeDatabases();
    if (!useExternalDb) {
      dropTestUser();
      runningInstance.close();
    }
  }

  protected String registerTestDb(String database) {
    if (!createdDatabases.containsKey(database)) {
      createdDatabases.put(database, database + "-" + UUID.randomUUID());
    }
    return createdDatabases.get(database);
  }

  protected void createDb(String database) {
    var dummyDocument = Document.parse("{\"test\":\"bar\"}");
    var collectionName = "test-provisioning-collection";
    mongo.getDatabase(database).getCollection(collectionName).insertOne(dummyDocument);
  }

  protected String getMongoDbConnectionString() {
    return connectionString;
  }

  private static Transition<MongodArguments> configureMongoDb() {
    return Start.to(MongodArguments.class)
        .initializedWith(MongodArguments.defaults().withArgs(Map.of("--noscripting", "")));
  }

  private static void createDatabaseUser(String username, String password) {
    var createUserCommand =
        new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append(
                "roles",
                List.of(new BasicDBObject("role", "userAdminAnyDatabase").append("db", "admin")));
    mongo.getDatabase("admin").runCommand(createUserCommand);
  }

  private static void dropTestUser() {
    var dropUserCommand = new BasicDBObject("dropUser", "test-user");
    mongo.getDatabase("admin").runCommand(dropUserCommand);
  }
}
