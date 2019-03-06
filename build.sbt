organization in ThisBuild := "io.surfkit"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val jwt = "com.pauldijou" %% "jwt-play-json" % "2.1.0"

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
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
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
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`gateway-api`)
  .dependsOn(`projectmanager-api`)

