package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.{Actor}
import java.util.concurrent.{ScheduledThreadPoolExecutor}
import utils.UniqueId

object Scheduler extends ScheduledThreadPoolExecutor(8) 

trait StoryMode
case object StartMode extends StoryMode
case object MainMode  extends StoryMode
case object StopMode  extends StoryMode


case object Start
case object Stop
case class RegisterSlave(slave:SlaveActor)
case class Perform(action:Action)
case class StoryStatsReport(val stats:StoryStats)
case class WorkloadStatsReport(val stats:Seq[StoryStats])

/**
 * a story actor is responsible for scheduling and running the actions
 * defined within a story.
 *
 * @author Chris Shorrock
 */
class StoryActor(val story:Story) extends Actor with UniqueId {
  import story._

  startActions.foreach { action =>
    if(action.interval.isDefined) throw new IllegalArgumentException("start actions may not define an interval")
    if(action.worker.isEmpty) throw new IllegalArgumentException("all actions must contain an executable block")
  }

  mainActions.foreach { action =>
    if(action.interval.isEmpty) throw new IllegalArgumentException("main actions must define an interval")
    if(action.worker.isEmpty) throw new IllegalArgumentException("all actions must contain an executable block")
  }

  stopActions.foreach  { action =>
    if(action.interval.isDefined) throw new IllegalArgumentException("stop actions may not define an interval")
    if(action.worker.isEmpty) throw new IllegalArgumentException("all actions must contain an executable block")
  }

  private val runnables = Map(mainActions.map { (a) => a -> new ScheduledRunnable(a) }:_*)
  private var mode:Option[StoryMode] = None
  private var slave:Option[SlaveActor] = None
  private val stats = new StoryStats(story)
  private var statsSent = false

  
  /**
   * called by akka to received the event
   */
  def receive = {
    case RegisterSlave(s) => slave = Some(s)
    case Start            => process(StartMode)
    case Stop             => process(StopMode)
    case Perform(action)  => perform(action)
  }


  /**
   * set's and processes the specified mode, performing the actions which need to
   * be performed.
   */
  private def process(newMode:StoryMode):Unit = {
    mode = Some(newMode)
    newMode match {
      case StartMode if (startActions.size > 0) => schedule(startActions(0), true)
      case StartMode => process(MainMode)
      case MainMode if (mainActions.size > 0) => mainActions.foreach { schedule(_, true) }
      case MainMode => process(StopMode)
      case StopMode if (stopActions.size > 0) => schedule(stopActions(0), true)
      case StopMode => sendStatisticsToSlave()
    }
  }


  /**
   * schedules the specified action for execution, either immediately, or within
   * the specified interval.
   */
  private def schedule(action:Action, initial:Boolean) = action.interval match {
    case None    => this ! Perform(action)
    case Some(i) => (initial && action.start.isDefined) match {
      // if this is our initial run we should execute in accordance with our
      // start parameter, otherwise use the delayed parameter.
      case true  =>
        val start = action.start.get
        Scheduler.schedule(runnables(action), start.nextDelay, start.unit)
      case false =>
        Scheduler.schedule(runnables(action), i.nextDelay, i.unit)
    }

  }


  /**
   * performs the specified action.
   */
  private def perform(action:Action) = {
    def after(action:Action, lst:List[Action]):Option[Action] = lst.indexOf(action) match {
      case -1 => None
      case i if ((i + 1) == lst.size) => None
      case i => Some(lst(i + 1))
    }

    // ignore the action if we're in stop mode and it's not in stop mode action
    mode.get match {
      case StopMode if (stopActions.contains(action)) => stats.gather(action)
      case StopMode => /* ignore */
      case _ => stats.gather(action)
    }

    // determines what to do next, if we're in start/stop mode then execute in
    // sequence, if we're in main mode, then reschedule 
    mode.get match {
      case StartMode => after(action, startActions) match {
        case None    => process(MainMode)
        case Some(a) => schedule(a, true) 
      }
      case MainMode  => schedule(action, false)
      case StopMode  => after(action, stopActions) match {
        case None    => sendStatisticsToSlave()
        case Some(a) => schedule(a, true)
      }
    }
  }


  private def sendStatisticsToSlave() {
    if (!statsSent) {
      slave match {
        case Some(s) => s ! StoryStatsReport(stats) ; stop
        case None => stop
      }
      statsSent = true
    }
  }


  class ScheduledRunnable(action:Action) extends Runnable {
    def run() = StoryActor.this ! Perform(action)
  }
}
