package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode}
import utils.Logging


/**
 * the master actor acts as the coordinator of the tests, communicating
 * with all slaves and scheduling the work that each of them need to
 * perform.
 *
 * @author Chris Shorrock
 */
class MasterActor(host:String, port:Int, slaves:Seq[Slave], workGenerator:WorkloadGenerator) extends Actor with Logging {
  def this() = this(null, -1, Nil, null)

  private lazy val slaveActors = Map(slaves.map { (slave) =>
    val actor = RemoteClient.actorFor("slave", slave.host, slave.port)
    (slave -> actor)
  }:_*)


  private var ready = List[Slave]()
  

  /**
   * called by akka to received the event
   */
  def receive = {
    case ReadyToStart(source) =>
      logger.debug("recieved ready to start message from: " + source)
      ready = source :: ready
      if (ready.length == slaves.length) { slaves.foreach { slaveActors(_) ! StartWork() } }
  }


  /**
   * overrides the start method to implement the business logic of starting, as well
   * as the remote actor registration.
   */
  override def start = {
    logger.debug("starting master actor on %s:%s".format(host, port))
    super.start

    RemoteNode.start("127.0.0.1", 1234)
    RemoteNode.register("master", this)

    slaves.foreach { (slave) =>
      val workload = workGenerator.generateWorkload(slave)
      val actor    = slaveActors(slave)

      logger.debug("sending work load registration of %s to %s".format(workload, slave))
      actor ! RegisterWork((host -> port), slave, workload)
    }

    this
  }


  /**
   * stops this actor and shuts down the remote node
   */
  override def stop {
    logger.debug("stopping master actor on %s:%s".format(host, port))
    super.stop
    RemoteNode.shutdown
  }
}