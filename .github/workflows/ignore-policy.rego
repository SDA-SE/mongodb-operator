package trivy

import data.lib.trivy

default ignore = false

# add CVE as String and a comment why it s ignored
ignore_cves := {
  # Jackson Databind: https://github.com/advisories/GHSA-5jmj-h7xm-6q6v
  # @JsonIgnoreProperties is not used together with @JsonFormat(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
  # in our code or in the code of the Kubernetes client. So we are not affected.
  # This ignore rule can likely be removed after upgrading to Jackson 2.22.1 which is not published
  # yes.
  "CVE-2026-54515"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
