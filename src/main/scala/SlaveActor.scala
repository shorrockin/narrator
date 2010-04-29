package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor
import utils.Logging
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode}

/**
 * slave actor is responsible for processing and executing work.
 */
class SlaveActor(host:String, port:Int) extends Actor with Logging {

  /**
   * called by akka to received the event
   */
  def receive = {
    case RegisterWork(source, me, workload) =>
      logger.debug("recieved workload from master of: " + workload)
      RemoteClient.actorFor("slave", source._1, source._2) ! ReadyToStart(me)
    
    case StartWork() =>
      logger.debug("recieved request to start doing work")
  }

  
  /**
   * overrides the start method to implement the business logic of starting, as well
   * as the remote actor registration.
   */
  override def start = {
    logger.debug("starting slave actor on %s:%s".format(host, port))
    super.start
    RemoteNode.start(host, port)
    RemoteNode.register("slave", this)
    this
  }

  
  /**
   * stops this actor and shuts down the remote node
   */
  override def stop {
    logger.debug("shutting down slave actor on %s:%s".format(host, port))
    super.stop
    RemoteNode.shutdown
  }  
}