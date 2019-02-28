package io.surfkit.servicemanager.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class ServiceManagerEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("ServiceManagerEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(ServiceManagerSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private def withTestDriver(block: PersistentEntityTestDriver[ProjectCommand[_], ProjectEvent, ProjectState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new ProjectEntity, UUID.randomUUID().toString)
    block(driver)
    driver.getAllIssues should have size 0
  }

  val testUserId = UUID.randomUUID()
  val testTeamId = UUID.randomUUID()

  lazy val tp = Project(
    id = UUID.randomUUID(),
    name = "Test Name",
    owner = testUserId,
    team = testTeamId,
    description = "This is a a description of the project",
    imgUrl = Some("http://some-image.com/image.png"),
    tasks = Map.empty[String,Task]
  )

  "Project entity" should {

    "create a new project" in withTestDriver { driver =>
      val outcome = driver.run(CreateProject(
        name = tp.name,
        owner = tp.owner,
        team = tp.team,
        description = tp.description,
        imageUrl = tp.imgUrl
      ))
      val ev = outcome.events.head.asInstanceOf[ProjectCreated]
      val cmpTo = tp.copy(id = ev.project.id)
      assert(ev.project == cmpTo)
    }

   /* "allow updating the greeting message" in withTestDriver { driver =>
      val outcome1 = driver.run(UseGreetingMessage("Hi"))
      outcome1.events should contain only GreetingMessageChanged("Hi")
      val outcome2 = driver.run(Hello("Alice"))
      outcome2.replies should contain only "Hi, Alice!"
    }*/

  }
}
