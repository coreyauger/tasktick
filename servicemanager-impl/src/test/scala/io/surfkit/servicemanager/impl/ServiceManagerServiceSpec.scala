package io.surfkit.servicemanager.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import io.surfkit.servicemanager.api._

class ServiceManagerServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new LagomhelmApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[ServiceManagerService]

  var someExistingProjectId = UUID.randomUUID()

  override protected def afterAll() = server.stop()

  "ServiceManager service" should {

    "return a known project by id" in {
      client.getProject(someExistingProjectId).invoke(someExistingProjectId).map { answer =>
        answer should ===("Hello, Alice!")
      }
    }

    /*"allow responding with a custom message" in {
      for {
        _ <- client.useGreeting("Bob").invoke(GreetingMessage("Hi"))
        answer <- client.hello("Bob").invoke()
      } yield {
        answer should ===("Hi, Bob!")
      }
    }*/
  }
}
