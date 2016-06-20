name := "steen"

version := "1.0"

scalaVersion := "2.11.7"
cancelable in Global := true

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

scalacOptions += "-feature"
scalacOptions += "-language:postfixOps"
scalacOptions += "-language:implicitConversions"
