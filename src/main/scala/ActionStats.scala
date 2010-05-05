package com.shorrockin.narrator

import java.io.Serializable
import collection.mutable.HashMap

/**
 * holds statistics related to the operation of an action
 *
 * @author Chris Shorrock
 */
@SerialVersionUID(1L) class ActionStats(val description:String) extends Serializable {
  var iterations     = 0L
  var totalTime      = 0L
  def averageTime    = noDivZero(iterations, 0) { totalTime / iterations }
  var maxTime        = 0L
  var minTime        = 0L
  var exceptions     = 0L
  var userExceptions = HashMap[String, Long]()

  private def noDivZero(test:Long, or:Long)(f: => Long):Long = if (test == 0) or else f

  /**
   * executes the specified action and processes the results
   * so that they are saved.
   */
  def gather(action:Action) = {
    val start = System.currentTimeMillis
    try {
      action.worker.get()
    } catch {
      case t:NarratorStoryException =>
        reportUserException(t.id, 1)
      case t:Throwable =>
        exceptions = exceptions + 1
        throw t
    } finally {
      val end = System.currentTimeMillis
      iterations = iterations + 1
      totalTime  = totalTime + (end - start)
      maxTime    = if (totalTime > maxTime) totalTime else maxTime
      minTime    = if (totalTime < minTime) totalTime else minTime
    }
  }

  def reportUserException(id:String, count:Long) {
    val current = userExceptions.getOrElseUpdate(id, 0L)
    userExceptions.put(id, current + count)
  }
}

/**
 * holds statistics related to the operation of a story, and all the actions
 * contained within that story.
 *
 * @author Chris Shorrock
 */
@SerialVersionUID(1L) class StoryStats(val description:String) extends Serializable {
  def this(s:Story) = this(s.description)

  var stats = List[ActionStats]()

  /**
   * executes the specified action and processes the results so that they are saved
   * within this story.
   */
  def gather(action:Action) = {
    val description = action.description
    val stat = find(description) match {
      case Some(s) => s
      case None    => val s = new ActionStats(description) ; stats = s :: stats ; s
    }

    stat.gather(action)
  }


  /**
   * finds the state for the action with the specified description.
   */
  def find(desc:String) = stats.find { _.description.equals(desc) }


  /**
   * merges the results of the story stats specified with this stat.
   */
  def merge(other:StoryStats) {
    if (other.description.equals(description)) {
      other.stats.foreach { stat =>
        find(stat.description) match {
          case None    => stats = stat :: stats
          case Some(s) =>
            s.iterations = stat.iterations + s.iterations
            s.totalTime  = stat.totalTime + s.totalTime
            s.exceptions = stat.exceptions + s.exceptions
            s.maxTime    = if (stat.maxTime > s.maxTime) stat.maxTime else s.maxTime
            s.minTime    = if (stat.minTime < s.minTime) stat.minTime else s.minTime

            stat.userExceptions.foreach { (tup) => s.reportUserException(tup._1, tup._2) }
        }
      }
    } else {
      throw new IllegalArgumentException("unable to merge story stats of two seperate descriptions %s and %s".format(description, other.description))
    }

  }
  
}
