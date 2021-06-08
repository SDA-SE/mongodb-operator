package com.sdase.k8s.operator.mongodb.model.v1beta1;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1beta1")
@Group("persistence.sda-se.com")
@Kind("MongoDb")
public class MongoDb extends CustomResource<MongoDbSpec, MongoDbStatus> implements Namespaced {}
