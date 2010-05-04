package com.shorrockin.narrator.utils

import com.shorrockin.narrator._

/**
 * a workload generator which is balanced in how it delegates work
 * amongst the list of slaves.
 *
 * @author Chris Shorrock
 */
trait BalancedWorkloadGenerator extends WorkloadGenerator {
  this:Narrator =>
  
  /**
   * returns all the stories, the number of instances to use and a map of the
   * configuration parameters.
   */
  val stories:Seq[(Class[_], Int, Map[String, String])]


  def generateWorkload(slave:Slave):Seq[Workload] = {
    val index = slaves.indexOf(slave)

    stories.map { (story) =>
      val splitSize = story._2 / slaves.size
      val start     = splitSize * index
      val end       = start + splitSize
      Workload(story._1, start, end, story._3)
    }
  }

}
