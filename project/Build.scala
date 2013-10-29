import sbt._
import Keys._
import play.Project._
import com.github.play2war.plugin._

object ApplicationBuild extends Build {

  val appName         = "playTridion"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
      "com.tridion" % "cd_ambient" % "7.1.0-SNAPSHOT",
      "com.tridion.smarttarget.ambientdata" % "session_cartridge" % "1.1.1"
    )


  val main = play.Project(appName, appVersion, appDependencies)
    .settings(Play2WarPlugin.play2WarSettings: _*)
    .settings(
      Play2WarKeys.servletVersion := "2.5",
      credentials += Credentials(Path.userHome / ".ivy2" / ".masterbuild_credentials"),
      resolvers += "Archiva main" at "http://masterbuild01.ams.dev:8080/archiva/repository/internal",
      resolvers += "Archiva snapshots" at "http://masterbuild01.ams.dev:8080/archiva/repository/snapshots"
    )

}
