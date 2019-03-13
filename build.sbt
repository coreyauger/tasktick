organization in ThisBuild := "io.surfkit"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val jwt = "com.pauldijou" %% "jwt-play-json" % "2.1.0"

val akkaMgmtVersion = "0.20.0"
val akkaManagement = "com.lightbend.akka.management" %% "akka-management" % akkaMgmtVersion
val akkaMgmtHttp =   "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaMgmtVersion
val akkaClusterBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaMgmtVersion
val akkaServiceDiscovery = "com.lightbend.akka.discovery" %% "akka-discovery-dns" % akkaMgmtVersion
val akkaDiscoveryK8s =  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaMgmtVersion
val akkaDiscoveryConfig = "com.lightbend.akka.discovery" %% "akka-discovery-config" % akkaMgmtVersion

val akkaManagementDeps = Seq(akkaManagement, akkaMgmtHttp, akkaClusterBootstrap, akkaServiceDiscovery, akkaDiscoveryK8s, akkaDiscoveryConfig)

lazy val `tasktick` = (project in file("."))
  .aggregate(`gateway-api`, `gateway-impl`, `projectmanager-api`, `projectmanager-impl`)


lazy val `projectmanager-api` = (project in file("projectmanager-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `projectmanager-impl` = (project in file("projectmanager-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      guice,
      macwire,
      scalaTest
    ) ++ akkaManagementDeps
  )
  .settings(lagomForkedTestSettings: _*)
  .settings(
    dockerAlias := dockerAlias.value.withRegistryHost(Option("127.0.0.1:30400"))
  )
  .dependsOn(`projectmanager-api`)


lazy val `gateway-api` = (project in file("gateway-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  ).dependsOn(`projectmanager-api`)

lazy val `gateway-impl` = (project in file("gateway-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      jwt,
      macwire,
      scalaTest
    ) ++ akkaManagementDeps
  )
  .settings(lagomForkedTestSettings: _*)
  .settings(
    dockerAlias := dockerAlias.value.withRegistryHost(Option("127.0.0.1:30400"))
  )
  .dependsOn(`gateway-api`)
  .dependsOn(`projectmanager-api`)

dockerBaseImage := "openjdk:8-jre-slim"