package com.sdase.k8s.operator.mongodb.db.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.os.CommonArchitecture;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.ImmutablePlatform;
import de.flapdoodle.os.Platform;
import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.ImmutableStart;
import de.flapdoodle.reverse.transitions.Start;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  static final String OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME =
      "TEST_MONGODB_CONNECTION_STRING";

  private static TransitionWalker.ReachedState<RunningMongodProcess> runningInstance;

  private static MongoClient mongo;

  private static String connectionString;
  private static boolean useExternalDb;

  private static Map<String, String> createdDatabases;

  protected static void startDb() {
    startDb(Version.Main.V5_0);
  }

  protected static void startDb(IFeatureAwareVersion mongoDbVersion) {

    createdDatabases = new HashMap<>();

    var mongoDbUrlOverride = System.getenv(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME);
    useExternalDb = StringUtils.isNotBlank(mongoDbUrlOverride);

    if (useExternalDb) {
      LOG.info("Test configured to execute with external database.");
      connectionString = mongoDbUrlOverride;
      mongo = new MongoClient(connectionString);
    } else {
      LOG.info("Test will start local MongoDB database.");
      runningInstance = doStart(mongoDbVersion);
      var serverAddress = runningInstance.asState().value().getServerAddress();
      var host = serverAddress.getHost();
      var port = serverAddress.getPort();

      var username = "test-user";
      var password = UUID.randomUUID().toString();

      connectionString = String.format("mongodb://%s:%s@%s:%d", username, password, host, port);
      mongo = new MongoClient(String.format("%s:%d", host, port)); // "no user" is admin in local db
      createDatabaseUser(username, password);
      logVersion();
    }
  }

  private static void logVersion() {
    Document document = mongo.getDatabase("admin").runCommand(new Document("buildInfo", 1));
    String version = (String) document.get("version");
    LOG.info("Testing with version {}", version);
  }

  private static TransitionWalker.ReachedState<RunningMongodProcess> doStart(
      IFeatureAwareVersion mongoDbVersion) {
    try {
      return Mongod.instance().withMongodArguments(configureMongoDb()).start(mongoDbVersion);
    } catch (Exception e) {
      if (e.getCause() instanceof IllegalArgumentException
          && e.getCause().getMessage().contains("OS_X")
          && e.getCause().getMessage().contains("ARM_64")) {
        LOG.info("Failed to start on OS_X ARM_64, trying with X86_64", e);
        return startOsxX86(mongoDbVersion);
      }
      throw e;
    }
  }

  private static TransitionWalker.ReachedState<RunningMongodProcess> startOsxX86(
      IFeatureAwareVersion mongoDbVersion) {
    // no downloads for OsX arm anymore, try with x86
    ImmutablePlatform platform =
        ImmutablePlatform.builder()
            .operatingSystem(CommonOS.OS_X)
            .distribution(Optional.empty())
            .version(Optional.empty())
            .architecture(CommonArchitecture.X86_64)
            .build();
    ImmutableStart<Platform> platformStart =
        ImmutableStart.<Platform>builder()
            .destination(StateID.of(Platform.class))
            .action(() -> State.of(platform))
            .build();
    return Mongod.instance()
        .withMongodArguments(configureMongoDb())
        .withPlatform(platformStart)
        .start(mongoDbVersion);
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
