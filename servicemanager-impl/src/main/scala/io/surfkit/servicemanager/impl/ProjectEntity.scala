package io.surfkit.servicemanager.impl

import java.time.Instant
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq


class ProjectEntity extends PersistentEntity {

  override type Command = ProjectCommand[_]
  override type Event = ProjectEvent
  override type State = ProjectState

  /**
    * The initial state... is a "Null" project
    */
  override def initialState: ProjectState = ProjectState(Project(
    id = UUID.fromString(entityId),
    name = "",
    owner = UUID.randomUUID(),
    team = UUID.randomUUID(),
    description = "",
    imgUrl = None,
    tasks = Map.empty[String, Task]
  ), false)

  def updateProject(state: ProjectState, name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String]) = state.project.copy(
      name = name,
      owner = owner,
      team = team,
      description = description,
      imgUrl = imageUrl,
      tasks = Map.empty[String, Task]  // NOTE: that we never populate tasks here..
    )

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case ProjectState(proj, _) =>
      Actions()
        .onCommand[CreateProject, ProjectCreated] {
          case (CreateProject(name, owner, team, description, imageUrl), ctx, state) =>
            ctx.thenPersist(ProjectCreated(updateProject(state, name, owner, team, description, imageUrl)))(ctx.reply)
        }.onCommand[UpdateProject, ProjectUpdated] {
          case (UpdateProject(name, owner, team, description, imageUrl), ctx, state) =>
            ctx.thenPersist(ProjectUpdated(updateProject(state, name, owner, team, description, imageUrl)))(ctx.reply)
        }.onCommand[DeleteProject, ProjectDeleted] {
          case (DeleteProject(id), ctx, state) => ctx.thenPersist(ProjectDeleted(id))(ctx.reply)
        }

        .onCommand[AddTask, TaskAdded] {
          case (AddTask(name, section, parent), ctx, state) =>
            // TODO: deal with parent
            ctx.thenPersist(TaskAdded(Task(
              id = UUID.randomUUID(),
              name = name,
              description = "",
              done =  false,
              assigned = None,
              startDate = None,
              endDate = None,
              lastUpdated = Instant.now(),
              section = section,
              subTasks = List.empty[Task],
              notes = Seq.empty[Note]
            )))(ctx.reply)
        }.onCommand[UpdateTask, TaskUpdated] {
          case (UpdateTask(id, name, description, done, assigned, startDate, endDate, section), ctx, state) =>
            val task = state.project.tasks.values.find(_.id == id).getOrElse{
              Task(
                id = id,
                name = name,
                description = description,
                done = done,
                assigned = assigned,
                lastUpdated = Instant.now(),
                startDate = startDate,
                endDate = endDate,
                section = section,
                subTasks = List.empty[Task],
                notes = Seq.empty[Note]
              )
            }
            ctx.thenPersist(TaskUpdated(task))(ctx.reply)
        }.onCommand[DeleteTask, TaskDeleted] {
          case (DeleteTask(id), ctx, state) => ctx.thenPersist(TaskDeleted(id))(ctx.reply)
        }

        .onCommand[AddNote, NoteAdded] {
          case (AddNote(task, user, note), ctx, state) =>
            ctx.thenPersist(NoteAdded(task, Note(id = UUID.randomUUID(), user = user, note = note, date = Instant.now())))(ctx.reply)
        }.onCommand[DeleteNote, NoteDeleted] {
          case (DeleteNote(task, id), ctx, state) => ctx.thenPersist(NoteDeleted(task, id))(ctx.reply)
        }

        .onReadOnlyCommand[GetProject, Project] {
          case (GetProject(_), ctx, state) => ctx.reply(state.project)
        }.onEvent {
          case (ProjectCreated(project), state) => ProjectState(project.copy(tasks = state.project.tasks)) // retain the tasks..
          case (ProjectUpdated(project), state) => ProjectState(project.copy(tasks = state.project.tasks)) // retain the tasks..
          case (ProjectDeleted(id), state) => ProjectState(state.project, true) // archived

          case (TaskAdded(task), state) => state.copy(project = state.project.copy(tasks = state.project.tasks + (task.id.toString -> task)) )
          case (TaskUpdated(task), state) => state.copy(project = state.project.copy(tasks = state.project.tasks + (task.id.toString -> task)) )
          case (TaskDeleted(id), state) => state.copy(project = state.project.copy(tasks = state.project.tasks - id.toString) )

          case (NoteAdded(taskId, note), state) =>
            state.project.tasks.get(taskId.toString).map { t =>
              state.copy(project = state.project.copy(tasks = state.project.tasks + (t.id.toString -> t.copy(notes = t.notes :+ note))))
            }.getOrElse(state)

          case (NoteDeleted(taskId, note), state) =>
            state.project.tasks.get(taskId.toString).map { t =>
              state.copy(project = state.project.copy(tasks = state.project.tasks + (t.id.toString -> t.copy(notes = t.notes.filterNot(_.id == note)))))
            }.getOrElse(state)
        }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class ProjectState(project: Project, archived: Boolean = false)
object ProjectState {
  implicit val format: Format[ProjectState] = Json.format
}

/**
  * This interface defines all the events that the ServiceManagerEntity supports.
  */
sealed trait ProjectEvent extends AggregateEvent[ProjectEvent] {
  def aggregateTag = ProjectEvent.Tag
}

object ProjectEvent {
  val Tag = AggregateEventTag[ProjectEvent]
}

// Project Events ..
case class ProjectCreated(project: Project) extends ProjectEvent
object ProjectCreated {
  implicit val format: Format[ProjectCreated] = Json.format
}
case class ProjectUpdated(project: Project) extends ProjectEvent
object ProjectUpdated {
  implicit val format: Format[ProjectUpdated] = Json.format
}
case class ProjectDeleted(id: UUID) extends ProjectEvent
object ProjectDeleted {
  implicit val format: Format[ProjectDeleted] = Json.format
}



// Task Events ..
case class TaskAdded(task: Task) extends ProjectEvent
object TaskAdded {
  implicit val format: Format[TaskAdded] = Json.format
}
case class TaskUpdated(task: Task) extends ProjectEvent
object TaskUpdated {
  implicit val format: Format[TaskUpdated] = Json.format
}
case class TaskDeleted(id: UUID) extends ProjectEvent
object TaskDeleted {
  implicit val format: Format[TaskDeleted] = Json.format
}

// Notes
case class NoteAdded(task: UUID, note: Note) extends ProjectEvent
object NoteAdded {
  implicit val format: Format[NoteAdded] = Json.format
}
case class NoteDeleted(task: UUID, id: UUID) extends ProjectEvent
object NoteDeleted {
  implicit val format: Format[NoteDeleted] = Json.format
}



/**
  * This interface defines all the commands that the ProjectEntity supports.
  */
sealed trait ProjectCommand[R] extends ReplyType[R]

case class Project(
               id: UUID,
               name: String,
               owner: UUID,
               team: UUID,
               description: String,
               imgUrl: Option[String],
               tasks: Map[String,Task]
                )
object Project {
  implicit val format: Format[Project] = Json.format
}

case class Note(id: UUID, user: UUID, note: String, date: Instant)
object Note {
  implicit val format: Format[Note] = Json.format
}

case class Task(
  id: UUID,
  name: String,
  description: String,
  done: Boolean,
  assigned: Option[UUID],
  startDate: Option[Instant],
  endDate: Option[Instant],
  lastUpdated: Instant,
  section: String,
  subTasks: List[Task],
  notes: Seq[Note]
)
object Task {
  implicit val format: Format[Task] = Json.format
}

// Projects

case class CreateProject(name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String] = None) extends ProjectCommand[ProjectCreated]
object CreateProject {
  implicit val format: Format[CreateProject] = Json.format
}

case class UpdateProject( name: String,
                          owner: UUID,
                          team: UUID,
                          description: String,
                          imgUrl: Option[String])extends ProjectCommand[ProjectUpdated]
object UpdateProject {
  implicit val format: Format[UpdateProject] = Json.format
}
case class DeleteProject(id: UUID)extends ProjectCommand[ProjectDeleted]
object DeleteProject {
  implicit val format: Format[DeleteProject] = Json.format
}
case class GetProject(id: UUID) extends ProjectCommand[Project]
object GetProject {
  implicit val format: Format[GetProject] = Json.format
}

// Tasks

case class AddTask(name: String, section: String, parent: Option[UUID] = None)extends ProjectCommand[TaskAdded]
object AddTask {
  implicit val format: Format[AddTask] = Json.format
}

case class UpdateTask( id: UUID,
                       name: String,
                       description: String,
                       done: Boolean,
                       assigned: Option[UUID],
                       startDate: Option[Instant],
                       endDate: Option[Instant],
                       section: String
                     ) extends ProjectCommand[TaskUpdated]
object UpdateTask {
  implicit val format: Format[UpdateTask] = Json.format
}
case class DeleteTask(id: UUID)extends ProjectCommand[TaskDeleted]
object DeleteTask {
  implicit val format: Format[DeleteTask] = Json.format
}


// Notes
case class AddNote(task: UUID, user: UUID, note: String)extends ProjectCommand[NoteAdded]
object AddNote {
  implicit val format: Format[AddNote] = Json.format
}
case class DeleteNote(task: UUID, id: UUID)extends ProjectCommand[NoteDeleted]
object DeleteNote {
  implicit val format: Format[DeleteNote] = Json.format
}



/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object ServiceManagerSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Project],
    JsonSerializer[CreateProject],
    JsonSerializer[UpdateProject],
    JsonSerializer[DeleteProject],
    JsonSerializer[ProjectDeleted],
    JsonSerializer[ProjectCreated],
    JsonSerializer[ProjectUpdated],

    JsonSerializer[Task],
    JsonSerializer[AddTask],
    JsonSerializer[UpdateTask],
    JsonSerializer[DeleteTask],
    JsonSerializer[TaskAdded],
    JsonSerializer[TaskUpdated],
    JsonSerializer[TaskDeleted],

    JsonSerializer[Note],
    JsonSerializer[AddNote],
    JsonSerializer[DeleteNote],
    JsonSerializer[NoteAdded],
    JsonSerializer[NoteDeleted],

    JsonSerializer[ProjectState]
  )
}
