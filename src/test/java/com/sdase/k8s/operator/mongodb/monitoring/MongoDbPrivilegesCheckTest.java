package com.sdase.k8s.operator.mongodb.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.db.manager.model.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MongoDbPrivilegesCheckTest {

  @Mock MongoDbService mongoDbService;

  @InjectMocks MongoDbPrivilegesCheck mongoDbPrivilegesCheck;

  @Test
  void shouldAcceptRootForUserManagement() {
    var givenUser = new User("id", "name", List.of(new User.UserRole("root", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToManageUsers();

    assertThat(actual).isTrue();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldAcceptUserAdminAnyDatabaseForUserManagement() {
    var givenUser =
        new User("id", "name", List.of(new User.UserRole("userAdminAnyDatabase", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToManageUsers();

    assertThat(actual).isTrue();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldAcceptUserAdminAnyDatabaseAlongWithOthersForUserManagement() {
    var givenUser =
        new User(
            "id",
            "name",
            List.of(
                new User.UserRole("userAdminAnyDatabase", "admin"),
                new User.UserRole("dbAdminAnyDatabase", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToManageUsers();

    assertThat(actual).isTrue();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldAcceptRootForDbManagement() {
    var givenUser = new User("id", "name", List.of(new User.UserRole("root", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToDeleteDatabases();

    assertThat(actual).isTrue();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldAcceptDbAdminAnyDatabaseAlongWithOthersForDbManagement() {
    var givenUser =
        new User(
            "id",
            "name",
            List.of(
                new User.UserRole("userAdminAnyDatabase", "admin"),
                new User.UserRole("dbAdminAnyDatabase", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToDeleteDatabases();

    assertThat(actual).isTrue();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldRejectForDbManagement() {
    var givenUser =
        new User(
            "id",
            "name",
            List.of(
                new User.UserRole("readWrite", "admin"), new User.UserRole("dbAdmin", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToDeleteDatabases();

    assertThat(actual).isFalse();
    verify(mongoDbService).whoAmI();
  }

  @Test
  void shouldRejectForUserManagement() {
    var givenUser =
        new User(
            "id",
            "name",
            List.of(
                new User.UserRole("readWrite", "admin"), new User.UserRole("dbAdmin", "admin")));
    when(mongoDbService.whoAmI()).thenReturn(Optional.of(givenUser));

    var actual = mongoDbPrivilegesCheck.hasPrivilegesToManageUsers();

    assertThat(actual).isFalse();
    verify(mongoDbService).whoAmI();
  }
}
