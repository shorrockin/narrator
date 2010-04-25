package com.shorrockin.narrator

import org.specs.Specification

class StoryActorSpec extends Specification {
  "a story actor" can {
    "detect invalid action order declarations" in {
      val story = new Story {
        "startup" as {}
        "interval 1" every (3 minutes) as {}
        "wrong" as {}
        "interval 2" every (2 minutes) as {}
      }

      def error = new StoryActor(story)
      error must throwA[IllegalArgumentException]
    }
    
    "scheduled interval based actions" in {}
    "re-schedule interval based actions after execution" in {}
  }
}