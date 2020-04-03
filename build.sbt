val scala212 = "2.12.11"
val monocleVersion = "2.0.0"
val refinedVersion = "0.9.13"
val poiVersion     = "3.14"
val specsVersion = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) =>
      "3.9.5"
    case _ =>
      "4.5.1"
  }
)

lazy val poi = Project(id = "poi", base = file(".")).settings(
  name               := "scala-poi-cats",
  organization       := "info.folone",
  scalaVersion       := scala212,
  crossScalaVersions := Seq(scala212, "2.11.12", "2.10.7", "2.13.0"),
  scalacOptions := Seq(
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-unchecked",
    "-explaintypes"
  ),
  Compile / parallelExecution   := true,
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  libraryDependencies ++= {
    Seq(
      "org.apache.poi"             % "poi"                        % poiVersion,
      "org.apache.poi"             % "poi-ooxml"                  % poiVersion,
      "org.typelevel"              %% "cats-effect"               % "2.1.2",
      "com.github.julien-truffaut" %% "monocle-core"              % monocleVersion,
      "com.github.julien-truffaut" %% "monocle-state"             % monocleVersion,
      "com.github.julien-truffaut" %% "monocle-macro"             % monocleVersion,
      "com.github.julien-truffaut" %% "monocle-unsafe"            % monocleVersion,
      "eu.timepit"                 %% "refined"                   % refinedVersion,
      "eu.timepit"                 %% "refined-cats"              % refinedVersion,
      "eu.timepit"                 %% "refined-eval"              % refinedVersion,
      "org.specs2"                 %% "specs2-core"               % specsVersion.value % Test,
      "org.specs2"                 %% "specs2-scalacheck"         % specsVersion.value % Test,
      "org.typelevel"              %% "discipline-specs2"         % "1.1.0" % Test,
      "org.typelevel"              %% "cats-laws"                 % "2.0.0" % Test,
      "org.scalacheck"             %% "scalacheck"                % "1.14.3" % Test,
      "io.chrisdavenport"          %% "cats-scalacheck"           % "0.2.0" % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3" % Test,
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      compilerPlugin(("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full))
    )
  },
  Global / onChangedBuildSource := ReloadOnSourceChanges,
  scalafmtOnCompile             := true,
  publishMavenStyle             := true,
  Test / publishArtifact        := false,
  credentials += {
    Seq("build.publish.user", "build.publish.password").map(k => Option(System.getProperty(k))) match {
      case Seq(Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
      case _ =>
        Credentials(Path.userHome / ".ivy2" / ".credentials")
    }
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("staging" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/folone/poi.scala</url>
    <licenses>
      <license>
        <name>Apache License</name>
        <url>http://opensource.org/licenses/Apache-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:folone/poi.scala.git</url>
      <connection>scm:git:git@github.com:folone/poi.scala.git</connection>
    </scm>
    <developers>
    {
      Seq(
        ("folone", "George Leontiev"),
        ("fedgehog", "Maxim Fedorov"),
        ("Michael Rans", "Michael Rans"),
        ("daneko", "Kouichi Akatsuka"),
        ("rintcius", "Rintcius Blok")
      ).map {
        case (id, name) =>
          <developer>
          <id>{id}</id>
          <name>{name}</name>
          <url>http://github.com/{id}</url>
        </developer>
      }
    }
    </developers>
  )
)
