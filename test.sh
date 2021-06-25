#!/bin/bash

echo "🏗 when creating MongoDB …"
kubectl apply -k kustomize/overlays/test/
sleep 5

echo "👀 then a secret should be created …"
secret="$(kubectl get secrets -n local-test local-test-db -o yaml)"
echo "  Found secret:"
echo "${secret}"
usernameBase64="$(echo "${secret}" | yq r - -p v data.u)"
echo "  Found base64 username in 'data.u': ${usernameBase64}"
echo "${secret}" | grep "bG9jYWwtdGVzdF9sb2NhbC10ZXN0LWRi" || exit 1

echo "👀 then a database user should be created …"

users="$(curl http://localhost/api/servers/mongodb.mongodb/databases/admin/collections/system.users/query?q=%7B%7D&sort=%7B%7D&skip=0&limit=20&project=%7B%7D)"
echo "Found users:"
echo "${users}"
echo "${users}" | jq '.results[] | select(._id=="local-test_local-test-db.local-test_local-test-db") | .roles[0].role' | grep "readWrite"
echo "${users}" | jq '.results[] | select(._id=="local-test_local-test-db.local-test_local-test-db") | .roles[0].db' | grep "local-test_local-test-db"
