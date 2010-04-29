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

      narrator.init(args("-foo foovalue -host 127.0.0.1 -port 9876"))
      narrator.stop
      
      narrator.exists("host") must beEqual(true)
      narrator.exists("blah") must beEqual(false)
      narrator.exists("foo") must beEqual(true)
      narrator.value("host") must beEqual("127.0.0.1")
      narrator.value("foo") must beEqual("foovalue")
    }

    "be able to throw an error when required parameters are not specified" in {
      val narrator = new Narrator {
        def generateWorkload(slave:Slave) = List[Workload]()
      }
      def exceptional = narrator.init(args("-port foovalue"))
      exceptional must throwA[MissingOptionException]
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
  }


  def args(str:String) = str.split(" ")
}