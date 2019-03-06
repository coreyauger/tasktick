package io.surfkit.projectmanager.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import io.surfkit.projectmanager.api.ProjectManagerService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

class ProjectManagerLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ServiceManagerApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new ServiceManagerApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[ProjectManagerService])
}

abstract class ServiceManagerApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[ProjectManagerService](wire[ProjectManagerServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = ServiceManagerSerializerRegistry

  // Register the ServiceManager persistent entity
  persistentEntityRegistry.register(wire[ProjectEntity])
}
