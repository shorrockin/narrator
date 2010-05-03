package com.shorrockin.narrator

import _root_.utils.UniqueId
import se.scalablesolutions.akka.actor.Actor
import utils.Logging
import org.apache.commons.lang.reflect.ConstructorUtils
import java.util.UUID

/**
 * slave actor is responsible for processing and executing work.
 */
class SlaveActor extends Actor with Logging with UniqueId {
  var stories:List[StoryActor] = Nil
  id = UUID.randomUUID.toString

  /**
   * called by akka to received the event
   */
  def receive = {
    case RegisterWork(source, me, workloads) =>
      logger.info("recieved workload from master of: " + workloads)

      workloads.foreach { workload =>
        logger.info("creating story actors for workload: " + workload)
        (workload.start until workload.end).foreach { i =>
          val story = ConstructorUtils.invokeConstructor(workload.story,
                                                         Array[AnyRef](i.asInstanceOf[java.lang.Integer],
                                                         workload.params)).asInstanceOf[Story]
          val actor = new StoryActor(story)
          link(actor)

          stories = actor :: stories
          actor.start
        }
      }

      reply(ReadyToStart(me))

    case Stop =>
      logger.info("recieved request to stop all stories")
      stories.foreach { _ ! Stop }

    case StartWork() =>
      logger.info("recieved request to start doing work")
      stories.foreach { _ ! Start }
  }
}