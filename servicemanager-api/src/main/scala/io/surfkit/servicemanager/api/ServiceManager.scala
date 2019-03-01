package io.surfkit.servicemanager.api

import java.time.Instant

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import java.util.UUID

import com.lightbend.lagom.scaladsl.api.transport.Method

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

  def createProject: ServiceCall[CreateProject, Project]

  def updateProject: ServiceCall[Project, Project]

  def addTask(project: UUID): ServiceCall[AddTask, Task]

  def updateTask(project: UUID): ServiceCall[Task, Task]

  def deleteTask(project: UUID, task: UUID): ServiceCall[NotUsed, Done]

  def addTaskNote(project: UUID, task: UUID): ServiceCall[AddNote, Note]

  def deleteTaskNote(project: UUID, task: UUID, note: UUID): ServiceCall[NotUsed, Done]

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
        restCall(Method.GET, "/api/project/:id", getProject _),
        restCall(Method.PUT, "/api/project/add", createProject _),
        restCall(Method.POST, "/api/project/add", updateProject _),
        restCall(Method.PUT, "/api/project/:project/task", addTask _),
        restCall(Method.POST, "/api/project/:project/task", updateTask _),
        restCall(Method.DELETE, "/api/project/:project/task/:task", deleteTask _),
        restCall(Method.PUT, "/api/project/:project/task/:task/note", addTaskNote _),
        restCall(Method.DELETE, "/api/project/:project/task/:task/note/:note", deleteTaskNote _)
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

case class CreateProject(name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String] = None)
object CreateProject{
  implicit val format: Format[CreateProject] = Json.format[CreateProject]
}

case class AddTask(name: String,
                   description: String,
                   section: String,
                   parent: Option[UUID] = None)
object AddTask{
  implicit val format: Format[AddTask] = Json.format[AddTask]
}

case class AddNote(user: UUID, note: String)
object AddNote{
  implicit val format: Format[AddNote] = Json.format[AddNote]
}

case class Task(
                 id: UUID,
                 name: String,
                 description: String,
                 done: Boolean,
                 assigned: Option[UUID],
                 startDate: Option[Instant],
                 endDate: Option[Instant],
                 lastUpdated: Instant,
                 section: String,
                 parent: Option[UUID] = None,
                 notes: Seq[Note] = Seq.empty[Note]
               )
object Task {
  implicit val format: Format[Task] = Json.format
}
case class Note(id: UUID, user: UUID, note: String, date: Instant)
object Note {
  implicit val format: Format[Note] = Json.format
}

case class Project(id: UUID,
                   name: String,
                   owner: UUID,
                   team: UUID,
                   description: String,
                   imgUrl: Option[String],
                   tasks: Map[String,Task])
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
