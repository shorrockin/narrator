package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.remote.{RemoteNode}
import utils.{UniqueId, Logging}
import java.util.concurrent.TimeUnit
import se.scalablesolutions.akka.dispatch.Dispatchers

/**
 * the master actor acts as the coordinator of the tests, communicating
 * with all slaves and scheduling the work that each of them need to
 * perform.
 *
 * @author Chris Shorrock
 */
class MasterActor(host:String, port:Int, slaves:Seq[Slave], workGenerator:WorkloadGenerator, duration:Option[Long]) extends Actor with Logging with UniqueId {
  def this() = this("proxy-master-actor", 0, Nil, null, None)

  private lazy val slaveActors = Map(slaves.map { (slave) =>
    (slave -> spawnLinkRemote(classOf[SlaveActor], slave.host, slave.port))
  }:_*)

  private var ready = List[Slave]()
  
  /**
   * called by akka to received the event
   */
  def receive = {
    case ReadyToStart(source) =>
      logger.info("recieved ready to start message from: " + source)
      ready = source :: ready
      if (ready.length == slaves.length) { slaves.foreach { slaveActors(_) ! StartWork() } }

      if (duration.isDefined) {
        logger.info("scheduling master to shutdown in %s msecs".format(duration.get.toString))
        Scheduler.schedule(new Runnable() { def run() = { MasterActor.this.!(Stop)(None) } }, duration.get, TimeUnit.MILLISECONDS)
      }

    case Stop =>
      logger.info("recieved request to stop master, notifying all slaves")
      slaveActors.foreach { actor =>
        try { actor._2 ! Stop }
        catch { case e:Exception => logger.warn("error occured while shutting down remote slave actor: " + actor._1, e) }
      }

    case WorkloadStatsReport(workloadStats) => {
      workloadStats.foreach { story =>
        logger.info("Slave Story Stats: " + story.description)
        story.stats.foreach { (s) =>
          logger.info("  " + s.description + ":")
          logger.info("    Times Ran: %s".format(s.iterations))
          logger.info("    Avg Request Time (msecs): %s".format(s.averageTime))
          logger.info("    Max Request Time (msecs): %s".format(s.maxTime))
          logger.info("    Min Request Time (msecs): %s".format(s.minTime))
          logger.info("    Unknown Exceptions In Actions: %s".format(s.exceptions))
          s.userExceptions.foreach { (tup) =>
            logger.info("    User Exception (%s): %s".format(tup._1, tup._2))
          }
        }
      }
    }
  }


  /**
   * overrides the start method to implement the business logic of starting, as well
   * as the remote actor registration.
   */
  override def start = {
    logger.info("starting master actor on %s:%s".format(host, port))
    super.start

    setReplyToAddress(host, port)
    RemoteNode.start(host, port)
    RemoteNode.register(id, this)

    slaves.foreach { (slave) =>
      val workload = workGenerator.generateWorkload(slave)
      val actor    = slaveActors(slave)

      logger.info("sending work load registration of %s to %s".format(workload, slave))
      actor ! RegisterWork((id, host, port), slave, workload)
    }

    this
  }
}