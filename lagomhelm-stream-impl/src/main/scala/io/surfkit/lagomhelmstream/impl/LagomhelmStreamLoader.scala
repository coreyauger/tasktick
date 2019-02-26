package io.surfkit.lagomhelmstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import io.surfkit.lagomhelmstream.api.LagomhelmStreamService
import io.surfkit.lagomhelm.api.LagomhelmService
import com.softwaremill.macwire._

class LagomhelmStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new LagomhelmStreamApplication(context) {
      override def serviceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new LagomhelmStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[LagomhelmStreamService])
}

abstract class LagomhelmStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[LagomhelmStreamService](wire[LagomhelmStreamServiceImpl])

  // Bind the LagomhelmService client
  lazy val lagomhelmService = serviceClient.implement[LagomhelmService]
}
