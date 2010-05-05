package com.shorrockin.narrator

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import se.scalablesolutions.akka.dispatch.Dispatchers

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

}