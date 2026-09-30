Releasing mod_cluster
=====================

Releases are done with the `maven-release-plugin`.
Artifacts are signed and uploaded to the JBoss Nexus repository,
from which they are synced to Maven Central.

How the Release Is Configured
-----------------------------

Most of the release configuration is inherited from `org.jboss:jboss-parent` and the project-specific overrides live in
`bom/pom.xml`.

* `release:perform` activates the `jboss-release` profile, which builds the javadoc and source-release archives,
  signs all artifacts with GPG and uploads them to JBoss Nexus (server id `jboss`) with the staging tag
  `org.jboss.mod_cluster-<version>`.
* Tests are skipped in both the `release:prepare` and `release:perform` builds (`-DskipTests -DskipITs`).
  We test combinations that are impossible to test from a single machine and platform in CI.
  Ensure that CI is green on the commit being released.
* SCM changes are temporarily kept manual: `release:prepare` does not push the release commits and tag, and `release:perform`
  builds from the local repository rather than cloning from GitHub. The commits and tag are pushed by hand once the
  upload succeeded.
* The `code-coverage` module is built but never deployed.

Prerequisites
-------------

* Temurin JDK 17
* No maven installation required – always use provided Maven Wrapper!
* Credentials for JBoss Nexus in `~/.m2/settings.xml` obtained from https://repository.jboss.org/nexus/

  ```xml
  <server>
      <id>jboss</id>
      <username>...</username>
      <password>...</password>
  </server>
  ```

* A GPG key usable by `gpg-agent` for signing artifacts and the release tag. Maven Central rejects unsigned
  artifacts and verifies signatures against the public key, so the key must be published on a public key server
  (e.g. `keys.openpgp.org`) and must not expire before the release is synced.
  The passphrase can be passed to Maven via the `MAVEN_GPG_PASSPHRASE` environment variable.
* A clean working tree and push access to `github.com/modcluster/mod_cluster`, referred to as the `upstream` remote
  below.

Release Steps
-------------

The examples below release `X.Y.Z.Final` and move the development version to `X.Y.(Z+1).Final-SNAPSHOT`.

1. Create a temporary `release` branch from the latest upstream `main`:

   ```
   git fetch upstream
   git checkout -b release upstream/main
   ```

2. Prepare the release. This sets the release version, builds, commits, creates a signed `X.Y.Z.Final` tag and then
   moves to the next development version. Nothing is pushed.

   ```
   ./mvnw -B release:prepare -DreleaseVersion=X.Y.Z.Final -DdevelopmentVersion=X.Y.(Z+1).Final-SNAPSHOT
   ```

3. Check the result. The tag must not contain any `SNAPSHOT` versions and `main` must only contain the next
   development version:

   ```
   git log --oneline -3
   git grep SNAPSHOT X.Y.Z.Final -- '*pom.xml'
   git grep SNAPSHOT -- '*pom.xml'
   ```

4. Perform the release. This builds the tag, signs the artifacts and uploads them to JBoss Nexus:

   ```
   ./mvnw -B release:perform
   ```

5. Check the uploaded artifacts in JBoss Nexus under `org/jboss/mod_cluster/*/X.Y.Z.Final`. Each jar must come with
   its `-sources.jar`, `-javadoc.jar` and `.asc` signatures.

6. Push the release commits and the tag, then remove the temporary branch:

   ```
   git push upstream release:main
   git push upstream X.Y.Z.Final
   git checkout main
   git branch -D release
   ```

7. Once synced, verify the release on
   [Maven Central](https://repo1.maven.org/maven2/org/jboss/mod_cluster/mod_cluster-core/).

Recovering From a Failed Release
--------------------------------

As long as nothing was uploaded or pushed, a failed release can simply be discarded and started over from step 1:

```
git checkout main
git branch -D release
git tag -d X.Y.Z.Final
./mvnw release:clean
```

Artifacts that were already synced to Maven Central cannot be changed or removed; fix the problem and release a new
version instead.

GitHub
------

1. Navigate to https://github.com/modcluster/mod_cluster/tags
2. Find the tag and create a release
3. Ensure to create discussion for the new release

Jira
----

1. Go to https://redhat.atlassian.net/projects/MODCLUSTER/versions/
2. Create the new release
3. Perform the release of the X.Y.Z.Final and move all issues to the next release

Happy releasing!

Rado
