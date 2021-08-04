package com.sdase.k8s.operator.mongodb.monitoring;

import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.db.manager.model.User;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbPrivilegesCheck implements ReadinessCheck {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbPrivilegesCheck.class);

  private static final Set<String> REQUIRED_ROLES_USER_MANAGEMENT_ANY_OF =
      Set.of("root", "userAdminAnyDatabase");
  private static final Set<String> REQUIRED_ROLES_DB_MANAGEMENT_ANY_OF =
      Set.of("root", "dbAdminAnyDatabase");

  private final MongoDbService mongoDbService;
  private boolean checkedMissingPrivilegesForDeleteDatabase;

  public MongoDbPrivilegesCheck(MongoDbService mongoDbService) {
    this.mongoDbService = mongoDbService;
  }

  public boolean hasPrivilegesToManageUsers() {
    return hasAnyRoleOf(REQUIRED_ROLES_USER_MANAGEMENT_ANY_OF);
  }

  public boolean hasPrivilegesToDeleteDatabases() {
    return hasAnyRoleOf(REQUIRED_ROLES_DB_MANAGEMENT_ANY_OF);
  }

  @Override
  public boolean isReady() {
    var isReady = hasPrivilegesToManageUsers();
    if (isReady && !checkedMissingPrivilegesForDeleteDatabase) {
      if (!hasPrivilegesToDeleteDatabases()) {
        LOG.warn(
            "Not enough privileges to delete databases. "
                + "No support for MongoDb resources with spec.database.pruneAfterDelete = true. "
                + "Deleting such resources from the cluster will fail!");
      }
      checkedMissingPrivilegesForDeleteDatabase = true;
    }
    return isReady;
  }

  private boolean hasAnyRoleOf(Set<String> requiredRolesAnyOf) {
    var user = mongoDbService.whoAmI();
    return user.map(
            u ->
                u.getRoles().stream()
                    .map(User.UserRole::getRole)
                    .anyMatch(requiredRolesAnyOf::contains))
        .orElse(false);
  }
}
