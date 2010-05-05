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

        val constructor = ConstructorUtils.getAccessibleConstructor(workload.story, Array[Class[_]](classOf[Int], classOf[Map[String, String]]))
        if (null == constructor) {
          logger.error("all stories must have a constructor of type Int, Map[String, String], %s does not".format(workload.story))
          throw new IllegalArgumentException("all stories must have a constructor of type Int, Map[String, String], %s does not".format(workload.story))
        }

        (workload.start until workload.end).foreach { i =>
          val story = constructor.newInstance(i.asInstanceOf[java.lang.Integer], workload.params).asInstanceOf[Story]
          val actor = new StoryActor(story)
          link(actor)

          stories = actor :: stories
          actor.start
          actor ! RegisterSlave(this)
        }
        logger.info("all story actors for workload created: " + workload)
      }

      logger.info("notifying master node that we're ready to begin")
      client = Some(src._2 -> src._3)
      master = Some(RemoteClient.actorFor(src._1, classOf[MasterActor].getName, src._2, src._3))
      master.get ! ReadyToStart(me)

    case Stop =>
      logger.info("recieved request to stop all stories")
      stories.foreach { _ ! Stop }
      logger.info("all stories have been requested to stop")

    case StartWork() =>
      logger.info("recieved request to start doing work, notifying stories")
      stories.foreach { _ ! Start }
      logger.info("all stories have been notified to start")

    case StoryStatsReport(stats) => {
      if (statsCounter == 0) logger.info("recieved statistics report from story, waiting on remaining")
      statsCounter = statsCounter + 1
      workloadStats.find { ss => ss.description.equals(stats.description) } match {
        case None    => workloadStats = stats :: workloadStats
        case Some(i) => i.merge(stats)
      }

      // TODO should be optionally configurable
      if (statsCounter % 10000 == 0) {
        logger.info("have processed %s workload reports, %s remaining".format(statsCounter, (stories.length - statsCounter)))
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
