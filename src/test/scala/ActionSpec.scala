package com.shorrockin.narrator

import org.specs._
import java.util.concurrent.TimeUnit

class ActionSpec extends Specification with IntervalCreator {

  "an action" can {
    val story = new Story(1, Map[String, String]())
    import story._

    "be constructed from a string and function" in {
      val action = "action description" as { println("action") }
      action.description must beEqual("action description")
      action.worker must beSome
    }

    "use fixed interval timing" in {
      val action = "fixed interval" every (5 minutes) as { println("hello") }
      action.description must beEqual("fixed interval")
      action.interval must beSome[Interval].which(_.start.equals(5))
      action.interval must beSome[Interval].which(_.end.equals(None))
      action.interval must beSome[Interval].which(_.unit.equals(TimeUnit.MINUTES))
    }

    "use ranged interval timing" in {
      val action = "ranged interval" every (2 to 7 msecs) as { println("hello") }
      action.description must beEqual("ranged interval")
      action.interval must beSome[Interval].which(_.start.equals(2))
      action.interval must beSome[Interval].which(_.end.equals(Some(7)))
      action.interval must beSome[Interval].which(_.unit.equals(TimeUnit.MILLISECONDS))
    }

    "utilize a starting range with 'in' method" in {
      // explicitly specify story.in, as the specification class also contains
      // an implicit 'in' statement.
      val action = story.in (0 to 3 minutes) execute "time started" every (2 to 8 seconds) as { println("hello")}
      action.description must beEqual("time started")
      action.start must beSome[Interval].which(_.start.equals(0))
      action.start must beSome[Interval].which(_.end.equals(Some(3)))
      action.start must beSome[Interval].which(_.unit.equals(TimeUnit.MINUTES))
    }

    "be defined as a followup action" in {
      val action = "following" after "initial" as { println("hello") }
      action.description must beEqual("following")
      action.follows must beSome[String].which(_.equals("initial"))
    }
  }
}