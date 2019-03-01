package io.surfkit.gateway.api

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json, OFormat}

object GatewayService  {
  val TOPIC_NAME = "gateway"
}

trait GatewayService extends Service {

  def oAuthService(service: String): ServiceCall[NotUsed, Done]

  def oAuthServiceCallback(service: String, code: String, state: String): ServiceCall[NotUsed, Done]

  def stream(token: String): ServiceCall[Source[SocketEvent, NotUsed], Source[SocketEvent, NotUsed]]

  def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("gateway")
      .withCalls(
        restCall(Method.GET, "/api/auth/:service", oAuthService _),
        restCall(Method.GET, "/api/auth/:service/callback?code&state", oAuthServiceCallback _),
        restCall(Method.GET, "/ws/stream/:token", stream _)
      )
      .withTopics(
        topic(GatewayService.TOPIC_NAME, greetingsTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[GreetingMessageChanged](_.name)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

sealed trait SocketApi
object SocketApi{
  implicit val format: OFormat[SocketApi] = Json.format[SocketApi]
}
case class Test(msg: String) extends SocketApi
object Test {
  implicit val format: Format[Test] = Json.format[Test]
}

case class SocketEvent(payload: SocketApi)
object SocketEvent {
  implicit val format: Format[SocketEvent] = Json.format[SocketEvent]
}

case class GreetingMessageChanged(name: String, message: String)
object GreetingMessageChanged {
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}
