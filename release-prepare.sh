#!/bin/bash

VERSION="$1"

# all files modified here for the release must be listed in the git plugin in .releaserc.yml

# update remote base reference tag in examples
yq -i ". | .resources[0] |= sub(\"^(.+ref=)(\\d+\\.\\d+\\.\\d+)$\", \"\${1}${VERSION}\")" kustomize/overlays/remote-examples/namespace/kustomization.yaml
yq -i ". | .resources[1] |= sub(\"^(.+ref=)(\\d+\\.\\d+\\.\\d+)$\", \"\${1}${VERSION}\")" kustomize/overlays/remote-examples/no-namespace/kustomization.yaml

# update image tag in release base
yq -i ". | .images[0].newTag = \"${VERSION}\"" kustomize/release/no-namespace/kustomization.yaml

# verify files changed
CHANGES=`git diff --name-only`
echo "$CHANGES"
echo ""
echo "$CHANGES" | grep "kustomize/overlays/remote-examples/namespace/kustomization.yaml"
echo "$CHANGES" | grep "kustomize/overlays/remote-examples/no-namespace/kustomization.yaml"
echo "$CHANGES" | grep "kustomize/release/no-namespace/kustomization.yaml"
