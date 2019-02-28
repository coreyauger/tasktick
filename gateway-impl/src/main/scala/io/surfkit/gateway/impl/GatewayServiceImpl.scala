package io.surfkit.gateway.impl

import akka.Done
import io.surfkit.gateway.api
import io.surfkit.gateway.api.GatewayService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.lightbend.lagom.scaladsl.api.transport.ResponseHeader
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

/**
  * Implementation of the GatewayService.
  */
class GatewayServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends GatewayService {

  lazy val cfg = ConfigFactory.load

  def oAuthService(service: String) = ServerServiceCall { (requestHeader, _) =>
    // use the "service" to build other OAuth providers using the same endpoint
    val redirect = service.toLowerCase match {
      case "github" =>
        val client_id = cfg.getString("oauth.github.client-id")
        val scope = cfg.getString("oauth.github.scope")
        val state = cfg.getString("oauth.state")
        val redirect_uri = "http://tasktick.io:63479/gateway/api/auth/github/callback"
        s"https://github.com/login/oauth/authorize?client_id=${client_id}&scope=${scope}&state=${state}&redirect_uri=${redirect_uri}"
      case _ => throw new RuntimeException("Failed")
    }

    val responseHeader = ResponseHeader.Ok
        .withStatus(303)
        .withHeader("Location", redirect)
    Future.successful((responseHeader, Done))
  }

  override def oAuthServiceCallback(service: String, code: String, state: String) = ServerServiceCall { (requestHeader, _) =>
    println(s"oAuthServiceCallback requestHeader: ${requestHeader}")
    val responseHeader = ResponseHeader.Ok
      .withHeader("Server", "Rarg service")
    Future.successful((responseHeader, Done))
  }


  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(GatewayEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[GatewayEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }
}
