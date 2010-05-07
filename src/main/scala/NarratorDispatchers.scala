package com.shorrockin.narrator

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import se.scalablesolutions.akka.dispatch.Dispatchers
import se.scalablesolutions.akka.actor.Actor

object NarratorDispatchers {
  var poolSize = 32

  lazy val storyDispatcher = {
    val out = Dispatchers.newExecutorBasedEventDrivenDispatcher("slave-driver")
    out.withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity
       .setCorePoolSize(poolSize)
       .setMaxPoolSize(128)
       .setKeepAliveTimeInMillis(60000)
       .setRejectionPolicy(new CallerRunsPolicy)
       .buildThreadPool
    out
  }

  /**
   * there's a bug (than oddly enough I reported in Jan 10) that meant
   * that when a dispatchers reference count reached zero it was shutdown
   * it looks like this is still present in this version of akka so we'll
   * create a dormant reference to the dispatcher so this is not the case.
   *
   * @see http://groups.google.com/group/akka-user/browse_thread/thread/86ae6b67a048b802?fwc=2
   */
  private val ref = ReferenceActor
  private object ReferenceActor extends Actor {
    dispatcher = storyDispatcher
    def receive = {
      case _ => /* ignore */
    }
  }


}