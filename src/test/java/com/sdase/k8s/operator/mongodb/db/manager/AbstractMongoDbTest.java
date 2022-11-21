package com.sdase.k8s.operator.mongodb.db.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
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

  /**
   * please store Starter or RuntimeConfig in a static final field if you want to use artifact store
   * caching (or else disable caching)
   */
  private static final MongodStarter starter = MongodStarter.getDefaultInstance();

  private static MongodExecutable mongodExe;
  private static MongodProcess mongod;

  private static MongoClient mongo;
  private static String testClientConnectionString;

  private static String connectionString;
  private static boolean useExternalDb;

  private static Map<String, String> createdDatabases;

  protected static void startDb() throws IOException {

    createdDatabases = new HashMap<>();

    var mongoDbUrlOverride = System.getenv(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME);
    useExternalDb = StringUtils.isNotBlank(mongoDbUrlOverride);

    if (useExternalDb) {
      LOG.info("Test configured to execute with external database.");
      connectionString = mongoDbUrlOverride;
      testClientConnectionString = connectionString;
    } else {
      LOG.info("Test will start local MongoDB database.");
      String host = Network.getLocalHost().getHostName();
      int port = Network.getFreeServerPort();
      var mongodConfig = createMongodConfig(host, port);
      mongodExe = starter.prepare(mongodConfig);
      mongod = mongodExe.start();

      var username = "test-user";
      var password = UUID.randomUUID().toString();

      connectionString = String.format("mongodb://%s:%s@%s:%d", username, password, host, port);
      testClientConnectionString =
          String.format("%s:%d", host, port); // "no user" is admin in local db
      createDatabaseUser(username, password);
    }
  }

  protected static void removeDatabase(String databaseName) {
    getMongoClient().getDatabase(databaseName).drop();
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
      mongod.stop();
      mongodExe.stop();
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
    getMongoClient().getDatabase(database).getCollection(collectionName).insertOne(dummyDocument);
  }

  protected String getMongoDbConnectionString() {
    return connectionString;
  }

  private static MongodConfig createMongodConfig(String host, int port) {
    return createMongodConfigBuilder(host, port).build();
  }

  private static ImmutableMongodConfig.Builder createMongodConfigBuilder(String host, int port) {
    return ImmutableMongodConfig.builder()
        .version(Version.Main.V4_0)
        .putArgs("--noscripting", "")
        .net(new Net(host, port, false));
  }

  private static void createDatabaseUser(String username, String password) {
    var createUserCommand =
        new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append(
                "roles",
                List.of(new BasicDBObject("role", "userAdminAnyDatabase").append("db", "admin")));
    getMongoClient().getDatabase("admin").runCommand(createUserCommand);
  }

  private static void dropTestUser() {
    var dropUserCommand = new BasicDBObject("dropUser", "test-user");
    getMongoClient().getDatabase("admin").runCommand(dropUserCommand);
  }

  private static synchronized MongoClient getMongoClient() {
    if (mongo == null) {
      mongo = new MongoClient(testClientConnectionString);
    }
    return mongo;
  }
}
