package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.{Scheduler, Actor}
import java.util.concurrent.Executors

object StoryActor {
  // enum to define the various modes that a story actor may have.
  trait StoryMode
  case object StartMode extends StoryMode
  case object MainMode  extends StoryMode
  case object StopMode  extends StoryMode

  case class Start()
  case class Stop()
  case class Perform(action:Action)  

  val service = Executors.newSingleThreadScheduledExecutor()
}

/**
 * a story actor is responsible for scheduling and running the actions
 * defined within a story.
 *
 * @author Chris Shorrock
 */
class StoryActor(story:Story) extends Actor {
  import StoryActor._
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

  
  /**
   * called by akka to received the event
   */
  def receive = {
    case Start => process(StartMode)
    case Stop => process(StopMode)
    case Perform(action) => perform(action)
  }


  /**
   * set's and processes the specified mode, performing the actions which need to
   * be performed.
   */
  private def process(newMode:StoryMode):Unit = {
    mode = Some(newMode)
    newMode match {
      case StartMode if (startActions.size > 0) => schedule(startActions(0))
      case StartMode => process(MainMode)
      case MainMode if (mainActions.size > 0) => mainActions.foreach { schedule(_) }
      case MainMode => process(StopMode)
      case StopMode if (stopActions.size > 0) => schedule(stopActions(0))
      case StopMode => /* all done */
    }
  }


  /**
   * schedules the specified action for execution, either immediately, or within
   * the specified interval.
   */
  private def schedule(action:Action) = action.interval match {
    case None    => this ! Perform(action)
    case Some(i) => service.schedule(runnables(action), i.nextDelay, i.unit)
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
      case StopMode if (stopActions.contains(action)) => action.worker.get()
      case StopMode => /* ignore */
      case _ => action.worker.get()
    }

    // determines what to do next, if we're in start/stop mode then execute in
    // sequence, if we're in main mode, then reschedule 
    mode.get match {
      case StartMode => after(action, startActions) match {
        case None    => process(MainMode)
        case Some(a) => schedule(a) 
      }
      case MainMode  => schedule(action)
      case StopMode  => after(action, stopActions) match {
        case None    => /* nothing all done */
        case Some(a) => schedule(a)
      }
    }
  }


  class ScheduledRunnable(action:Action) extends Runnable {
    def run() = StoryActor.this ! Perform(action)
  }
}
