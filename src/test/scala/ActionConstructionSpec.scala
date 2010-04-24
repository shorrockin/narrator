package com.shorrockin.narrator

import org.specs._
import java.util.concurrent.TimeUnit

class ActionConstructionSpec extends Specification with ActionCreator with IntervalCreator {
  
  "an action can be constructed from a string and function" in {
    val action = "action description" as { println("action") }
    action.description must beEqual("action description")
    action.worker must beSome
  }

  "an action can use fixed interval timing" in {
    val action = "fixed interval" every (5 minutes) as { println("hello") }
    action.description must beEqual("fixed interval")
    action.interval must beSome[Interval].which(_.start.equals(5))
    action.interval must beSome[Interval].which(_.end.equals(None))
    action.interval must beSome[Interval].which(_.unit.equals(TimeUnit.MINUTES))
  }

  "an action can use ranged interval timing" in {
    val action = "ranged interval" every (2 to 7 msecs) as { println("hello") }
    action.description must beEqual("ranged interval")
    action.interval must beSome[Interval].which(_.start.equals(2))
    action.interval must beSome[Interval].which(_.end.equals(Some(7)))
    action.interval must beSome[Interval].which(_.unit.equals(TimeUnit.MILLISECONDS))
  }
  
  "an action be defined as a followup action" in {
    val action = "following" after "initial" as { println("hello") }
    action.description must beEqual("following")
    action.follows must beSome[String].which(_.equals("initial"))
  }
}