package io.surfkit.lagomhelmstream.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

/**
  * The LagomHelm stream interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the LagomhelmStream service.
  */
trait LagomhelmStreamService extends Service {

  def stream: ServiceCall[Source[String, NotUsed], Source[String, NotUsed]]

  override final def descriptor = {
    import Service._

    named("lagomhelm-stream")
      .withCalls(
        namedCall("stream", stream)
      ).withAutoAcl(true)
  }
}

