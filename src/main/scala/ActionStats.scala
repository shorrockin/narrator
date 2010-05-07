package com.shorrockin.narrator

import java.io.Serializable
import collection.mutable.HashMap
import utils.Logging

object ActionStats extends Logging {
}

/**
 * holds statistics related to the operation of an action
 *
 * @author Chris Shorrock
 */
@SerialVersionUID(1L) class ActionStats(val description:String) extends Serializable {
  import ActionStats._

  var iterations     = 0L
  var totalTime      = 0L
  var maxTime        = 0L
  var minTime        = 0L
  var exceptions     = 0L
  var userExceptions = HashMap[String, Long]()

  private def iterFloat = iterations.asInstanceOf[Float]

  def userExceptionCount = userExceptions.foldLeft(0L) { _ + _._2 }
  def totalExceptions    = exceptions + userExceptionCount
  def averageTime        = noDivZero(iterations, 0F)  { totalTime / iterFloat }
  def successRate        = noDivZero(iterations, -1F) { ((iterations - totalExceptions) / iterFloat) * 100 }
  def totalExceptionRate = noDivZero(iterations, -1F) { (totalExceptions / iterFloat) * 100 }
  def exceptionRate      = noDivZero(iterations, -1F) { (exceptions / iterFloat) * 100 }
  def userExceptionRate  = noDivZero(iterations, -1F) { (userExceptionCount / iterFloat) * 100 }
  def requestRatePerSec  = noDivZero(iterations, 0F)  { (iterFloat / (totalTime / 1000F)) }

  private def noDivZero[E](test:Long, or:E)(f: => E):E = if (test == 0) or else f

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
        logger.error("unexpected exception in story actor: " + t.getMessage, t)
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


@SerialVersionUID(1L) class WorkloadStats extends Serializable {
  var stats = List[StoryStats]()

  def merge(other:StoryStats):WorkloadStats = {
    stats.find { ss => ss.description.equals(other.description) } match {
      case None    => stats = other :: stats
      case Some(i) => i.merge(other)
    }
    this
  }

  def merge(other:WorkloadStats):WorkloadStats = {
    merge(other.stats)
  }

  def merge(other:List[StoryStats]):WorkloadStats = {
    other.foreach { merge(_) }
    this
  }
}
