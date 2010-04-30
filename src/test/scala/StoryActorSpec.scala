package com.shorrockin.narrator

import org.specs.Specification

class StoryActorSpec extends Specification {
  "a story actor" can {
    "detect invalid action order declarations" in {
      val story = new Story(1, Map[String, String]()) {
        "startup" as {}
        "interval 1" every (3 minutes) as {}
        "wrong" as {}
        "interval 2" every (2 minutes) as {}
      }

      def error = new StoryActor(story)
      error must throwA[IllegalArgumentException]
    }

    "utilize start parameter of action to determine start time" in {
      var counter = 0

      val story = new Story(1, Map[String, String]()) {
        this.in (3 msecs) execute "start time execution test" every (30 minutes) as { counter = counter + 1 }
      }

      val actor = new StoryActor(story)
      actor.start
      actor.!(Start)(None)
      Thread.sleep(20)                                                                                                                                       
      counter must beEqual(1)
    }
    
    "scheduled and re-schedule interval based actions" in {
      var counter = Map[String, Int]()
      def inc(str:String) { counter = counter + (str -> (counter.getOrElse(str, 0) + 1)) }

      val story = new Story(1, Map[String, String]()) {
        "startup 1" as { inc("startup 1") }
        "startup 2" as { inc("startup 2") }

        // explicitly declare this.in so it doesn't get confused between
        // the implicit 'in' also in the specification
        this.in (0 to 3 msecs) execute "interval 1" every (10 msecs) as { inc("interval 1") }
        "interval 2" every (5 to 20 msecs) as { inc("interval 2") }

        "shutdown 1" as { inc("shutdown 1") }
        "shutdown 2" as { inc("shutdown 2") }
      }
      val actor = new StoryActor(story)
      actor.start

      actor.!(Start)(None)
      Thread.sleep(100)

      actor.!(Stop)(None)
      Thread.sleep(20)

      counter("startup 1") must beEqual(1)
      counter("startup 2") must beEqual(1)

      counter("interval 1") must beGreaterThan(2)
      counter("interval 2") must beGreaterThan(2)

      counter("shutdown 1") must beEqual(1)
      counter("shutdown 2") must beEqual(1)      
    }
  }
}