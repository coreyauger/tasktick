package io.surfkit.gateway.impl

import java.nio.file.{Files, Path, Paths}
import java.time.Instant
import java.util.UUID

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.surfkit.gateway.api
import io.surfkit.gateway.api._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.NegotiatedSerializer
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, MessageProtocol, RequestHeader, ResponseHeader}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtJson}

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import io.surfkit.gateway.impl.util.{JwtTokenUtil, SecurePasswordHashing}
import io.surfkit.servicemanager.api._
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.json._

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Implementation of the GatewayService.
  */
class GatewayServiceImpl(system: ActorSystem,
                         persistentEntityRegistry: PersistentEntityRegistry,
                         projectService: ServiceManagerService
                        ) extends GatewayService{

  implicit val actorSysterm = system
  implicit val materializer = ActorMaterializer()
  val ws = StandaloneAhcWSClient()

  import GatewayServiceImpl._
  import AuthenticationServiceComposition._

  val config = ConfigFactory.load()
  val wwwPath = config.getString("www.base-url")
  val indexHtml = Files.readAllBytes( Paths.get(wwwPath + "/index.html") )

  // TODO: make this NOT a def in production..
  def indexJs = Files.readAllBytes( Paths.get(wwwPath + "/bundle.js") )

  override def getPwaIndex = ServiceCall {  _  =>
    Future.successful(new String(indexHtml))
  }
  override def getPwaScript = {  _  =>
    Future.successful(new String(indexJs))
  }
  override def getPwaImage = ServerServiceCall { _ =>
    Future.successful(Array[Byte]())
  }

  override def getUser = authenticated { (tokenContent, _) =>
    ServerServiceCall { _ =>
      // NOTE: email should NOT be used in production use userId
      val ref = persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString)
      ref.ask(GetUserState())
    }
  }

  override def registerUser = ServiceCall { request =>
    // TODO request validation
    val ref = persistentEntityRegistry.refFor[UserEntity](request.email)  // NOTE: in production "email" is a bad id (bad hashing etc)
    // we are only using "email" to avoid having a Read side at this point.
    ref.ask(
      CreateUser(
        firstName = request.firstName,
        lastName = request.lastName,
        email = request.email,
        password = request.password
      )
    )
  }

  override def loginUser = ServiceCall { request =>
    val ref = persistentEntityRegistry.refFor[UserEntity](request.email)
    def passwordMatches(providedPassword: String, storedHashedPassword: String) = SecurePasswordHashing.validatePassword(providedPassword, storedHashedPassword)
    for {
      resp <- ref.ask(GetUserState())
      token = Seq(resp.user).find(user => passwordMatches(request.password, user.hashedPassword))
        .map(user => TokenContent(userId = user.id, email = user.email))
        .map(tokenContent => JwtTokenUtil.generateTokens(tokenContent))
        .getOrElse(throw Forbidden("Username and password combination not found"))
    } yield {
        UserLoginDone(token.authToken, token.refreshToken.getOrElse(throw new IllegalStateException("Refresh token missing")))
      }
  }

  override def refreshToken = authenticatedWithRefreshToken { tokenContent =>
    ServerServiceCall { _ =>
      val token = JwtTokenUtil.generateAuthTokenOnly(tokenContent)
      Future.successful(TokenRefreshDone(token.authToken))
    }
  }





  override def getProjects(skip: Int = 0, take: Int = 25) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ _ =>
      val ref = persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString)
      ref.ask(GetUserProjects(skip, take))
    }
  }
  override def getProject(id: UUID) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ add =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(id))   // check the user has access to this project
        project <- projectService.getProject(projRef.id).invoke
      }yield ProjectList(Seq(project))
    }
  }

  override def createProject = authenticated { (tokenContent, _) =>
    ServerServiceCall{ project =>
      for{
        proj <- projectService.createProject.invoke(project)
        refAdded <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(AddProjectRef(ProjectRef(proj.id, proj.name)))
      }yield refAdded.ref
    }
  }

  override def updateProject = authenticated { (tokenContent, _) =>
    ServerServiceCall{ update =>
      for{
        proj <- projectService.updateProject.invoke(update.project)
        // TODO: check if the name was modified and add update the ref in the UserEntity
      }yield ProjectList(Seq(proj))
    }
  }

  /*override def archiveProject(id: UUID)= authenticated { (tokenContent, _) =>
    ServerServiceCall{ project =>
      val ref = persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString)
      for{
        proj <- projectService.pr.invoke(project)
      }yield proj
    }
  }*/

  override def addProjectTask(project: UUID)= authenticated { (tokenContent, _) =>
    ServerServiceCall{ add =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(project))   // check the user has access to this project
        task <- projectService.addTask(projRef.id).invoke(add)
      }yield TaskList(Seq(task))
    }
  }

  override def updateProjectTask(project: UUID, task: UUID) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ add =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(project))   // check the user has access to this project
        task <- projectService.updateTask(projRef.id).invoke(add)
      }yield TaskList(Seq(task))
    }
  }

  override def deleteProjectTask(project: UUID, task: UUID) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ _ =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(project))   // check the user has access to this project
        _ <- projectService.deleteTask(projRef.id, task).invoke()
      }yield Deleted(task)
    }
  }

  override def addNote(project: UUID, task: UUID) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ add =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(project))   // check the user has access to this project
        note <- projectService.addTaskNote(projRef.id, task).invoke(add)
      }yield NoteList(Seq(note))
    }
  }

  override def deleteNote(project: UUID, task: UUID, note: UUID) = authenticated { (tokenContent, _) =>
    ServerServiceCall{ _ =>
      for{
        projRef <- persistentEntityRegistry.refFor[UserEntity](tokenContent.email.toString).ask(GetUserProject(project))   // check the user has access to this project
        _ <- projectService.deleteTaskNote(projRef.id, task, note).invoke()
      }yield Deleted(note)
    }
  }



  override def oAuthService(service: String) = ServerServiceCall { (requestHeader, _) =>
    // use the "service" to build other OAuth providers using the same endpoint
    val oAuth = OAuthApplication(service)
    val redirect = s"${oAuth.auth_url}?client_id=${oAuth.client_id}&scope=${oAuth.scope}&state=${oAuth.state}&redirect_uri=${oAuth.redirect_uri}"
    val responseHeader = ResponseHeader.Ok
        .withStatus(303)
        .withHeader("Location", redirect)
    Future.successful((responseHeader, Done))
  }

  override def oAuthServiceCallback(service: String, code: String, state: String) = ServerServiceCall { (requestHeader, _) =>
    val oAuth = OAuthApplication(service)
    if( state != oAuth.state )
      throw new RuntimeException(s"Service: ${service} returned bad state value for oAuth")
    ws.url(oAuth.token_url).post( oAuth.toJson(code) ).map{ response =>
      val responseHeader = ResponseHeader.Ok
      println(s"\n\nAccess Token: ${response.body}\n\n")
      (responseHeader, Done)
    }
  }

  override def stream(token: String) =
    ServerServiceCall { src =>

      val withAuthHeader = RequestHeader.Default.withHeader("Authorization", s"Bearer ${token}")

      val tokenContent = authToken(token)
      //println(s"New Connection !!! ${tokenContent}")
      //println(s"withAuthHeader: ${withAuthHeader}")
      //val source = Source.tick(100 milliseconds, 2 seconds, "tick").mapAsync(4){ _ =>
      val source = src.mapAsync(4) {
        case SocketEvent(_: GetUser) => getUser.invokeWithHeaders(withAuthHeader, NotUsed).map(x => SocketEvent(UserList( Seq(x._2.user) )))
        case SocketEvent(x: GetProjects) => getProjects(x.skip, x.take).invokeWithHeaders(withAuthHeader, NotUsed).map(x => SocketEvent(x._2))
        case SocketEvent(x: GetProject) => getProject(x.id).invokeWithHeaders(withAuthHeader, NotUsed).map(x => SocketEvent(x._2))
        case SocketEvent(x: NewProject) => createProject.invokeWithHeaders(withAuthHeader, CreateProject(
                                              name = x.name,
                                              owner = UUID.fromString(tokenContent.userId),
                                              team  = x.team,
                                              description = x.description,
                                              imageUrl = x.imageUrl
                                            )).map(x => SocketEvent(x._2))
        case SocketEvent(x: EditProject) => updateProject.invokeWithHeaders(withAuthHeader, UpdateProject(
                                              project = x.project
                                            )).map(x => SocketEvent(x._2))
          // FIXME: ...
        case SocketEvent(x: AddTask) => addProjectTask(x.project).invokeWithHeaders(withAuthHeader, x).map(x => SocketEvent(x._2))
        case SocketEvent(x: UpdateTask) => updateProjectTask(x.project, x.task.id).invokeWithHeaders(withAuthHeader, x).map(x => SocketEvent(x._2))
        case SocketEvent(x: DeleteTask) => deleteProjectTask(x.project, x.task).invokeWithHeaders(withAuthHeader, NotUsed).map(x => SocketEvent(x._2))
        case SocketEvent(x: AddNote) => addNote(x.project, x.task).invokeWithHeaders(withAuthHeader, x).map(x => SocketEvent(x._2))
        case SocketEvent(x: DeleteNote) => deleteNote(x.project, x.task, x.note).invokeWithHeaders(withAuthHeader, NotUsed).map(x => SocketEvent(x._2))
        case SocketEvent(_: HeartBeat) => Future.successful( SocketEvent(HeartBeat(Instant.now)) )
        case x =>
          println(s"Unknown message: ${x}")
          Future.successful(SocketEvent(Test("pong")))
      }.mapMaterializedValue(_ => NotUsed)
      Future.successful(source)
    }

}

object GatewayServiceImpl{

  lazy val cfg = ConfigFactory.load

  import play.api.libs.json._

  sealed trait OAuthApplication{
    def auth_url: String
    def token_url: String
    def client_id: String
    def client_secret: String
    def scope: String
    def redirect_uri: String
    def state: String

    def toJson(code: String) = Json.obj(
      "client_id" -> client_id,
      "client_secret" -> client_secret,
      "redirect_uri" -> redirect_uri,
      "state" -> state,
      "code" -> code
    )
  }
  object OAuthApplication{
    def apply(service: String) = {
      val state = cfg.getString("oauth.state")
      service.toLowerCase match {
        case "github" =>
          val auth_url = cfg.getString("oauth.github.auth-url")
          val token_url = cfg.getString("oauth.github.token-url")
          val client_id = cfg.getString("oauth.github.client-id")
          val scope = cfg.getString("oauth.github.scope")
          val client_secret = cfg.getString("oauth.github.client-secret")
          val redirect_uri = cfg.getString("oauth.github.redirect-url")
          GithubApplication(auth_url, token_url, client_id, scope, client_secret, redirect_uri, state)
        //case "facebook" => ...
        case _ => throw new RuntimeException("Unsupported OAuth service")
      }
    }
    case class GithubApplication(auth_url: String, token_url: String, client_id: String, scope: String, client_secret: String, redirect_uri: String, state: String) extends OAuthApplication
    //case class FacebookApplication(client_id: String, scope: String, client_secret: String, redirect_uri: String) extends OAuthApplication
  }
}


object AuthenticationServiceComposition {
  val secret = ConfigFactory.load().getString("jwt.secret")
  val algorithm = JwtAlgorithm.HS512



  def authenticated[Request, Response](serviceCall: (TokenContent, String) => ServerServiceCall[Request, Response]) =
    ServerServiceCall.compose { requestHeader =>
      val tokenContent = extractTokenContent(requestHeader).filter(tokenContent => isAuthToken(tokenContent))
      val authToken = extractTokenHeader(requestHeader)
      tokenContent match {
        case Some(tokenContent) => serviceCall(tokenContent, authToken.getOrElse(""))
        case _ => throw Forbidden("Authorization token is invalid")
      }
    }

  def authToken(token: String) = {
    if(validateToken(token))
      decodeToken(token)
    else
      throw Forbidden("Authorization token is invalid")
  }


  def authenticatedWithRefreshToken[Request, Response](serviceCall: TokenContent => ServerServiceCall[Request, Response]) =
    ServerServiceCall.compose { requestHeader =>
      val tokenContent = extractTokenContent(requestHeader).filter(tokenContent => isRefreshToken(tokenContent))
      tokenContent match {
        case Some(tokenContent) => serviceCall(tokenContent)
        case _ => throw Forbidden("Refresh token is invalid")
      }
    }

  private def extractTokenHeader(requestHeader: RequestHeader) =
    requestHeader.getHeader("Authorization").map(header => sanitizeToken(header))

  private def extractTokenContent[Response, Request](requestHeader: RequestHeader) =
    extractTokenHeader(requestHeader)
      .filter(rawToken => validateToken(rawToken))
      .map(rawToken => decodeToken(rawToken))

  private def sanitizeToken(header: String) = header.replaceFirst("Bearer ", "")
  private def validateToken(token: String) = Jwt.isValid(token, secret, Seq(algorithm))
  private def decodeToken(token: String) = {
    val jsonTokenContent = JwtJson.decode(token, secret, Seq(algorithm))
    jsonTokenContent match {
      case Success(json) => Json.parse(json.content).as[TokenContent]
      case Failure(_) => throw Forbidden(s"Unable to decode token")
    }
  }

  private def isAuthToken(tokenContent: TokenContent) = !tokenContent.isRefreshToken
  private def isRefreshToken(tokenContent: TokenContent) = tokenContent.isRefreshToken

}