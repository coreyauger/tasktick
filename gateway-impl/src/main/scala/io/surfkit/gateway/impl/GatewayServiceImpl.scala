package io.surfkit.gateway.impl

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import io.surfkit.gateway.api
import io.surfkit.gateway.api._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.lightbend.lagom.scaladsl.api.transport.ResponseHeader
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, RequestHeader}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtJson}

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import io.surfkit.gateway.impl.util.JwtTokenUtil
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.json._

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Implementation of the GatewayService.
  */
class GatewayServiceImpl(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry) extends GatewayService{

  implicit val actorSysterm = system
  implicit val materializer = ActorMaterializer()
  val ws = StandaloneAhcWSClient()

  import GatewayServiceImpl._
  import AuthenticationServiceComposition._


  override def getIdentityState() = authenticated { (tokenContent, _) =>
    ServerServiceCall { _ =>
      val ref = persistentEntityRegistry.refFor[UserEntity](tokenContent.clientId.toString)
      ref.ask(GetUserState())
    }
  }


  /*override def getProjects = ServiceCall{ req =>
    val ref = persistentEntityRegistry.refFor[UserEntity](userId.toString)
    req.ask(...)
  }*/

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

  // TODO: make sure auth
  override def stream(token: String) = ServiceCall { src =>
    //val source = Source.tick(100 milliseconds, 2 seconds, "tick").map { _ =>
    val source = src.mapAsync(4){
      //case x: GetProjects => getProjects.invoke(x).map(x => SocketEvent(x))
      case _ => Future.successful( SocketEvent(Test("ping")) )
    }.mapMaterializedValue(_ => NotUsed)
    Future.successful(source)
  }

  /*override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(UserEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[UserEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }*/
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
    def apply(service: String) ={
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