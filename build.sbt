import com.typesafe.sbt.packager.docker._

name := "verifiable-credentials-service"

version := "0.1.0"

scalaVersion := "2.13.1"

val catsVersion = "2.0.0"
val catsEffectVersion = "2.0.0"
val catsMtlVersion = "0.7.0"
val circeVersion = "0.12.0"
val http4sVersion = "0.21.0-M5"
val pureConfigVersion = "0.12.1"
val fs2Version = "2.0.0"
val izumiVersion = "0.9.4"

val scalaTestVersion = "3.0.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "org.typelevel" %% "cats-mtl-core" % catsMtlVersion,
  "org.http4s"    %% "http4s-core" % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"    %% "http4s-blaze-client" % http4sVersion,
  "org.http4s"    %% "http4s-circe" % http4sVersion,
  "org.http4s"    %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-reactive-streams" % fs2Version,

  "org.parboiled" %% "parboiled" % "2.1.8",

  "org.bouncycastle" % "bcpkix-jdk15on" % "1.64",

  "io.7mind.izumi" %% "logstage-core" % izumiVersion,
  "io.7mind.izumi" %% "logstage-rendering-circe" % izumiVersion,
  "io.7mind.izumi" %% "logstage-adapter-slf4j" % izumiVersion,

  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "io.circe" %% "circe-literal" % circeVersion % Test,

).map(_.withSources())

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

dockerBaseImage := "openjdk:8u212-jre-alpine"
packageName in Docker := "git.usafe.digital:4567/usafe/did-registry"

dockerCommands ++= Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "apk update && apk upgrade && apk add bash && apk add nss")
)

scalacOptions ++= Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xmacro-settings:materialize-derivations", //Explain internal macro derivations
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
)

scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")

fork in Test := true

cancelable in Global := true

