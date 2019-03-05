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

  def getProject(id: UUID): ServiceCall[NotUsed, Project]
  def createProject: ServiceCall[CreateProject, Project]
  def updateProject: ServiceCall[Project, Project]
  def addTask(project: UUID): ServiceCall[AddTask, Task]
  def updateTask(project: UUID): ServiceCall[UpdateTask, Task]
  def deleteTask(project: UUID, task: UUID): ServiceCall[NotUsed, Done]
  def addTaskNote(project: UUID, task: UUID): ServiceCall[AddNote, Note]
  def deleteTaskNote(project: UUID, task: UUID, note: UUID): ServiceCall[NotUsed, Done]


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

case class CreateProject(name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String] = None)
object CreateProject{ implicit val format: Format[CreateProject] = Json.format[CreateProject] }

case class UpdateProject(project: Project)
object UpdateProject{implicit val format: Format[UpdateProject] = Json.format[UpdateProject]}

case class AddTask(project: UUID, name: String, description: String, section: String, parent: Option[UUID] = None)
object AddTask{implicit val format: Format[AddTask] = Json.format[AddTask] }

case class UpdateTask(project: UUID, task: Task)
object UpdateTask{implicit val format: Format[UpdateTask] = Json.format[UpdateTask]}

case class AddNote(project: UUID, task: UUID, user: UUID, note: String)
object AddNote{implicit val format: Format[AddNote] = Json.format[AddNote] }

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
object Task {implicit val format: Format[Task] = Json.format }

case class Note(id: UUID, user: UUID, note: String, date: Instant)
object Note {implicit val format: Format[Note] = Json.format}

case class Project(id: UUID,
                   name: String,
                   owner: UUID,
                   team: UUID,
                   description: String,
                   imgUrl: Option[String],
                   tasks: Map[String,Task])
object Project {implicit val format: Format[Project] = Json.format[Project]}

case class ProjectUpdated(project: Project)
object ProjectUpdated {
  implicit val format: Format[ProjectUpdated] = Json.format[ProjectUpdated]
}
