package io.surfkit.servicemanager.impl

import io.surfkit.servicemanager.api
import io.surfkit.servicemanager.api.ServiceManagerService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import scala.concurrent.ExecutionContext.Implicits.global
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import java.util.UUID
/**
  * Implementation of the ServiceManagerService.
  */
class ServiceManagerServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends ServiceManagerService {

  override def getProject(id: UUID) = ServiceCall { _ =>
    val ref = persistentEntityRegistry.refFor[ProjectEntity](id.toString)
    ref.ask(GetProject(id)).map(convertProject)
  }

  private def convertProject(ev: Project): api.Project = ev match{
    case p:Project => api.Project(p.id, p.name)
  }

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
