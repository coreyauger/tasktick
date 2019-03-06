package io.surfkit.gateway.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, TransportException}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import io.surfkit.gateway.api._
import io.surfkit.projectmanager.api.CreateProject

import scala.util.Try

class GatewayServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new GatewayApplication(ctx) with LocalServiceLocator
  }

  val client: GatewayService = server.serviceClient.implement[GatewayService]

  override protected def afterAll(): Unit = server.stop()

  val testUserEmail = "testing@example.com"
  val testUserPass = "ThisIs@TestPass"

  //def authedRequest

  "gateway service auth" should {

    "register a user" in {
      client.registerUser.invoke(RegisterUser(
        firstName = "fname",
        lastName = "lname",
        email = testUserEmail,
        password = testUserPass
      )).map { answer =>
        assert( Try(UUID.fromString(answer.id)).isSuccess )
      }
    }

    "login a user" in {
      for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, testUserPass))
      } yield {
        assert(!answer.authToken.isEmpty)
      }
    }

    "fail to login a user with wrong pass" in {
      (for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, "ThisISWRONG!!!"))
      } yield {
        assert(!answer.authToken.isEmpty)
      }).recover{
        case _:Forbidden =>  assert(true)
        case x => assert(false, s"Should have got another exception type instead of: ${x}")
      }
    }

    "fail to login a user that does not exist" in {
      (for {
        answer <- client.loginUser.invoke(UserLogin("not.registered@example.com", testUserPass))
      } yield {
        assert(!answer.authToken.isEmpty)
      }).recover{
        case _:InvalidCommandException =>  assert(true)
        case _:TransportException =>  assert(true)
        case x =>  assert(false, s"Should have got another exception type instead of: ${x}")
      }
    }

    "Get the identity from auth token"in {
      for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, testUserPass))
        user <- client.getUser.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${answer.authToken}" )
        ).invoke()
      } yield {
        println(s"user: ${user}")
        assert(user.user.email == testUserEmail)
      }
    }

    "Fail to get the identity from a bad auth token"in {
      (for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, testUserPass))
        user <- client.getUser.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${answer.authToken}XXXX" )
        ).invoke()
      } yield {
        println(s"user: ${user}")
        assert(user.user.email == testUserEmail)
      }).recover{
        case _:Forbidden =>  assert(true)
        case x => assert(false, s"Should have got another exception type instead of: ${x}")
      }
    }

    "Allow an auth token to refresh"in {
      for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, testUserPass))
        newAuth <- client.refreshToken.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${answer.refreshToken}" )
        ).invoke()
        user <- client.getUser.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${newAuth.authToken}" )
        ).invoke()
      } yield {
        println(s"user: ${user}")
        assert(user.user.email == testUserEmail)
      }
    }

  }

/*
  val testProjectName = "Test project"

  "gateway service projects" should {
    "Allow a user to create a project" in {
      (for {
        answer <- client.loginUser.invoke(UserLogin(testUserEmail, testUserPass))
        user <- client.getIdentityState.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${answer.authToken}" )
        ).invoke()
        project <- client.createProject.handleRequestHeader(x =>
          x.withHeader("Authorization", s"Bearer ${answer.authToken}" )
        ).invoke(CreateProject(
          name = testProjectName,
          owner = UUID.fromString(user.user.id),
          team = UUID.randomUUID(),
          description = "testing",
          imageUrl = None
        ))
      } yield {
        println(s"project: ${project}")
        assert(project.name == testProjectName)
      })
    }
  }*/

}
