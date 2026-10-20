# Phase 0: Maven multi-module skeleton + CI

## Concepts covered

**Maven and the POM.** Maven builds Java projects: downloads libraries, compiles, runs tests, packages jars.
Everything it needs to know lives in `pom.xml` (Project Object Model).

**Coordinates.** Every Maven artifact has a unique address: `groupId:artifactId:version`.
Ours: `dev.harsh.miniredis:miniredis-server:0.1.0-SNAPSHOT`. `SNAPSHOT` means "work in progress,
this version can still change" (as opposed to a fixed release like `0.1.0`).

**The lifecycle.** `mvn verify` runs a fixed sequence of phases, stopping at the first failure:

```
validate → compile → test → package → verify   (→ install → deploy)
```

Plugins do the real work in each phase: `maven-compiler-plugin` compiles, `maven-surefire-plugin` runs
unit tests, `maven-jar-plugin` builds the jar. Running a later phase always runs all earlier ones.

**Multi-module project.** One root project containing several sub-projects (modules), each with its
own `pom.xml` and its own jar. The root POM plays two separate roles, which are easy to mix up:

| Role | Where it's declared | What it means |
|---|---|---|
| Aggregation | root `<modules>` | "Build these modules when I'm built." |
| Inheritance | child `<parent>` | "Copy that POM's settings into me." |

**The reactor.** Maven's planner for multi-module builds. It reads every module, works out which depends
on which, and builds them in a valid order (e.g. client before starter). You saw it print
"Reactor Build Order".

**`dependencyManagement` vs `dependencies`.**
- `<dependencies>` = "put this library on my classpath."
- `<dependencyManagement>` = "IF any module uses this library, use THIS version." It adds nothing by itself.

That's why the child POM declares `junit-jupiter` with no version: the parent decides it.

**BOM (bill of materials).** A POM that only contains a list of versions for a family of artifacts.
Importing `junit-bom` makes all JUnit pieces (api, engine, params, launcher) use one matching version.

**Dependency scopes.**
| Scope | Available when | Passed on to modules that depend on you? |
|---|---|---|
| `compile` (default) | compiling + running everything | yes |
| `test` | only compiling + running tests | no |
| `provided` | compile time; someone else supplies it at runtime | no |
| `runtime` | running only, not compiling | yes |

JUnit is `test`: main code can't even import it, and it never leaks to whoever depends on the server.

**`release` vs `source`/`target`.** `source`/`target` control the language level and bytecode version.
`release` does that AND compiles against the Java 21 API list. So a Java 25 method used while
building on JDK 27 is a compile error instead of a `NoSuchMethodError` crash on a Java 21 machine.

**CI (GitHub Actions).** On every push to `main` and every pull request, GitHub starts a fresh Ubuntu
machine, checks out the code, installs JDK 21, and runs `mvn -B verify`. Green tick = the project builds
from scratch on a machine that isn't yours, on the Java version we promise.

## Key design decisions + trade-offs

| Decision | Why | Trade-off |
|---|---|---|
| Only `miniredis-server` exists for now | Each module is added in the phase that needs it; no empty modules, Spring Boot version chosen fresh in Phase 7 | The full 5-module structure isn't visible from day one |
| `maven.compiler.release=21`, local JDK 27, CI JDK 21 | Use a modern local JDK, but enforce the Java 21 API at compile time, and prove it on a real 21 in CI | Slight runtime differences between 27 and 21 are only caught by CI |
| Plugin versions pinned in `pluginManagement` | Same build on every machine/Maven version (reproducible builds) | We must bump versions ourselves |
| JUnit 5 via BOM, `test` scope | Required by the plan; BOM keeps JUnit's parts aligned | JUnit 6 is out; migrating later is small |
| `BuildSanityTest` canary | Proves the test pipeline works end to end before there's real code | A test that will rarely fail in practice |
| CI on push to `main` + PRs, `-B` batch mode | Covers both direct pushes and reviews; clean logs | Pushes to other branches without a PR don't run CI |
| Failsafe (integration tests) deferred to Phase 1 | Nothing to integration-test yet | — |

## 5 likely interview questions

**1. What's the difference between `<modules>` and `<parent>`?**
> `<modules>` is aggregation: the root says "build these too", which lets one command build everything.
> `<parent>` is inheritance: the child copies the parent's configuration (versions, plugins, properties).
> They're independent. You can aggregate without inheriting and vice versa. In my project the root
> POM does both, which is the common setup.

**2. What's the difference between `dependencyManagement` and `dependencies`?**
> `dependencies` actually adds a library to a module's classpath. `dependencyManagement` only fixes the
> version to use if a module adds it. I put versions in the parent's `dependencyManagement` (and import
> JUnit's BOM there) so every module uses the same versions, and child POMs list dependencies without versions.

**3. Why did you split the project into modules instead of one big project?**
> Mainly to enforce architecture. The server has zero runtime dependencies, but the Spring Boot starter needs
> Spring. Because they're separate modules, the server physically can't import Spring: it's a compile
> error, not a code-review comment. It also produces separate jars (apps only pull in the client + starter),
> keeps versions in one place, and one `mvn verify` builds and tests everything in the right order.

**4. What happens when you run `mvn verify`?**
> Maven runs the lifecycle phases in order: validate, compile, test, package, verify. Plugins bound to each
> phase do the work: compiler, Surefire for unit tests, jar plugin for packaging, and (later) Failsafe
> for integration tests in the verify phase. It stops at the first failure, so a red test means no jar.
> I use `verify` rather than `install` in CI because nothing needs to be copied to the local repository.

**5. What does your CI pipeline do and why does it use JDK 21 when you develop on 27?**
> On every push to main and every PR, GitHub Actions starts a clean Ubuntu runner, installs Temurin JDK 21,
> restores the Maven dependency cache, and runs `mvn -B verify`. Java 21 is our supported version. Locally
> `release 21` guarantees I only use Java 21 APIs; CI goes further and proves the code actually builds and
> passes tests on a real Java 21.

## How to explain this phase in 60 seconds

> "I set MiniRedis up as a Maven multi-module project. A parent POM holds the shared rules: Java 21, plus
> pinned versions for libraries and build plugins. Each deliverable (server, client, Spring Boot starter,
> demo, dashboard) becomes its own module with its own jar. The main reason is to enforce boundaries: the
> server must have zero runtime dependencies, and because it's a separate module it literally cannot import
> Spring, which only the starter uses. Versions are managed centrally through `dependencyManagement` and
> JUnit's BOM, so modules can't drift apart. I compile with `--release 21`, which blocks APIs newer than 21
> even on my newer local JDK, and GitHub Actions runs `mvn verify` on a real JDK 21 on every push and PR,
> so a green tick means the project builds from scratch on the version we support."
