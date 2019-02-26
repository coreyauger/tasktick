package io.surfkit.lagomhelmstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import io.surfkit.lagomhelmstream.api.LagomhelmStreamService
import io.surfkit.lagomhelm.api.LagomhelmService

import scala.concurrent.Future

/**
  * Implementation of the LagomhelmStreamService.
  */
class LagomhelmStreamServiceImpl(lagomhelmService: LagomhelmService) extends LagomhelmStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(lagomhelmService.hello(_).invoke()))
  }
}
