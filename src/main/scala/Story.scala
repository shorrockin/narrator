package com.shorrockin.narrator

/**
 * a story object generally represents a user and the actions that
 * they may take.
 *
 * @author Chris Shorrock 
 */
class Story extends IntervalCreator {
  private var actions = List[Action]()
  private var _locked  = false

  private lazy val leftRight = lock { actions.span(_.interval.isEmpty) }
  lazy val startActions      = leftRight._1
  lazy val mainActions       = leftRight._2.span(_.interval.isDefined)._1
  lazy val stopActions       = leftRight._2.span(_.interval.isDefined)._2

  def locked = _locked
  def size = actions.size
  def apply(index:Int) = actions(index)
  def find(description:String) = actions.find(_.description.equals(description))
  
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



  

  
}

