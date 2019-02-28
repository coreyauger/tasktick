package io.surfkit.gateway.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object GatewayService  {
  val TOPIC_NAME = "change-me"
}

/**
  * The gateway service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the GatewayService.
  */
trait GatewayService extends Service {

  def oAuthService(service: String): ServiceCall[NotUsed, Done]

  def oAuthServiceCallback(service: String, code: String, state: String): ServiceCall[NotUsed, Done]

  /**
    * This gets published to Kafka.
    */
  def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("gateway")
      .withCalls(
        restCall(Method.GET, "/api/auth/:service", oAuthService _),
        restCall(Method.GET, "/api/auth/:service/callback?code&state", oAuthServiceCallback _)
      )
      .withTopics(
        topic(GatewayService.TOPIC_NAME, greetingsTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[GreetingMessageChanged](_.name)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

case class OAuthGetToken(service: String, code: String, state: String)
object OAuthGetToken {
  implicit val format: Format[OAuthGetToken] = Json.format[OAuthGetToken]
}


case class GreetingMessageChanged(name: String, message: String)
object GreetingMessageChanged {
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}
