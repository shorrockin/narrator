package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor
import org.apache.commons.lang.reflect.ConstructorUtils
import utils.{UniqueId, Logging}
import se.scalablesolutions.akka.remote.RemoteClient
import java.net.InetSocketAddress

/**
 * slave actor is responsible for processing and executing work.
 */
class SlaveActor extends Actor with Logging with UniqueId {
  var stories:List[StoryActor]    = Nil
  var workloadStats               = List[StoryStats]()
  var statsCounter                = 0
  var master:Option[Actor]        = None
  var client:Option[(String, Int)] = None
  

  /**
   * called by akka to received the event
   */
  def receive = {
    case RegisterWork(src, me, workloads) =>
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
          actor ! RegisterSlave(this)
        }
      }

      client = Some(src._2 -> src._3)
      master = Some(RemoteClient.actorFor(src._1, classOf[MasterActor].getName, src._2, src._3))
      master.get ! ReadyToStart(me)

    case Stop =>
      logger.info("recieved request to stop all stories")
      stories.foreach { _ ! Stop }

    case StartWork() =>
      logger.info("recieved request to start doing work")
      stories.foreach { _ ! Start }

    case StoryStatsReport(stats) => {
      logger.debug("recieved story stats report from story")
      statsCounter = statsCounter + 1
      workloadStats.find { ss => ss.description.equals(stats.description) } match {
        case None    => workloadStats = stats :: workloadStats
        case Some(i) => i.merge(stats)
      }

      if (statsCounter >= stories.length && master.isDefined) {
        logger.info("sending workload report to master")
        master.get ! WorkloadStatsReport(workloadStats)

        logger.info("shutting down slave and open client connection")
        stories.foreach { _.stop }
        client.foreach { (tup) => RemoteClient.shutdownClientFor(new InetSocketAddress(tup._1, tup._2)) }
        stop
      }
    }
  }
}