package io.surfkit.gateway.api

import java.time.Instant
import java.util.UUID

import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.deser.{MessageSerializer, StrictMessageSerializer}
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, Method}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json, OFormat}
import io.surfkit.projectmanager.api._

object GatewayService  {
  val TOPIC_NAME = "gateway"
}

trait GatewayService extends Service {

  def getPwaIndex: ServiceCall[NotUsed, String]
  def getPwaScript: ServiceCall[NotUsed, String]
  //def getPwaStatic = ServiceCall[NotUsed, Any]
  def getPwaImage(img:String): ServiceCall[NotUsed, Array[Byte]]

  def registerUser: ServiceCall[RegisterUser, GeneratedIdDone]
  def loginUser: ServiceCall[UserLogin, UserLoginDone]
  def refreshToken: ServiceCall[NotUsed, TokenRefreshDone]
  def getUser: ServiceCall[NotUsed, UserStateDone]

  def getProjects(skip: Int = 0, take: Int = 25): ServiceCall[NotUsed, ProjectRefList]
  def getProject(project: UUID): ServiceCall[NotUsed, ProjectList]
  def createProject: ServiceCall[CreateProject, ProjectRef]
  def updateProject: ServiceCall[UpdateProject, ProjectList]
  //def archiveProject(id: UUID): ServiceCall[NotUsed, Project]
  def addProjectTask(project: UUID): ServiceCall[AddTask, TaskList]
  def updateProjectTask(project: UUID, task: UUID): ServiceCall[UpdateTask, TaskList]
  def deleteProjectTask(project: UUID, task: UUID): ServiceCall[NotUsed, Deleted]
  def addNote(project: UUID, task: UUID): ServiceCall[AddNote, NoteList]
  def deleteNote(project: UUID, task: UUID, note: UUID): ServiceCall[NotUsed, Deleted]

  def oAuthService(service: String): ServiceCall[NotUsed, Done]
  def oAuthServiceCallback(service: String, code: String, state: String): ServiceCall[NotUsed, Done]
  def stream(token: String): ServiceCall[Source[SocketEvent, NotUsed], Source[SocketEvent, NotUsed]]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("gateway")
      .withCalls(
        //pathCall("/", getPwaIndex _)(
          //MessageSerializer.NotUsedMessageSerializer,
          //new ContentTypeSerializer()),
        pathCall("/index.html", getPwaIndex _)(
          MessageSerializer.NotUsedMessageSerializer,
          new ContentTypeSerializer()),
        pathCall("/p/:rest", getPwaIndex _)(
          MessageSerializer.NotUsedMessageSerializer,
          new ContentTypeSerializer()),
        pathCall("/p/project/:rest", getPwaIndex _)(
          MessageSerializer.NotUsedMessageSerializer,
          new ContentTypeSerializer()),
        pathCall("/bundle.js", getPwaScript _)(
          MessageSerializer.NotUsedMessageSerializer,
          new ContentTypeSerializer("text/javascript")),
        //pathCall("/static/:rest", getPwaStatic),
        pathCall("/img/:rest", getPwaImage _)(
          MessageSerializer.NotUsedMessageSerializer,
          new ImageSerializer("image/png")),

        restCall(Method.POST, "/api/user/register", registerUser _),
        restCall(Method.POST, "/api/user/login", loginUser _),
        restCall(Method.PUT, "/api/user/token", refreshToken _),
        restCall(Method.GET, "/api/user", getUser _),

        restCall(Method.GET, "/api/user/projects", getProjects _),
        restCall(Method.GET, "/api/user/project/:id", getProject _),
        restCall(Method.PUT, "/api/user/project/create", createProject _),
        restCall(Method.POST, "/api/user/project/update", updateProject _),
        //restCall(Method.DELETE, "/api/user/project/:id/archive", archiveProject _),
        restCall(Method.PUT, "/api/user/project/:project/task/add", addProjectTask _),
        restCall(Method.POST, "/api/user/project/:project/task/:task", updateProjectTask _),
        restCall(Method.DELETE, "/api/user/project/:project/task/:task", deleteProjectTask _),
        restCall(Method.PUT, "/api/user/project/:project/task/:id/note/add", addNote _),
        restCall(Method.DELETE, "/api/user/project/:project/task/:task/note/:note", deleteNote _),

        restCall(Method.GET, "/api/auth/:service", oAuthService _),
        restCall(Method.GET, "/api/auth/:service/callback?code&state", oAuthServiceCallback _),
        restCall(Method.GET, "/ws/stream/:token", stream _)
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

class ContentTypeSerializer(contentType: String = "text/html") extends StrictMessageSerializer[String] {
  final private val serializer = {
    new NegotiatedSerializer[String, ByteString]() {
      override val protocol = MessageProtocol(Some(contentType), Some("utf-8"))
      def serialize(s: String) = ByteString.fromString(s, "utf-8")
    }
  }
  final private val deserializer = {
    new NegotiatedDeserializer[String, ByteString] {
      override def deserialize(bytes: ByteString) = bytes.toString()
    }
  }

  override def serializerForRequest = serializer
  override def deserializer(protocol: MessageProtocol) = deserializer
  override def serializerForResponse(acceptedMessageProtocols: scala.collection.immutable.Seq[MessageProtocol]) = serializer
}

class ImageSerializer(contentType: String = "image/png") extends StrictMessageSerializer[Array[Byte]] {
  final private val serializer = {
    new NegotiatedSerializer[Array[Byte], ByteString]() {
      override val protocol = MessageProtocol(Some(contentType))
      def serialize(s: Array[Byte]) = ByteString.fromArray( s )
    }
  }
  final private val deserializer = {
    new NegotiatedDeserializer[Array[Byte], ByteString] {
      override def deserialize(bytes: ByteString) = bytes.toArray[Byte]
    }
  }

  override def serializerForRequest = serializer
  override def deserializer(protocol: MessageProtocol) = deserializer
  override def serializerForResponse(acceptedMessageProtocols: scala.collection.immutable.Seq[MessageProtocol]) = serializer
}


/**
  *  WebSocket API
  */

sealed trait SocketApi
object SocketApi{ implicit val format: OFormat[SocketApi] = Json.format[SocketApi] }

case class Test(msg: String) extends SocketApi
object Test { implicit val format: Format[Test] = Json.format[Test] }

case class ProjectRef(id: UUID, name: String) extends SocketApi
object ProjectRef { implicit val format: Format[ProjectRef] = Json.format[ProjectRef] }

case class ProjectRefList(projects: Seq[ProjectRef]) extends SocketApi
object ProjectRefList { implicit val format: Format[ProjectRefList] = Json.format[ProjectRefList] }

case class GetProjects(skip: Int = 0, take: Int = 25) extends SocketApi
object GetProjects { implicit val format: Format[GetProjects] = Json.format[GetProjects] }

case class GetProject(id: UUID) extends SocketApi
object GetProject { implicit val format: Format[GetProject] = Json.format[GetProject] }

case class NewProject(name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String] = None) extends SocketApi
object NewProject{ implicit val format: Format[NewProject] = Json.format[NewProject] }

case class EditProject(project: Project) extends SocketApi
object EditProject{implicit val format: Format[EditProject] = Json.format[EditProject]}

case class ProjectList(projects: Seq[Project]) extends SocketApi
object ProjectList { implicit val format: Format[ProjectList] = Json.format[ProjectList] }

case class NewTask(project: UUID, name: String, description: String, section: String, parent: Option[UUID] = None) extends SocketApi
object NewTask { implicit val format: Format[NewTask] = Json.format[NewTask] }

case class EditTask(task: Task) extends SocketApi
object EditTask{implicit val format: Format[EditTask] = Json.format[EditTask]}

case class TaskList(tasks: Seq[Task]) extends SocketApi
object TaskList { implicit val format: Format[TaskList] = Json.format[TaskList] }

case class NoteList(notes: Seq[Note]) extends SocketApi
object NoteList { implicit val format: Format[NoteList] = Json.format[NoteList] }

case class SocketEvent(payload: SocketApi)
object SocketEvent { implicit val format: Format[SocketEvent] = Json.format[SocketEvent] }

case class HeartBeat(ts: Instant) extends SocketApi
object HeartBeat { implicit val format: Format[HeartBeat] = Json.format[HeartBeat] }

case class DeleteTask(project: UUID, task: UUID)
object DeleteTask { implicit val format: Format[DeleteTask] = Json.format[DeleteTask] }

case class DeleteNote(project: UUID, task: UUID, note: UUID)
object DeleteNote { implicit val format: Format[DeleteNote] = Json.format[DeleteNote] }

case class UserList(users: Seq[User]) extends SocketApi
object UserList { implicit val format: Format[UserList] = Json.format[UserList] }

case class Deleted(id: UUID) extends SocketApi
object Deleted { implicit val format: Format[Deleted] = Json.format[Deleted] }

case class GetUser(ts: Instant) extends SocketApi
object GetUser { implicit val format: Format[GetUser] = Json.format[GetUser] }

case class NewNote(project: UUID, task: UUID, note: String) extends SocketApi
object NewNote { implicit val format: Format[NewNote] = Json.format[NewNote] }

/**
  *  Identity
  */

case class UserLoginDone(authToken: String, refreshToken: String)
object UserLoginDone { implicit val format: Format[UserLoginDone] = Json.format}

case class TokenRefreshDone(authToken: String)
object TokenRefreshDone { implicit val format: Format[TokenRefreshDone] = Json.format }

case class UserStateDone(user: User)
object UserStateDone { implicit val format: Format[UserStateDone] = Json.format }

case class User(id: String, firstName: String, lastName: String, email: String, hashedPassword: String)
object User { implicit val format: Format[User] = Json.format }

case class createUser(
             firstName: String,
             lastName: String,
             email: String,
             password: String)
object createUser { implicit val format: Format[createUser] = Json.format }

case class RegisterUser(
             firstName: String,
             lastName: String,
             email: String,
             password: String)
object RegisterUser { implicit val format: Format[RegisterUser] = Json.format}

case class UserLogin(email: String, password: String)
object UserLogin { implicit val format: Format[UserLogin] = Json.format}

case class GeneratedIdDone(id: String)
object GeneratedIdDone { implicit val format: Format[GeneratedIdDone] = Json.format }

case class TokenContent(userId: String, email: String, isRefreshToken: Boolean = false)
object TokenContent {implicit val format: Format[TokenContent] = Json.format }