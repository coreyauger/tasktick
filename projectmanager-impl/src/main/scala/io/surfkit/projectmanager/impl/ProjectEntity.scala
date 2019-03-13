package io.surfkit.projectmanager.impl

import java.time.Instant
import java.util.UUID
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}


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
    tasks = Set.empty[Task]
  ), false)

  def updateProject(state: ProjectState, name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String]) = state.project.copy(
      name = name,
      owner = owner,
      team = team,
      description = description,
      imgUrl = imageUrl,
      tasks = Set.empty[Task]  // NOTE: that we never populate tasks here..
    )

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case ProjectState(proj, created, archived) =>
      Actions()
        .onCommand[CreateProject, ProjectCreated] {
          case (CreateProject(name, _, _, _, _), ctx, state) if name.isEmpty  =>
            ctx.commandFailed(new RuntimeException("Can't create project with an empty name"))
            ctx.done
          case (CreateProject(name, owner, team, description, imageUrl), ctx, state) if !created  =>
            ctx.thenPersist(ProjectCreated(updateProject(state, name, owner, team, description, imageUrl)))(ctx.reply)
          case (_, ctx, _) if created =>
            ctx.commandFailed(new RuntimeException("Project Already Exists"))
            ctx.done
        }.onCommand[UpdateProject, ProjectUpdated] {
          case (UpdateProject(name, owner, team, description, imageUrl), ctx, state) =>
            ctx.thenPersist(ProjectUpdated(updateProject(state, name, owner, team, description, imageUrl)))(ctx.reply)
        }.onCommand[DeleteProject, ProjectDeleted] {
          case (DeleteProject(id), ctx, state) => ctx.thenPersist(ProjectDeleted(id))(ctx.reply)
        }

        .onCommand[AddTask, TaskAdded] {
          case (AddTask(name, description, section, parent), ctx, state) =>
            // TODO: deal with parent
            ctx.thenPersist(TaskAdded(Task(
              id = UUID.randomUUID(),
              project = UUID.fromString(entityId),
              name = name,
              description = description,
              done =  false,
              assigned = None,
              startDate = None,
              endDate = None,
              lastUpdated = Instant.now(),
              section = section,
              parent = parent,
              notes = Seq.empty[Note]
            )))(ctx.reply)
        }.onCommand[UpdateTask, TaskUpdated] {
          case (UpdateTask(id, name, description, parent, done, assigned, startDate, endDate, section), ctx, state) =>
            val task = state.project.tasks.find(_.id == id).map{ t =>
              t.copy(
                id = id,
                name = name,
                description = description,
                done = done,
                assigned = assigned,
                lastUpdated = Instant.now(),
                startDate = startDate,
                parent = parent,
                endDate = endDate,
                section = section
              )
            }.getOrElse{
              Task(
                id = id,
                name = name,
                project = UUID.fromString(entityId),
                description = description,
                done = done,
                assigned = assigned,
                lastUpdated = Instant.now(),
                startDate = startDate,
                endDate = endDate,
                section = section,
                parent = parent,
                notes = Seq.empty[Note]
              )
            }
            ctx.thenPersist(TaskUpdated(task))(ctx.reply)
        }.onCommand[DeleteTask, TaskDeleted] {
          case (DeleteTask(id), ctx, state) => ctx.thenPersist(TaskDeleted(id))(ctx.reply)
        }

        .onCommand[AddNote, NoteAdded] {
          case (AddNote(task, user, note), ctx, state) =>
            ctx.thenPersist(NoteAdded(task, Note(id = UUID.randomUUID(), task=task, project = UUID.fromString(entityId),user = user, note = note, date = Instant.now())))(ctx.reply)
        }.onCommand[DeleteNote, NoteDeleted] {
          case (DeleteNote(task, id), ctx, state) => ctx.thenPersist(NoteDeleted(task, id))(ctx.reply)
        }

        .onReadOnlyCommand[GetProject, Project] {
          case (GetProject(_), ctx, state) => ctx.reply(proj)
        }.onEvent {
          case (ProjectCreated(project), state) => state.copy(project = project, true, false) // retain the tasks..
          case (ProjectUpdated(project), state) => state.copy(project = project.copy(tasks = state.project.tasks)) // retain the tasks..
          case (ProjectDeleted(_), state) => state.copy(archived = true)

          case (TaskAdded(task), state) => state.copy(project = state.project.copy(tasks = state.project.tasks.filterNot(_.id == task.id) + task) )
          case (TaskUpdated(task), state) => state.copy(project = state.project.copy(tasks = state.project.tasks.filterNot(_.id == task.id) + task) )
          case (TaskDeleted(id), state) => state.copy(project = state.project.copy(tasks = state.project.tasks.filterNot(_.id == id)) )

          case (NoteAdded(taskId, note), state) =>
            state.project.tasks.find(_.id == taskId).map { t =>
              state.copy(project = state.project.copy(tasks = state.project.tasks + t.copy(notes = t.notes.filterNot(_.id == note.id) :+ note)))
            }.getOrElse(state)

          case (NoteDeleted(taskId, note), state) =>
            state.project.tasks.find(_.id == taskId).map { t =>
              state.copy(project = state.project.copy(tasks = state.project.tasks + t.copy(notes = t.notes.filterNot(_.id == note))))
            }.getOrElse(state)
        }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class ProjectState(project: Project, created: Boolean = false, archived: Boolean = false)
object ProjectState {
  implicit val format: Format[ProjectState] = Json.format
}

case class Project(
                    id: UUID,
                    name: String,
                    owner: UUID,
                    team: UUID,
                    description: String,
                    imgUrl: Option[String],
                    tasks: Set[Task]
                  )
object Project { implicit val format: Format[Project] = Json.format }

case class Note(id: UUID, project: UUID, task: UUID, user: UUID, note: String, date: Instant)
object Note { implicit val format: Format[Note] = Json.format }

case class Task(
                 id: UUID,
                 project: UUID,
                 name: String,
                 description: String,
                 done: Boolean,
                 assigned: Option[UUID],
                 startDate: Option[Instant],
                 endDate: Option[Instant],
                 lastUpdated: Instant,
                 section: String,
                 parent: Option[UUID] = None,
                 notes: Seq[Note] = Seq.empty[Note]
               )
object Task { implicit val format: Format[Task] = Json.format }


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
object ProjectCreated { implicit val format: Format[ProjectCreated] = Json.format }

case class ProjectUpdated(project: Project) extends ProjectEvent
object ProjectUpdated { implicit val format: Format[ProjectUpdated] = Json.format}

case class ProjectDeleted(id: UUID) extends ProjectEvent
object ProjectDeleted { implicit val format: Format[ProjectDeleted] = Json.format }

// Task Events ..
case class TaskAdded(task: Task) extends ProjectEvent
object TaskAdded { implicit val format: Format[TaskAdded] = Json.format }

case class TaskUpdated(task: Task) extends ProjectEvent
object TaskUpdated { implicit val format: Format[TaskUpdated] = Json.format }

case class TaskDeleted(id: UUID) extends ProjectEvent
object TaskDeleted { implicit val format: Format[TaskDeleted] = Json.format }

// Notes
case class NoteAdded(task: UUID, note: Note) extends ProjectEvent
object NoteAdded {implicit val format: Format[NoteAdded] = Json.format }

case class NoteDeleted(task: UUID, id: UUID) extends ProjectEvent
object NoteDeleted { implicit val format: Format[NoteDeleted] = Json.format }



/**
  * This interface defines all the commands that the ProjectEntity supports.
  */
sealed trait ProjectCommand[R] extends ReplyType[R]

// Projects
case class CreateProject(name: String, owner: UUID, team: UUID, description: String, imageUrl: Option[String] = None) extends ProjectCommand[ProjectCreated]
object CreateProject { implicit val format: Format[CreateProject] = Json.format }

case class UpdateProject( name: String,
                          owner: UUID,
                          team: UUID,
                          description: String,
                          imageUrl: Option[String]) extends ProjectCommand[ProjectUpdated]
object UpdateProject { implicit val format: Format[UpdateProject] = Json.format }

case class DeleteProject(id: UUID)extends ProjectCommand[ProjectDeleted]
object DeleteProject { implicit val format: Format[DeleteProject] = Json.format }

case class GetProject(id: UUID) extends ProjectCommand[Project]
object GetProject { implicit val format: Format[GetProject] = Json.format }

// Tasks
case class AddTask(name: String,  description: String, section: String, parent: Option[UUID] = None) extends ProjectCommand[TaskAdded]
object AddTask { implicit val format: Format[AddTask] = Json.format }

case class UpdateTask( id: UUID,
                       name: String,
                       description: String,
                       parent: Option[UUID],
                       done: Boolean,
                       assigned: Option[UUID],
                       startDate: Option[Instant],
                       endDate: Option[Instant],
                       section: String
                     ) extends ProjectCommand[TaskUpdated]
object UpdateTask { implicit val format: Format[UpdateTask] = Json.format }

case class DeleteTask(id: UUID)extends ProjectCommand[TaskDeleted]
object DeleteTask { implicit val format: Format[DeleteTask] = Json.format }

// Notes
case class AddNote(task: UUID, user: UUID, note: String)extends ProjectCommand[NoteAdded]
object AddNote { implicit val format: Format[AddNote] = Json.format }

case class DeleteNote(task: UUID, id: UUID)extends ProjectCommand[NoteDeleted]
object DeleteNote { implicit val format: Format[DeleteNote] = Json.format }


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
  override def serializers: scala.collection.immutable.Seq[JsonSerializer[_]] = scala.collection.immutable.Seq(
    JsonSerializer[Project],
    JsonSerializer[Task],
    JsonSerializer[Note],
    JsonSerializer[GetProject],
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
