package munit

import org.scalacheck.Prop
import org.scalacheck.{Test => ScalaCheckTest}
import org.scalacheck.util.Pretty
import org.scalacheck.rng.Seed
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import munit.internal.FutureCompat._

trait ScalaCheckSuite extends FunSuite {

  def property(
      name: String
  )(body: => Prop)(implicit loc: Location): Unit = {
    property(new TestOptions(name, Set.empty, loc))(body)
  }

  def property(
      options: TestOptions
  )(body: => Prop)(implicit loc: Location): Unit = {
    test(options)(body)
  }

  // Allow property bodies of type Unit
  // This is done to support using MUnit assertions in property bodies
  // instead of returning a Boolean.
  implicit def unitToProp: Unit => Prop = _ => Prop.passed

  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ scalaCheckPropTransform

  protected def scalaCheckTestParameters = ScalaCheckTest.Parameters.default

  protected def scalaCheckPrettyParameters = Pretty.defaultParams

  protected def scalaCheckInitialSeed: String = Seed.random().toBase64

  private val scalaCheckPropTransform: TestTransform =
    new TestTransform("ScalaCheck Prop", t => {
      t.withBodyMap[TestValue](
        _.transformCompat {
          case Success(prop: Prop) => propToTry(prop, t)
          case r                   => r
        }(munitExecutionContext)
      )
    })

  private def propToTry(prop: Prop, test: Test): Try[Unit] = {
    import ScalaCheckTest._
    val seed =
      scalaCheckTestParameters.initialSeed.getOrElse(
        Seed.fromBase64(scalaCheckInitialSeed).get
      )
    val result = check(scalaCheckTestParameters, prop.useSeed(test.name, seed))
    def renderResult(r: Result) = {
      val resultMessage = Pretty.pretty(r, scalaCheckPrettyParameters)
      if (r.passed) {
        resultMessage
      } else {
        val seedMessage = s"""|Failing seed: ${seed.toBase64}
                              |You can reproduce this failure by adding this to your suite:
                              |
                              |  override val scalaCheckInitialSeed = "${seed.toBase64}"
                              |""".stripMargin
        seedMessage + "\n" + resultMessage
      }
    }

    result.status match {
      case Passed | Proved(_) =>
        println(renderResult(result))
        Success(())
      case status @ PropException(_, e: FailException, _) =>
        // Promote FailException (i.e failed assertions) to property failures
        val r = result.copy(status = Failed(status.args, status.labels))
        Failure(e.withMessage(e.getMessage() + "\n\n" + renderResult(r)))
      case _ =>
        // Fail using the test location
        Try(fail("\n" + renderResult(result))(test.location))
    }
  }

}
