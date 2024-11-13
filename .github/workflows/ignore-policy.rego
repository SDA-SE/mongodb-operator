package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # Jetty used by Javalin, we are not affeected, see https://github.com/javalin/javalin/issues/2340
  # Can only be "fixed" with Jetty 12.x, but Javalin is not compatible yet.
  # See also https://github.com/jetty/jetty.project/pull/12012#issuecomment-2427226041
  # > Like the CVE points out, if you are using the HttpURI directly in your webapp, and are using
  # > it in a proxy scenario which is used by Chrome, then stop using Jetty's HttpURI, and any other
  # > RFC3986 parser, and find a WhatWG parser instead.
  "CVE-2024-6763"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
