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

import scala.concurrent.Future

/**
  * Implementation of the GatewayService.
  */
class GatewayServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends GatewayService {

  def oAuthService(service: String) = ServerServiceCall { (requestHeader, _) =>
    println(s"oAuthService requestHeader: ${requestHeader}")
    val responseHeader = ResponseHeader.Ok
      .withHeader("Server", "Hello service")
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
