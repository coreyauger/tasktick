package io.surfkit.gatewaystream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import io.surfkit.gatewaystream.api.GatewayStreamService
import io.surfkit.gateway.api.GatewayService
import com.softwaremill.macwire._

class GatewayStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new GatewayStreamApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new GatewayStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[GatewayStreamService])
}

abstract class GatewayStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[GatewayStreamService](wire[GatewayStreamServiceImpl])

  // Bind the GatewayService client
  lazy val gatewayService: GatewayService = serviceClient.implement[GatewayService]
}
