import sbt._

class NarratorProject(info:ProjectInfo) extends DefaultProject(info) {
  val shorrockin = "Shorrockin Repository" at "http://maven.shorrockin.com"

  // taken from the akka project, as the pom in the repo does not appear to
  // include them.
  val sunjdmk = "sunjdmk" at "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo"
  val databinder = "DataBinder" at "http://databinder.net/repo"
  val configgy = "Configgy" at "http://www.lag.net/repo"
  val codehaus = "Codehaus" at "http://repository.codehaus.org"
  val codehaus_snapshots = "Codehaus Snapshots" at "http://snapshots.repository.codehaus.org"
  val jboss = "jBoss" at "http://repository.jboss.org/maven2"
  val guiceyfruit = "GuiceyFruit" at "http://guiceyfruit.googlecode.com/svn/repo/releases/"
  val google = "Google" at "http://google-maven-repository.googlecode.com/svn/repository"
  val java_net = "java.net" at "http://download.java.net/maven/2"
  val scala_tools_snapshots = "scala-tools snapshots" at "http://scala-tools.org/repo-snapshots"
  val scala_tools_releases = "scala-tools releases" at "http://scala-tools.org/repo-releases"
}