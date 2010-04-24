package com.shorrockin.narrator

/**
 * companion class for action which contains all the implicits necessary to
 * create actions from simple syntax.
 */
trait ActionCreator {
  implicit def stringToAction(str:String) = new Action(str)
}

/**
 * An action is executed within the context of a story.
 *
 * @author Chris Shorrock
 */
class Action(val description:String) {
  var worker:Option[() => Unit] = None
  var interval:Option[Interval] = None
  var follows:Option[String]    = None

  def as(f: => Unit)    = asChained { worker = Some({ () => f }) }
  def every(i:Interval) = asChained { interval = Some(i) }
  def after(a:String) = asChained { follows = Some(a) }

  private def asChained[E](f: => Unit) = { f ; this }
}