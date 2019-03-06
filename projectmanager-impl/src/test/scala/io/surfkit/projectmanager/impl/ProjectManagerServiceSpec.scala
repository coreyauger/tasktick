package io.surfkit.projectmanager.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import io.surfkit.projectmanager.api._

class ProjectManagerServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new ServiceManagerApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[ServiceManagerService]

  override protected def afterAll() = server.stop()
  import ProjectEntitySpec._

  var createdId = UUID.randomUUID()

  "ServiceManager service" should {

    "add a new project" in {
      client.createProject.invoke(io.surfkit.servicemanager.api.CreateProject(
        name = tp.name,
        owner = tp.owner,
        team = tp.team,
        description = tp.description,
        imageUrl = tp.imgUrl
      )).map{ answer =>
        createdId = answer.id
        answer should === (tp.id == answer.id)
      }
    }

    "update a project with new values" in {
      client.updateProject.invoke(io.surfkit.servicemanager.api.Project(
        id = createdId,
        name = tp2.name,
        owner = tp2.owner,
        team = tp2.team,
        description = tp2.description,
        tasks = Set.empty,
        imgUrl = tp2.imgUrl
      )).map{ answer =>
        answer should === (tp2.id == createdId)
      }
    }

  }
}
