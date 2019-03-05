package io.surfkit.gateway.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import io.surfkit.gateway.api.{GeneratedIdDone, UserStateDone}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.util.Try

class UserEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("UserEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(UserSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  val testEmail = "test.user@example.com"
  val testPass = "Thi5IsMyP@ss"

  /*private def withTestDriver(block: PersistentEntityTestDriver[UserCommand[_], UserEvent, UserState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new UserEntity, testEmail)
    block(driver)
    driver.getAllIssues should have size 0
  }*/

  val driver = new PersistentEntityTestDriver(system, new UserEntity, testEmail)


  "user entity" should {

    "create a user" in  {
      val outcome = driver.run(CreateUser(
        firstName = "fname",
        lastName = "lname",
        email = testEmail,
        password = testPass
      ))
      val entityId = outcome.replies.head.asInstanceOf[GeneratedIdDone]
      driver.getAllIssues should have size 0
      assert( Try(UUID.fromString(entityId.id)).isSuccess )
    }

    "get a user" in  {
      val outcome = driver.run(GetUserState())
      val us = outcome.replies.head.asInstanceOf[UserStateDone]
      assert(us.user.email == testEmail && us.user.firstName == "fname" && us.user.lastName == "lname")
    }

    "fail to create another user" in {
      val outcome = driver.run(CreateUser(
        firstName = "xxx",
        lastName = "yyy",
        email = "some.other@example.com",
        password = testPass
      ))
      //println(s"outcome: ${outcome}")
      driver.getAllIssues should have size 1
      assert(outcome.replies.head.isInstanceOf[InvalidCommandException])
    }

    "fail to find a user that does not exist" in {
      val driver = new PersistentEntityTestDriver(system, new UserEntity, "test@test.com")
      val outcome = driver.run(GetUserState())
      //println(s"outcome: ${outcome}")
      driver.getAllIssues should have size 1
      assert(outcome.replies.head.isInstanceOf[InvalidCommandException])
    }
  }
}
