package io.surfkit.gateway.impl

import java.time.LocalDateTime
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
  override def initialState: UserState = UserState(None)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case UserState(_) => Actions()

      //
      // JWT Ideneity Commands / Actions / Events....
      ///
        .onCommand[RegisterClient, GeneratedIdDone] {
        case (RegisterClient(company, firstName, lastName, email, username, password), ctx, state) =>
          state.client match {
            case Some(_) =>
              ctx.invalidCommand(s"Client with id ${entityId} is already registered")
              ctx.done
            case None =>
              val hashedPassword = SecurePasswordHashing.hashPassword(password)
              val userId = UUID.randomUUID()

              ctx.thenPersistAll(
                ClientCreated(company),
                UserCreated(
                  userId = userId,
                  firstName = firstName,
                  lastName = lastName,
                  email = email,
                  username = username,
                  hashedPassword = hashedPassword
                )
              ) { () =>
                ctx.reply(GeneratedIdDone(entityId))
              }
          }
      }
        .onCommand[CreateUser, GeneratedIdDone] {
        case (CreateUser(firstName, lastName, email, username, password), ctx, state) =>
          state.client match {
            case Some(_) =>
              val hashedPassword = SecurePasswordHashing.hashPassword(password)
              val userId = UUID.randomUUID()

              ctx.thenPersist(
                UserCreated(
                  userId = userId,
                  firstName = firstName,
                  lastName = lastName,
                  email = email,
                  username = username,
                  hashedPassword = hashedPassword
                )
              ) { _ =>
                ctx.reply(GeneratedIdDone(userId.toString))
              }
            case None =>
              ctx.invalidCommand(s"Client with id ${entityId} not found")
              ctx.done
          }
      }
      .onReadOnlyCommand[GetUserState, UserStateDone] {
        case (GetUserState(), ctx, state) =>
          state.client match {
            case None =>
              ctx.invalidCommand(s"Client registered with ${entityId} can't be found")
            case Some(client: Client) =>
              ctx.reply(
                UserStateDone(
                  id = entityId,
                  company = client.company,
                  users = client.users.map(user =>
                    io.surfkit.gateway.api.User(
                      id = user.id.toString,
                      firstName = user.firstName,
                      lastName = user.lastName,
                      email = user.email,
                      username = user.username
                    )
                  )
                )
              )
          }
      }
        .onEvent {
          case (ClientCreated(company), _) => UserState(Some(Client(id = UUID.fromString(entityId), company = company)))
          case (UserCreated(userId, firstName, lastName, email, username, password), state) =>
            state.addUser(
              User(
                id = userId,
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                password = password
              )
            )
        }
    //
    // END : JWT Ideneity Commands / Actions / Events....
    //
  }
}

/**
  * The current state held by the persistent entity.
  */

case class UserState(client: Option[Client]) {
  def addUser(user: User): UserState = client match {
    case None => throw new IllegalStateException("User can't be added before client is created")
    case Some(client) =>
      val newUsers =  client.users :+ user
      UserState(Some(client.copy(users = newUsers)))
  }
}
object UserState {
  implicit val format: Format[UserState] = Json.format
}

case class Client(
                   id: UUID,
                   company: String,
                   users: scala.collection.immutable.Seq[User] = scala.collection.immutable.Seq.empty
                 )
object Client {
  implicit val format: Format[Client] = Json.format
}

case class User(
                 id: UUID,
                 firstName: String,
                 lastName: String,
                 email: String,
                 username: String,
                 password: String
               )
object User {
  implicit val format: Format[User] = Json.format
}





/**
  * This interface defines all the events that the UserEntity supports.
  */
sealed trait UserEvent extends AggregateEvent[UserEvent] {
  def aggregateTag: AggregateEventTag[UserEvent] = UserEvent.Tag
}

object UserEvent {
  val Tag: AggregateEventTag[UserEvent] = AggregateEventTag[UserEvent]
}



case class ClientCreated(company: String) extends UserEvent
object ClientCreated {
  implicit val format: Format[ClientCreated] = Json.format
}

case class UserCreated(userId: UUID, firstName: String, lastName: String, email: String, username: String, hashedPassword: String) extends UserEvent
object UserCreated {
  implicit val format: Format[UserCreated] = Json.format
}





/**
  * This interface defines all the commands that the UserEntity supports.
  */
sealed trait UserCommand[R] extends ReplyType[R]
case class RegisterClient(
                           company: String,
                           firstName: String,
                           lastName: String,
                           email: String,
                           username: String,
                           password: String
                         ) extends PersistentEntity.ReplyType[GeneratedIdDone] with UserCommand[GeneratedIdDone]
object RegisterClient {
  implicit val format: Format[RegisterClient] = Json.format
}

case class CreateUser(
                       firstName: String,
                       lastName: String,
                       email: String,
                       username: String,
                       password: String
                     ) extends PersistentEntity.ReplyType[GeneratedIdDone] with UserCommand[GeneratedIdDone]
object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}

case class GetUserState() extends PersistentEntity.ReplyType[UserStateDone] with UserCommand[UserStateDone]



/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[GeneratedIdDone],
    JsonSerializer[CreateUser],
    JsonSerializer[RegisterClient],
    JsonSerializer[UserStateDone],
    JsonSerializer[ClientCreated],
    JsonSerializer[UserCreated],
    JsonSerializer[UserLogin],
    JsonSerializer[UserLoginDone],
    JsonSerializer[UserCreated],
    JsonSerializer[User],
    JsonSerializer[Token],
    JsonSerializer[UserState]
  )
}
