package com.shorrockin.narrator

/**
 * sent from the master to a slave to register the work which needs
 * to be performed.
 */
case class RegisterWork(source:(String, String, Int), target:Slave, workload:Seq[Workload])

/**
 * sent from the slave back to the master to indicate that it is ready
 * to start executing the workload.
 */
case class ReadyToStart(source:Slave)

/**
 * sent from the master to the slave to indicate that we should start
 * executing all pending workloads.
 */
case class StartWork()