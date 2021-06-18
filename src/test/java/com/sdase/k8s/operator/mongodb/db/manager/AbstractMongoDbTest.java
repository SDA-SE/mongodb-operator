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
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.bson.Document;

public abstract class AbstractMongoDbTest {

  /**
   * please store Starter or RuntimeConfig in a static final field if you want to use artifact store
   * caching (or else disable caching)
   */
  private static final MongodStarter starter = MongodStarter.getDefaultInstance();

  private static MongodExecutable mongodExe;
  private static MongodProcess mongod;

  private static MongoClient mongo;

  private static String host;
  private static int port;
  private static String username;
  private static String password;

  protected static void startDb() throws IOException {
    host = Network.getLocalHost().getHostName();
    port = Network.getFreeServerPort();
    username = "test-user";
    password = UUID.randomUUID().toString();
    var mongodConfig = createMongodConfig();
    mongodExe = starter.prepare(mongodConfig);
    mongod = mongodExe.start();

    mongo = new MongoClient(host + ":" + port);
    createDatabaseUser();
  }

  protected static void removeDatabases() {
    StreamSupport.stream(mongo.listDatabaseNames().spliterator(), false)
        .filter(dbName -> !"admin".equals(dbName))
        .filter(dbName -> !"config".equals(dbName))
        .forEach(dbName -> mongo.getDatabase(dbName).drop());
  }

  protected static void stopDb() {
    removeDatabases();
    mongod.stop();
    mongodExe.stop();
  }

  protected void createDb(String database) {
    var dummyDocument = Document.parse("{\"test\":\"bar\"}");
    var collectionName = "test-provisioning-collection";
    mongo.getDatabase(database).getCollection(collectionName).insertOne(dummyDocument);
  }

  protected String getMongoDbConnectionString() {
    return String.format("mongodb://%s:%s@%s:%d/", username, password, host, port);
  }

  private static MongodConfig createMongodConfig() {
    return createMongodConfigBuilder().build();
  }

  private static ImmutableMongodConfig.Builder createMongodConfigBuilder() {
    return ImmutableMongodConfig.builder()
        .version(Version.Main.V4_0)
        .putArgs("--noscripting", "")
        .net(new Net(host, port, false));
  }

  private static void createDatabaseUser() {
    var createUserCommand =
        new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append("roles", List.of(new BasicDBObject("role", "userAdmin").append("db", "admin")));
    mongo.getDatabase("admin").runCommand(createUserCommand);
  }
}
