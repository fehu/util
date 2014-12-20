import sbt.ConcurrentRestrictions.Tag
import sbt.KeyRanks._
import sbt._
import Keys._
import sbt.plugins.{IvyPlugin, CorePlugin}
import scala.util.Try

object PublishingSettings extends Plugin{
  def get = projectSettings

  override lazy val projectSettings = /*IvyPlugin.projectSettings ++ */Seq(
    // Default Local ghRepo Environment variable
    ghRepoLocalEnv := "LOCAL_GITHUB_REPO",

    ghRepoLocalFile := sys.env.get(ghRepoLocalEnv.value) map file,
    ghRepoLocalResolver := ghRepoLocalFile.value map (Resolver.file("publish-gh-repo-local", _)),
    resolvers ++= ghRepoLocalResolver.value.toSeq,
    ghPublishConfig := {
      val resolver = ghRepoLocalResolver.value getOrElse noGhRepoLocalError(ghRepoLocalEnv.value)
      Classpaths.publishConfig(packagedArtifacts.in(ghPublishLocal).value, None, resolverName = resolver.name, checksums = checksums.in(ghPublishLocal).value, logging = ivyLoggingLevel.value, overwrite = isSnapshot.value)
    },
    ghPublishLocal <<= Classpaths.publishTask(ghPublishConfig, deliverLocal),
    ghSubmit := {
      val repoDir = ghRepoLocalFile.value getOrElse noGhRepoLocalError(ghRepoLocalEnv.value)
      def runCmd(cmd: Seq[String]) = Process(cmd, repoDir)

      val message = s"updating gh-repository for project ${name.value}"
      val commitCommands = Seq(
        "git" :: "add" :: "*" :: Nil,
        "git" :: "commit" :: "-m" :: ("\"" + message + "\"") :: Nil
      )

      streams.value.log.info("## parallel = " + parallelExecution.value)
      streams.value.log.info("## " + message)
      /* commit */
      (Process("dummy", 0) /: commitCommands)((acc, cmd) => acc #&& runCmd(cmd)).!.ensuring(_ == 0, "failed to add/commit")
    },

    ghSubmit <<= ghSubmit.dependsOn(ghPublishLocal),
    ghPush := {
      val repoDir = ghRepoLocalFile.value getOrElse noGhRepoLocalError(ghRepoLocalEnv.value)
      def runCmd(cmd: Seq[String]) = Process(cmd, repoDir)

      val remote = runCmd(Seq("git", "remote")).!! match {
        case "" => sys.error("no remote repository configured")
        case name => name
      }

      runCmd(s"git push $remote gh-pages".split(" ")).ensuring(_ == 0, "failed to push")
    },

    ghPublish := {
      TaskUtils.runTasksForAllSubProjects(Project.extract(state.value).currentProject, state.value, ghPublishLocal, ghSubmit)
      Project.runTask(ghPush, state.value)
    },

    aggregate in ghPush := false,
    aggregate in ghPublish := false

  )

  val ghPublish = TaskKey[Unit]("gh-repo-publish", "publish to remote github-based repo")

  val ghRepoLocalFile = SettingKey[Option[File]]("gh-repo-local-file", "local version of github-based repo project")
  val ghRepoLocalResolver = SettingKey[Option[Resolver]]("gh-repo-local-resolver", "local version of github-based repo project")
  val ghRepoLocalEnv = SettingKey[String]("gh-repo-local-env", "Environment variable with a path to the local version of gh repo project")

  val ghSubmit = TaskKey[Unit]("gh-repo-submit", "submit repo changes")
  val ghPush = TaskKey[Unit]("gh-repo-push", "push repo changes")

  val ghPublishConfig = TaskKey[PublishConfiguration]("gh-repo-publish-config")
  val ghPublishLocal = TaskKey[Unit]("gh-repo-publish-local")

//  override def requires = IvyPlugin
//  override def trigger = allRequirements

  private def noGhRepoLocalError(envVar: String) =
    sys.error(s"local version of github-based repo project is not defined, check $envVar environment variable")
}
