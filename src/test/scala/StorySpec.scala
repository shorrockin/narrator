package com.shorrockin.narrator

import org.specs.Specification

class StorySpec extends Specification {

  "a story" can {
    "contain many ordered actions" in {
      val story = new Story(1, Map[String, String]()) {
        "execute this action first" as { println("first") }
        "execute this after" every (3 minutes) as { println("second") }
        "finally do this" as { println("third") }
      }

      story.size must beEqual(3)
      story(0).description must beEqual("execute this action first")
      story(1).description must beEqual("execute this after")
      story(2).description must beEqual("finally do this")
    }
  }

  "a story" should {
    "detect duplication actions" in {
      def generate = new Story(1, Map[String, String]()) {
        "action a" as {}
        "action a" as {}
      }

      generate must throwA[IllegalStateException]
    }

    "split actions into start, main, and finish actions" in {
      val story = new Story(1, Map[String, String]()) {
        "first action" as {}
        "second first action" as {}
        "main action 1" every (3 msecs) as {}
        "main action 2" every (10 minutes) as {}
        "main action 3" every (20 minutes) as {}
        "end action" as {}
      }

      story.size must beEqual(6)
      story.startActions.size must beEqual(2)
      story.mainActions.size must beEqual(3)
      story.stopActions.size must beEqual(1)
      story.locked must beEqual(true)
    }

    "detect all start actions" in {
      val story = new Story(1, Map[String, String]()) {
        "first action" as {}
        "second action" as {}
      }

      story.size must beEqual(2)
      story.startActions.size must beEqual(2)
      story.mainActions.size must beEqual(0)
      story.stopActions.size must beEqual(0)
    }

    "detect no start actions" in {
      val story = new Story(1, Map[String, String]()) {
        "main action" every (3 msecs) as {}
        "other main action" every (10 minutes) as {}
      }

      story.size must beEqual(2)
      story.startActions.size must beEqual(0)
      story.mainActions.size must beEqual(2)
      story.stopActions.size must beEqual(0)
    }    
  }
}