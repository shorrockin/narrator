package com.shorrockin.narrator

/**
 * a story object generally represents a user and the actions that
 * they may take.
 *
 * @author Chris Shorrock 
 */
class Story(val id:Int, val config:Map[String, String]) extends IntervalCreator {
  private var actions = List[Action]()
  private var _locked  = false

  var description            = this.getClass.getSimpleName
  private lazy val leftRight = lock { actions.span(_.interval.isEmpty) }
  lazy val startActions      = leftRight._1
  lazy val mainActions       = leftRight._2.span(_.interval.isDefined)._1
  lazy val stopActions       = leftRight._2.span(_.interval.isDefined)._2

  def allActions = actions
  def locked = _locked
  def size = actions.size
  def apply(index:Int) = actions(index)
  def find(description:String) = actions.find(_.description.equals(description))
  def in(start:Interval):StartingAction = StartingAction(start)

  
  /**
   * implicitly converts a string into an action and stores it in
   * this collection.
   */
  implicit def stringToAction(str:String) = locked match {
    case false => find(str) match {
      case None =>
        val action = new Action(str)
        actions =  actions ::: List(action)
        action
      case Some(_) => throw new IllegalStateException("action must contain a unique description")
    }
    case true => throw new IllegalStateException("actions cannot be added once a story has been locked")
  }


  /**
   * locks this story and exectues the action
   */
  private def lock[E](f: => E):E = this.synchronized { _locked = true ; f }


  /**
   * a partial action that is used as a building blocks for executing further actions.
   */
  case class StartingAction(start:Interval) {
    def execute(desc:String) = stringToAction(desc).startingIn(start)
  }

}


