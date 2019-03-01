package io.surfkit.gateway.api

import java.util.UUID

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json, OFormat}
import io.surfkit.servicemanager.api._

object GatewayService  {
  val TOPIC_NAME = "gateway"
}

trait GatewayService extends Service {

  //def registerClient(): ServiceCall[ClientRegistration, GeneratedIdDone]
  //def loginUser(): ServiceCall[UserLogin, UserLoginDone]
  //def refreshToken(): ServiceCall[NotUsed, TokenRefreshDone]
  def getIdentityState(): ServiceCall[NotUsed, UserStateDone]
  //def createUser(): ServiceCall[UserCreation, GeneratedIdDone]

  //def getProjects: ServiceCall[GetProjects, ProjectsList]
  def oAuthService(service: String): ServiceCall[NotUsed, Done]
  def oAuthServiceCallback(service: String, code: String, state: String): ServiceCall[NotUsed, Done]
  def stream(token: String): ServiceCall[Source[SocketEvent, NotUsed], Source[SocketEvent, NotUsed]]
  //def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("gateway")
      .withCalls(
        //restCall(Method.POST, "/api/client/registration", registerClient _),
        //restCall(Method.POST, "/api/user/login", loginUser _),
        //restCall(Method.PUT, "/api/user/token", refreshToken _),
        restCall(Method.GET, "/api/state/identity", getIdentityState _),
        //restCall(Method.POST, "/api/user", createUser _)
        //restCall(Method.GET, "/api/user/projects", getProjects _),
        restCall(Method.GET, "/api/auth/:service", oAuthService _),
        restCall(Method.GET, "/api/auth/:service/callback?code&state", oAuthServiceCallback _),
        restCall(Method.GET, "/ws/stream/:token", stream _)
      )
      /*.withTopics(
        topic(GatewayService.TOPIC_NAME, greetingsTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[GreetingMessageChanged](_.name)
          )
      )*/
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
case class GetProjects(skip: Int = 0, take: Int = 50) extends SocketApi
object GetProjects {
  implicit val format: Format[GetProjects] = Json.format[GetProjects]
}
case class ProjectsList(projects: Seq[Project]) extends SocketApi
object ProjectsList {
  implicit val format: Format[ProjectsList] = Json.format[ProjectsList]
}


case class SocketEvent(payload: SocketApi)
object SocketEvent {
  implicit val format: Format[SocketEvent] = Json.format[SocketEvent]
}

case class GreetingMessageChanged(name: String, message: String)
object GreetingMessageChanged {
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}






case class UserLoginDone(authToken: String, refreshToken: String)
object UserLoginDone {
  implicit val format: Format[UserLoginDone] = Json.format
}

case class TokenRefreshDone(authToken: String)
object TokenRefreshDone {
  implicit val format: Format[TokenRefreshDone] = Json.format
}

case class UserStateDone(id: String, company: String, users: scala.collection.immutable.Seq[User])
object UserStateDone {
  implicit val format: Format[UserStateDone] = Json.format
}

case class User(id: String, firstName: String, lastName: String, email: String, username: String)
object User {
  implicit val format: Format[User] = Json.format
}

case class ClientRegistration(
                               company: String,
                               firstName: String,
                               lastName: String,
                               email: String,
                               username: String,
                               password: String
                             )
object ClientRegistration {
  implicit val format: Format[ClientRegistration] = Json.format
}

case class UserCreation(
                         firstName: String,
                         lastName: String,
                         email: String,
                         username: String,
                         password: String
                       )
object UserCreation {
  implicit val format: Format[UserCreation] = Json.format
}

case class UserLogin(username: String, password: String)
object UserLogin {
  implicit val format: Format[UserLogin] = Json.format
}

case class GeneratedIdDone(id: String)
object GeneratedIdDone {
  implicit val format: Format[GeneratedIdDone] = Json.format
}

case class TokenContent(clientId: UUID, userId: UUID, username: String, isRefreshToken: Boolean = false)
object TokenContent {
  implicit val format: Format[TokenContent] = Json.format
}