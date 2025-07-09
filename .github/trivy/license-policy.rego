package trivy
import data.lib.trivy

default ignore := false

# permissive licenses from export of backend definition in Fossa,
# see policy-backend-fossa for reference
default permissive := {
    "0BSD",
    "AFL-3.0", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "android-sdk",
    "Apache-1.1", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "Apache-2.0", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "Artistic-1.0", # Safe if code isn’t modified and notice requirements are followed. Otherwise, you must state and disclose the source code of modifications/derivative works.
    "BouncyCastle",
    "BSD-1-Clause", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "BSD-2-Clause", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "BSD-3-Clause", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "BSD-3-Clause-No-Nuclear-Warranty",
    "BSD-4-Clause", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "CC-BY-2.5",
    "CC-BY-3.0",
    "CC0-1.0",
    "CDDL-1.0", # Safe if code isn’t modified and notice requirements are followed. Otherwise, you must state and disclose the source code of modifications/derivative works.
    "CDDL-1.1",
    "CPL-1.0",
    "EPL-1.0",
    "EPL-2.0",
    "GPL-2.0-with-classpath-exception", # Safe to include or link in an executable provided that source availability/attribution requirements are followed.
    "ICU",
    "ISC", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "JSON",
    "LGPL-2.0-only", # Requires you to (effectively) disclose your source code if the library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "LGPL-2.0-or-later", # Requires you to (effectively) disclose your source code if the library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "LGPL-2.1-only", # Requires you to (effectively) disclose your source code if the library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "LGPL-2.1-or-later", # Requires you to (effectively) disclose your source code if the library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "LGPL-3.0-only", # Requires you to (effectively) disclose your source code ifthe library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "LGPL-3.0-or-later", # Requires you to (effectively) disclose your source code ifthe library is statically linked to your project. Not triggered if dynamically linked or a separate process.
    "MIT", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "MPL-1.1", # Safe if code isn’t modified and notice requirements are followed. Otherwise, you must state and disclose the source code of modifications/derivative works.
    "MPL-2.0", # Safe if code isn’t modified and notice requirements are followed. Otherwise, you must state and disclose thesource code of modifications/derivative works.
    "OpenSSL",
    "public-domain",
    "SAX-PD",
    "Unlicense",
    "W3C", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "WTFPL", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
    "X11",
    "Zlib", # Permissive license which is perfectly safe to use provided proper attribution is given and retained.
  }

# mapping of licenses identified by cyclonedx to known license keys
default licenseMapping := {
    "Bouncy Castle Licence": "BouncyCastle",
    "Apache 2 License": "Apache-2.0",
    "GNU General Public License, version 2 with the GNU Classpath Exception": "GPL-2.0-with-classpath-exception",
    "Eclipse Public License (EPL) 2.0": "EPL-2.0",
  }

# default: allow everything defined in the list of permissive licenses
ignore {
  input.Name == permissive[_]
}

# allow licenses that are only named different due to the used tooling
ignore {
  licenseMapping[input.Name] == permissive[_]
}

# ch.qos.logback:logback-parent is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback/blob/master/LICENSE.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback:logback-parent"
  input.Name == "GNU Lesser General Public License"
}

# ch.qos.logback:logback-classic is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback/blob/master/LICENSE.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback:logback-classic"
  input.Name == "GNU Lesser General Public License"
}

# ch.qos.logback:logback-core is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback/blob/master/LICENSE.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback:logback-core"
  input.Name == "GNU Lesser General Public License"
}

# ch.qos.logback.contrib:logback-jackson is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback-contrib/blob/master/license-template.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback.contrib:logback-jackson"
  input.Name == "GNU Lesser General Public License"
}

# ch.qos.logback.contrib:logback-json-classic is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback-contrib/blob/master/license-template.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback.contrib:logback-json-classic"
  input.Name == "GNU Lesser General Public License"
}

# ch.qos.logback.contrib:logback-json-core is dual licensed as LGPL 2.1 or Eclipse Public License v1.0
# see https://github.com/qos-ch/logback-contrib/blob/master/license-template.txt
# cyclonedx identifies GNU Lesser General Public License
ignore {
  input.PkgName == "ch.qos.logback.contrib:logback-json-core"
  input.Name == "GNU Lesser General Public License"
}
