package utils

import se.scalablesolutions.akka.actor.Actor
import java.util.UUID

trait UniqueId {
  this:Actor =>
  id = UUID.randomUUID.toString
}