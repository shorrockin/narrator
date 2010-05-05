package com.shorrockin.narrator

import utils.{BalancedWorkloadGenerator, Logging}

object SampleApplication extends Narrator with BalancedWorkloadGenerator {

  def main(args:Array[String]) = init(args)

  
  lazy val stories = (classOf[SampleStory], 10000, Map[String, String]()) :: Nil

  class SampleStory(id:Int, config:Map[String, String]) extends Story(id, config) with Logging {
    var counter = 0
    description = "narrator sample application"

    "start by saying hello" as {
      logger.debug("[%s] is starting up and saying 'hello world'".format(id))
    }

    in (0 to 20 msecs) execute "continue saying 'this is a sample application'" every (5 to 20 seconds) as {
      counter = counter + 1
      logger.debug("[%s] is executing this every 5 to 20 seconds, and have done so %s times before".format(id, counter))
      if (counter == 1) {
        throw new NarratorStoryException("thrown during first run")
      }
    }

    "finish up with a goodbye" as {
      logger.debug("[%s] is shutting down and saying 'goodbye'".format(id))
    }
  }
}