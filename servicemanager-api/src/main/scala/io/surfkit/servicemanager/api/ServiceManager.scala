package io.surfkit.servicemanager.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import java.util.UUID

object ServiceManagerService  {
  val TOPIC_NAME = "greetings"
}

/**
  * The ServiceManager service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ServiceManagerService.
  */
trait ServiceManagerService extends Service {

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  //def hello(id: String): ServiceCall[NotUsed, String]
  def getProject(id: UUID): ServiceCall[UUID, Project]

  /**
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
    * "Hi"}' http://localhost:9000/api/hello/Alice
    */
  //def useGreeting(id: String): ServiceCall[GreetingMessage, Done]


  /**
    * This gets published to Kafka.
    */
  def projectsTopic(): Topic[ProjectUpdated]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("projects")
      .withCalls(
        pathCall("/api/project/:id", getProject _),
        //pathCall("/api/hello/:id", useGreeting _)
      )
      .withTopics(
        topic(ServiceManagerService.TOPIC_NAME, projectsTopic)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[ProjectUpdated](_.project.id.toString)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

/**
  * The greeting message class.
  */

case class Project(id: UUID, name: String)
object Project {
  implicit val format: Format[Project] = Json.format[Project]
}

case class GreetingMessage(message: String)
object GreetingMessage {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessage] = Json.format[GreetingMessage]
}



/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class ProjectUpdated(project: Project)
object ProjectUpdated {
  implicit val format: Format[ProjectUpdated] = Json.format[ProjectUpdated]
}
