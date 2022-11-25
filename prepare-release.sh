#!/usr/bin/env sh

TAG_NAME="$1"

yq -i e ".images = [{\"name\": \"localhost:5000/mongodb-operator\", \"newName\": \"quay.io/sdase/mongodb-operator\", \"newTag\": \"${TAG_NAME}\"}]" kustomize/bases/release-no-ns/kustomization.yaml

kustomize build kustomize/bases/release > kustomize/bundle.yaml
kustomize build kustomize/bases/release-no-ns > kustomize/bundle-no-ns.yaml

git add kustomize/bundle.yaml
git add kustomize/bundle-no-ns.yaml
git add kustomize/bases/release-no-ns/kustomization.yaml
git commit -m "chore: prepare release ${TAG_NAME}"
git push
