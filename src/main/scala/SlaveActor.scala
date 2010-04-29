package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor
import utils.Logging
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode}
import org.apache.commons.lang.reflect.ConstructorUtils

/**
 * slave actor is responsible for processing and executing work.
 */
class SlaveActor(host:String, port:Int) extends Actor with Logging {
  def this() = this("proxy-slave-actor", 0)

  var stories:List[StoryActor] = Nil

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

          stories = actor :: stories
          actor.start
        }
      }

      master(source) ! ReadyToStart(me)

    case Stop =>
      logger.info("recieved request to stop all stories")
      stories.foreach { _ ! Stop }

    
    case StartWork() =>
      logger.info("recieved request to start doing work")
      stories.foreach { _ ! Start }
  }


  /**
   * returns the master node for the specified source
   */
  def master(source:(String, Int)) = RemoteClient.actorFor("master", source._1, source._2)

  
  /**
   * overrides the start method to implement the business logic of starting, as well
   * as the remote actor registration.
   */
  override def start = {
    logger.info("starting slave actor on %s:%s".format(host, port))
    super.start
    RemoteNode.start(host, port)
    RemoteNode.register("slave", this)
    this
  }

  
  /**
   * stops this actor and shuts down the remote node
   */
  override def stop {
    logger.info("shutting down slave actor on %s:%s".format(host, port))
    super.stop
    stories.foreach { _ ! Stop }
    RemoteNode.shutdown
  }  
}