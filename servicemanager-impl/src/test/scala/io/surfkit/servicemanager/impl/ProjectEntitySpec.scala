package io.surfkit.servicemanager.impl

import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver.Reply
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class ProjectEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("ServiceManagerEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(ServiceManagerSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import ProjectEntitySpec._

  val testDriver = new PersistentEntityTestDriver(system, new ProjectEntity, entityId.toString)

  val taskTestDriver = new PersistentEntityTestDriver(system, new ProjectEntity, entityId2.toString)


  private def withTestDriver(block: PersistentEntityTestDriver[ProjectCommand[_], ProjectEvent, ProjectState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new ProjectEntity, entityId.toString)
    block(driver)
    driver.getAllIssues should have size 0
  }

  private def withTestDriver2(block: PersistentEntityTestDriver[ProjectCommand[_], ProjectEvent, ProjectState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new ProjectEntity, entityId2.toString)
    block(driver)
    driver.getAllIssues should have size 0
  }

  "Project entity" should {

    "should not create a project with no name" in {
      val outcome = testDriver.run(CreateProject(
        name = "",
        owner = tp.owner,
        team = tp.team,
        description = tp.description,
        imageUrl = tp.imgUrl
      ))
      outcome.sideEffects.head match{
        case Reply(_: RuntimeException) => assert(true)
        case _ => assert(false, "Should return error")
      }
      testDriver.getAllIssues should have size 1
    }

    "create a new project" in  {
      val outcome = testDriver.run(CreateProject(
        name = tp.name,
        owner = tp.owner,
        team = tp.team,
        description = tp.description,
        imageUrl = tp.imgUrl
      ))
      val ev = outcome.events.head.asInstanceOf[ProjectCreated]
      assert(ev.project == tp)
    }

    "update a project" in  {
      val outcome = testDriver.run(UpdateProject(
        name = tpUpdate.name,
        owner = tpUpdate.owner,
        team = tpUpdate.team,
        description = tpUpdate.description,
        imageUrl = tpUpdate.imgUrl
      ))
      val ev = outcome.events.head.asInstanceOf[ProjectUpdated]
      assert(ev.project == tpUpdate)
    }

    "get a project" in  {
      val outcome = testDriver.run(GetProject(entityId))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj == tpUpdate)
    }

    /* "allow updating the greeting message" in withTestDriver { driver =>
       val outcome1 = driver.run(UseGreetingMessage("Hi"))
       outcome1.events should contain only GreetingMessageChanged("Hi")
       val outcome2 = driver.run(Hello("Alice"))
       outcome2.replies should contain only "Hi, Alice!"
     }*/
  }


  "Project Entity " should {
    "Create a new Project" in{
      val outcome = taskTestDriver.run(CreateProject(
        name = tp2.name,
        owner = tp2.owner,
        team = tp2.team,
        description = tp2.description,
        imageUrl = tp2.imgUrl
      ))
      val ev = outcome.events.head.asInstanceOf[ProjectCreated]
      assert(ev.project == tp2)
    }

    "Add a Task to the project" in{
      val outcome = taskTestDriver.run(AddTask(name = task1.name, description = task1.description, section = task1.section))
      val ev = outcome.events.head.asInstanceOf[TaskAdded]
      assert(ev.task.name == task1.name && ev.task.section ==  task1.section)
    }

    "Update the task info" in{
      val outcome = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 1)
      val task = task1.copy(id = proj.tasks.head._2.id)
      val outcome2 = taskTestDriver.run(UpdateTask(
        id = task.id,
        name = task.name,
        description = task.description,
        done = task.done,
        assigned = task.assigned,
        startDate = task.startDate,
        parent = task.parent,
        endDate = task.endDate,
        section = task.section
      ))
      val now = Instant.now
      val ev = outcome2.events.head.asInstanceOf[TaskUpdated]
      assert(ev.task.copy(lastUpdated = now) == task.copy(lastUpdated = now))
    }

    "Adding another Task to the project" in{
      val outcome = taskTestDriver.run(AddTask(name = task1.name, description = task1.description, section = task1.section))
      val ev = outcome.events.head.asInstanceOf[TaskAdded]
      assert(ev.task.name == task1.name && ev.task.section ==  task1.section)
      val outcome2 = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome2.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 2)
    }

    "Delete Task to the project" in{
      val outcome = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 2)
      val taskToDel = proj.tasks.head._2
      val outcome2 = taskTestDriver.run(DeleteTask(taskToDel.id))
      val outcome3 = taskTestDriver.run(GetProject(tp2.id))
      val proj2 = outcome3.replies.head.asInstanceOf[Project]
      assert(proj2.id == tp2.id)
      assert(proj2.tasks.size == 1)
    }

    "Add a note to a task" in {
      val outcome = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 1)
      val task = proj.tasks.head._2
      val noteTxt = "Testing Note Text"
      taskTestDriver.run(AddNote(task.id, proj.owner, noteTxt))
      taskTestDriver.run(AddNote(task.id, proj.owner, noteTxt))
      val outcome2 = taskTestDriver.run(GetProject(tp2.id))
      val proj2 = outcome2.replies.head.asInstanceOf[Project]
      assert(proj2.id == tp2.id)
      assert(proj2.tasks.size == 1)
      val task2 = proj2.tasks.head._2
      assert(task2.notes.size == 2)
      assert(task2.notes.forall(_.note == noteTxt))
    }

    "Delete a note" in {
      val outcome = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 1)
      val task = proj.tasks.head._2
      val note = task.notes.head
      taskTestDriver.run(DeleteNote(task.id, note.id))
      val outcome2 = taskTestDriver.run(GetProject(tp2.id))
      val proj2 = outcome2.replies.head.asInstanceOf[Project]
      assert(proj2.id == tp2.id)
      assert(proj2.tasks.size == 1)
      val task2 = proj2.tasks.head._2
      assert(task2.notes.size == 1)
      val noteTxt = "Testing Note Text"
      assert(task2.notes.forall(_.note == noteTxt))
    }

    "Adding another Task as a subtask" in{
      val outcome = taskTestDriver.run(GetProject(tp2.id))
      val proj = outcome.replies.head.asInstanceOf[Project]
      assert(proj.id == tp2.id)
      assert(proj.tasks.size == 1)
      val task = proj.tasks.head._2
      val outcome2 = taskTestDriver.run(AddTask(name = task1.name, description = task1.description, section = task1.section, parent = Some(task1.id)))
      val ev2 = outcome2.events.head.asInstanceOf[TaskAdded]
      assert(ev2.task.name == task1.name && ev2.task.section ==  task1.section && ev2.task.parent == Some(task1.id))
      val outcome3 = taskTestDriver.run(GetProject(tp2.id))
      val proj2 = outcome3.replies.head.asInstanceOf[Project]
      assert(proj2.id == tp2.id)
      assert(proj2.tasks.size == 2)
    }
  }

}

object ProjectEntitySpec{
  val entityId = UUID.randomUUID()
  val entityId2 = UUID.randomUUID()

  val testUserId = UUID.randomUUID()
  val testTeamId = UUID.randomUUID()

  lazy val tp = Project(
    id = entityId,
    name = "Test Name",
    owner = testUserId,
    team = testTeamId,
    description = "This is a a description of the project",
    imgUrl = Some("http://some-image.com/image.png"),
    tasks = Map.empty[String,Task]
  )

  val testUserId2 = UUID.randomUUID()
  val testTeamId2 = UUID.randomUUID()

  lazy val tpUpdate = Project(
    id = entityId,
    name = "Test Name2",
    owner = testUserId2,
    team = testTeamId2,
    description = "Updated description",
    imgUrl = Some("http://some-image.com/image2.png"),
    tasks = Map.empty[String,Task]
  )

  lazy val tp2 = Project(
    id = entityId2,
    name = "Test Name2",
    owner = testUserId2,
    team = testTeamId2,
    description = "Updated description",
    imgUrl = Some("http://some-image.com/image2.png"),
    tasks = Map.empty[String,Task]
  )

  lazy val task1 = Task(
    id = UUID.randomUUID(),
    name = "Task 1",
    description = "Task Description",
    done = false,
    assigned = None,
    startDate = None,
    endDate = None,
    lastUpdated = Instant.now,
    section = "Section 1",
  )

}
