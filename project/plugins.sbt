// Version management plugin
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

// Coverage report
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.7")

// Check code style
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
// Autoformat code
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")

// Publishing
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
