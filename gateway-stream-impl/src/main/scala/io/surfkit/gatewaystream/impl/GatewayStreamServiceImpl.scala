package io.surfkit.gatewaystream.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import io.surfkit.gatewaystream.api.GatewayStreamService
import io.surfkit.gateway.api.GatewayService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implementation of the GatewayStreamService.
  */
class GatewayStreamServiceImpl(gatewayService: GatewayService) extends GatewayStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(gatewayService.oAuthService(_).invoke().map(_ => "fix me")))
  }
}
