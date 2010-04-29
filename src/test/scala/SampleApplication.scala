package com.shorrockin.narrator

import _root_.utils.BalancedWorkloadGenerator
import utils.Logging

object SampleApplication extends Narrator with BalancedWorkloadGenerator {

  def main(args:Array[String]) = init(args)
  
  lazy val stories = (classOf[SampleStory], 1000, Map[String, String]()) :: Nil

  class SampleStory(id:Int, config:Map[String, String]) extends Story(id, config) with Logging {
    "start by saying hello" as {
      logger.debug("hello world!")
    }

    "continue saying 'this is a sample application'" every (5 to 20 seconds) as {
      logger.debug("this is a sample application")
    }

    "finish up with a goodbye" as {
      logger.debug("goodbye!")
    }
  }
}