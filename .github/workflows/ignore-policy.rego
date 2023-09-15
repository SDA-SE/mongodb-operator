package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  "CVE-2023-42503" # commons-compression; only used in tests by Flapdoodle
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
