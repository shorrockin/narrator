package com.shorrockin.narrator

import scala.{Option, None, Some}
import org.apache.commons.cli.{Option => CliOption}
import org.apache.commons.cli._

/**
 * narrator object acts as the entry point for our application
 */ 
trait Narrator extends WorkloadGenerator {

  import OptionBuilder._
  private val defaultOptions = { withArgName("host")
                                 hasArgs()
                                 isRequired()
                                 withDescription("the host address this process should bind to")
                                 create("host") } ::
                               { withArgName("port")
                                 hasArgs()
                                 isRequired()
                                 withDescription("the port value that we should bind to")
                                 create("port") } ::
                               { withArgName("servers")
                                 hasArgs()
                                 withDescription("a list of servers that should execute our stories. if this value is not specified that this process will act as a slave process.")
                                 create("servers") } :: Nil

  private var clOpt:Option[CommandLine] = None
  private var shutdown = List[() => Unit]()

  lazy val slaves =  values("servers").map { server =>
    val split = server.split(":")
    Slave(split(0), split(1).toInt)
  }

  /**
   * main entry point for the narrator, extracts and parses the arguments
   * then starts our story execution process.
   */
  def init(args:Array[String]) {
    val opts = new Options
    defaultOptions.foreach { opts.addOption(_) }
    options.foreach        { opts.addOption(_) }

    try {
      clOpt = Some(new GnuParser().parse(opts, args))

      if (exists("servers")) startMaster()
      else startSlave()
    } catch {
      case e:Exception =>
        if (args.length == 0 || args(0).equals("-help")) displayHelp(opts)
        else throw e
    }
  }


  /**
   * performs the shutdown hooks for this app shutting down anything which has been
   * registered with it.
   */
  def stop() {
    shutdown.foreach { _() }
  }


  /**
   * starts this as a master node
   */
  def startMaster() {
    val master = new MasterActor(value("host"), value("port").toInt, slaves, this)
    shutdown = { () => { master.stop } } :: shutdown
    master.start
  }


  /**
   * starts this as a slave mode
   */
  def startSlave() {
    val slave = new SlaveActor(value("host"), value("port").toInt)
    shutdown = { () => { slave.stop } } :: shutdown
    slave.start
  }


  /**
   * displays the help text for this narrator
   */
  def displayHelp(opts:Options) {
    new HelpFormatter().printHelp("narrator", opts)
  }


  /**
   * returns the value passed in as a command line argument for the specified
   * option. throws an exception if it hasn't been init'd
   */
  def value(opt:String) = commandLine.getOptionValue(opt)


  /**
   * returns a list of all the values for the specified option
   */
  def values(opt:String) = commandLine.getOptionValues(opt)


  /**
   * true if the specified option exists, false otherwise
   */
  def exists(opt:String) = commandLine.hasOption(opt)


  /**
   * true if this is initialized
   */
  def initialized = clOpt.isDefined


  /**
   * returns the command line or the
   */
  private def commandLine = clOpt match {
    case None     => throw new IllegalStateException("command line arguments have not been parsed yet")
    case Some(cl) => cl
  }


  /**
   * override to allow additional properties to be specified from the command line.
   */
  def options:Seq[CliOption] = Nil
}


/**
 * defines the location of a slave actor
 */
case class Slave(val host:String, val port:Int)


/**
 * defines a unit of work assigned to a slave
 */
case class Workload(val story:Class[_], val startId:Int, val endId:Int, val params:Map[String, String]) {}


/**
 * simple trait used to retrieve the amount of work to give a slave server.
 */
trait WorkloadGenerator {
  def generateWorkload(slave:Slave):Seq[Workload]
}