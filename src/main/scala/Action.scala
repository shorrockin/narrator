package com.shorrockin.narrator

/**
 * An action is executed within the context of a story.
 *
 * @author Chris Shorrock
 */
class Action(val description:String) {
  var worker:Option[() => Unit] = None

  def as(f: => Unit):Unit = worker = Some({ () => f })
}