package io.surfkit.lagomhelm.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import io.surfkit.lagomhelm.api.LagomhelmService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

class LagomhelmLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new LagomhelmApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new LagomhelmApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[LagomhelmService])
}

abstract class LagomhelmApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[LagomhelmService](wire[LagomhelmServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = LagomhelmSerializerRegistry

  // Register the LagomHelm persistent entity
  persistentEntityRegistry.register(wire[LagomhelmEntity])
}
