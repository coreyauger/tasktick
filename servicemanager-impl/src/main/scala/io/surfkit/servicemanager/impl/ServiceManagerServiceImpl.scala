package io.surfkit.servicemanager.impl

import io.surfkit.servicemanager.api
import io.surfkit.servicemanager.api.ServiceManagerService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer

import scala.concurrent.ExecutionContext.Implicits.global
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import java.util.UUID

import akka.{Done, NotUsed}
/**
  * Implementation of the ServiceManagerService.
  */
class ServiceManagerServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends ServiceManagerService {

  override def getProject(id: UUID) = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](id.toString)
    ref.ask(GetProject(id)).map(convertProject)
  }

  override def createProject = ServiceCall{ req =>
    val newProjectId = UUID.randomUUID
    val ref = persistentEntityRegistry.refFor[ProjectEntity](newProjectId.toString)
    ref.ask(CreateProject(
      name = req.name,
      owner = req.owner,
      team = req.team,
      description = req.description,
      imageUrl = req.imageUrl
    )).map(x => convertProject(x.project))
  }

  override def updateProject = ServiceCall{ req =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](req.id.toString)
    ref.ask(UpdateProject(
    name = req.name,
    owner = req.owner,
    team = req.team,
    description = req.description,
    imageUrl = req.imgUrl
    )).map(x => convertProject(x.project))
  }

  override def addTask(project: UUID) = ServiceCall{ req =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](project.toString)
    ref.ask(AddTask(
      name = req.name,
      description = req.description,
      section = req.section,
      parent = req.parent
    )).map(x => convertTask(x.task))
  }

  override def updateTask(project: UUID) = ServiceCall{ req =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](project.toString)
    ref.ask(UpdateTask(
      id = req.id,
      name = req.name,
      description = req.description,
      parent = req.parent,
      done = req.done,
      assigned = req.assigned,
      startDate = req.startDate,
      endDate = req.endDate,
      section = req.section
    )).map(x => convertTask(x.task))
  }

  override def deleteTask(project: UUID, task: UUID) = ServiceCall{ _ =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](project.toString)
    ref.ask(DeleteTask(task)).map(_ => Done)
  }

  override def addTaskNote(project: UUID, task: UUID) = ServiceCall{ req =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](project.toString)
    ref.ask(AddNote(task, req.user, req.note)).map(x => convertNote(x.note))
  }

  override def deleteTaskNote(project: UUID, task: UUID, note: UUID) = ServiceCall{ _ =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](project.toString)
    ref.ask(DeleteNote(task, note)).map(_ => Done)
  }




  private def convertNote(n: Note):api.Note = api.Note(
    id = n.id,
    user = n.user,
    note = n.note,
    date = n.date
  )

  private def convertTask(t: Task): api.Task = api.Task(
    id = t.id,
    name = t.name,
    description = t.description,
    done = t.done,
    assigned = t.assigned,
    startDate = t.startDate,
    endDate = t.endDate,
    lastUpdated = t.lastUpdated,
    section = t.section,
    parent = t.parent,
    notes = t.notes.map(convertNote)
  )
  private def convertProject(p: Project): api.Project = api.Project(
      id = p.id,
      name = p.name,
      owner = p.owner,
      team = p.team,
      description = p.description,
      imgUrl = p.imgUrl,
      tasks = p.tasks.map{
        case (key, value) => key -> convertTask(value)
      }
     )


  override def projectsTopic(): Topic[api.ProjectUpdated] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(ProjectEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[ProjectEvent]): api.ProjectUpdated = {
    helloEvent.event match {
      case ProjectUpdated(p) => api.ProjectUpdated(convertProject(p))
    }
  }

}
