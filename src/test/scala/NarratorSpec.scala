package com.shorrockin.narrator

import org.specs.Specification
import org.apache.commons.cli.{MissingOptionException, Option => CliOption}

class NarratorSpec extends Specification {
  "narrator" should {
    "be able to parse and customize command line arguments" in {
      val narrator = new Narrator {
        override def options = new CliOption("foo", "foo", true, "description of foo here") :: Nil
        def generateWorkload(slave:Slave) = List[Workload]()
      }

      narrator.init(args("-foo foovalue -host 1.2.3.4 -port 1234"))
      narrator.stop
      
      narrator.exists("host") must beEqual(true)
      narrator.exists("blah") must beEqual(false)
      narrator.exists("foo") must beEqual(true)
      narrator.value("host") must beEqual("1.2.3.4")
      narrator.value("foo") must beEqual("foovalue")
    }

    "be able to throw an error when required parameters are not specified" in {
      val narrator = new Narrator {
        def generateWorkload(slave:Slave) = List[Workload]()
      }
      def exceptional = narrator.init(args("-port foovalue"))
      exceptional must throwA[MissingOptionException]
    }


    "be able to extract multiple slave server addresses" in {
      val narrator = new Narrator {
        def generateWorkload(slave:Slave) = List[Workload]()
      }
      narrator.init(args("-host 127.0.0.1 -port 1234 -servers 1.2.3.4:9999 5.6.7.8:8888"))
      narrator.stop
      
      val servers = narrator.values("servers")
      servers.length must beEqual(2)
      servers(0) must beEqual("1.2.3.4:9999")
      servers(1) must beEqual("5.6.7.8:8888")
    }

    "be able to display help" in {
      val narrator = new Narrator {
        override def options = new CliOption("foo", "foo", true, "description of foo here") :: Nil
        def generateWorkload(slave:Slave) = List[Workload]()
      }
      narrator.init(args("-help"))
      narrator.stop
      
      narrator.initialized must beEqual(false)
    }
    
    "start as a master node" in {}

    "start as a slave node" in {}
  }


  def args(str:String) = str.split(" ")
}