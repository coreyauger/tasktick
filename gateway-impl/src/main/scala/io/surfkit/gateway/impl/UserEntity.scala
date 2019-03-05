package io.surfkit.gateway.impl

import java.util.UUID
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import io.surfkit.gateway.api._
import io.surfkit.gateway.impl.util.{SecurePasswordHashing, Token}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq


class UserEntity extends PersistentEntity {

  override type Command = UserCommand[_]
  override type Event = UserEvent
  override type State = UserState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: UserState = UserState(None, Seq.empty)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case UserState(_, _) => Actions()

      //
      // JWT Ideneity Commands / Actions / Events....
      ///
      .onCommand[CreateUser, GeneratedIdDone] {
        case (CreateUser(firstName, lastName, email, password), ctx, state) =>
          state.user match {
            case None if email == entityId =>
              val hashedPassword = SecurePasswordHashing.hashPassword(password)
              val userId = UUID.randomUUID()
              ctx.thenPersist(
                UserCreated(
                  userId = userId,
                  firstName = firstName,
                  lastName = lastName,
                  email = email,
                  hashedPassword = hashedPassword
                )
              ) { _ =>
                ctx.reply(GeneratedIdDone(userId.toString))
              }
            case _ =>
              ctx.invalidCommand(s"Failed to create user where entityId is ${entityId}")
              ctx.done
          }
      }
      .onCommand[AddProjectRef, ProjectRefAdded] {
        case (AddProjectRef(ref), ctx, _) =>
          ctx.thenPersist(ProjectRefAdded(ref))(ctx.reply)
      }
      .onReadOnlyCommand[GetUserState, UserStateDone] {
        case (GetUserState(), ctx, state) =>
          state.user match {
            case None =>
              ctx.invalidCommand(s"User registered with ${entityId} can't be found")
            case Some(user: User) =>
              ctx.reply(
                UserStateDone(
                  io.surfkit.gateway.api.User(
                    id = user.id.toString,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    hashedPassword = user.hashedPassword
                  )
                )
              )
          }
      }
      .onReadOnlyCommand[GetUserProjects, ProjectRefList] {
        case (GetUserProjects(skip, take), ctx, state) =>
            ctx.reply(ProjectRefList(state.projects))
      }
      .onReadOnlyCommand[GetUserProject, ProjectRef] {
        case (GetUserProject(id), ctx, state) =>
          state.projects.find(_.id == id) match{
            case Some(ref) => ctx.reply(ref)
            case None => ctx.invalidCommand(s"Could not locate project ${id} for user ${entityId}")
          }
      }
      .onEvent {
        case (UserCreated(userId, firstName, lastName, email, hashedPassword), state) =>
          state.copy(Some(
            User(
              id = userId,
              firstName = firstName,
              lastName = lastName,
              email = email,
              hashedPassword = hashedPassword
            ))
          )
        case (ProjectRefAdded(ref), state) =>
          state.copy(projects = state.projects :+ ref)
      }
    //
    // END : JWT Ideneity Commands / Actions / Events....
    //
  }
}

/**
  * State
  */

case class UserState(user: Option[User], projects: Seq[ProjectRef])
object UserState { implicit val format: Format[UserState] = Json.format }

case class User(
             id: UUID,
             firstName: String,
             lastName: String,
             email: String,
             hashedPassword: String
           )
object User { implicit val format: Format[User] = Json.format }


/**
  * Events
  */
sealed trait UserEvent extends AggregateEvent[UserEvent] {
  def aggregateTag: AggregateEventTag[UserEvent] = UserEvent.Tag
}
object UserEvent {
  val Tag: AggregateEventTag[UserEvent] = AggregateEventTag[UserEvent]
}

case class ClientCreated(company: String) extends UserEvent
object ClientCreated { implicit val format: Format[ClientCreated] = Json.format }

case class UserCreated(userId: UUID, firstName: String, lastName: String, email: String, hashedPassword: String) extends UserEvent
object UserCreated { implicit val format: Format[UserCreated] = Json.format }


case class ProjectRefAdded(ref: ProjectRef) extends UserEvent
object ProjectRefAdded { implicit val format: Format[ProjectRefAdded] = Json.format }

/**
  * Commands
  */
sealed trait UserCommand[R] extends ReplyType[R]

case class CreateUser(
             firstName: String,
             lastName: String,
             email: String,
             password: String
           ) extends UserCommand[GeneratedIdDone]
object CreateUser { implicit val format: Format[CreateUser] = Json.format }

case class GetUserState() extends UserCommand[UserStateDone]

case class GetUserProjects(skip: Int, take: Int) extends UserCommand[ProjectRefList]
object GetUserProjects { implicit val format: Format[GetUserProjects] = Json.format }

case class AddProjectRef(ref: ProjectRef) extends UserCommand[ProjectRefAdded]
object AddProjectRef { implicit val format: Format[AddProjectRef] = Json.format }

case class GetUserProject(id: UUID) extends UserCommand[ProjectRef]
object GetUserProject { implicit val format: Format[GetUserProject] = Json.format }


/**
  * Serialization
  */
object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[GetUserProject],
    JsonSerializer[AddProjectRef],
    JsonSerializer[ProjectRefAdded],
    JsonSerializer[GetUserProjects],
    JsonSerializer[GeneratedIdDone],
    JsonSerializer[CreateUser],
    JsonSerializer[UserStateDone],
    JsonSerializer[ClientCreated],
    JsonSerializer[UserCreated],
    JsonSerializer[UserCreated],
    JsonSerializer[User],
    JsonSerializer[Token],
    JsonSerializer[UserState]
  )
}
