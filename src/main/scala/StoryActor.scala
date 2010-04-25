package com.shorrockin.narrator

import se.scalablesolutions.akka.actor.Actor

/**
 * a story actor is responsible for scheduling and running the actions
 * defined within a story.
 *
 * @author Chris Shorrock
 */
class StoryActor(story:Story) extends Actor {
  story.startActions.foreach { action =>
    if(action.interval.isDefined) throw new IllegalArgumentException("start actions may not define an interval") 
  }

  story.mainActions.foreach { action =>
    if(action.interval.isEmpty) throw new IllegalArgumentException("main actions must define an interval") 
  }

  story.stopActions.foreach  { action =>
    if(action.interval.isDefined) throw new IllegalArgumentException("stop actions may not define an interval")
  }
  

  def receive = {
    case Start => scheduleStartActions()
    case Stop => scheduleStopActions()
    case Perform(action) => perform(action)
  }


  def scheduleStartActions() {
  }


  def scheduleStopActions() {
  }


  def perform(action:Action) {
  }
}

case class Start()
case class Stop()
case class Perform(action:Action)