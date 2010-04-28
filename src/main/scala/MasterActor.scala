package com.shorrockin.narrator

import se.scalablesolutions.akka.remote.RemoteNode
import se.scalablesolutions.akka.actor.Actor

/**
 * defines the location of a slave actor
 */
case class Slave(val host:String, val port:Int)


/**
 * defines a unit of work assigned to a slave
 */
case class Workload(val story:Class[Story], val ids:Range, val params:Map[String, String])


/**
 * simple trait used to retrieve the amount of work to give a slave server.
 */
trait SlaveWorkloadGenerator {
  def generateWorkload(slave:Slave):Seq[Workload]
}


/**
 * the master actor acts as the coordinator of the tests, communicating
 * with all slaves and scheduling the work that each of them need to
 * perform.
 *
 * @author Chris Shorrock
 */
class MasterActor(host:String, port:Int, slaves:Seq[Slave], workGenerator:SlaveWorkloadGenerator) extends Actor {

  /**
   * called by akka to received the event
   */
  def receive = {
    case "meep" =>
  }


  /**
   * overrides the start method to implement the business logic of starting, as well
   * as the remote actor registration.
   */
  override def start = {
    super.start
    RemoteNode.start(host, port)
    RemoteNode.register("master", this)
    this
  }


  /**
   * stops this actor and shuts down the remote node
   */
  override def stop {
    super.stop
    RemoteNode.shutdown
  }


}