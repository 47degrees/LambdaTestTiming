package com.fortysevendeg

import com.fortysevendeg.lambdatest._
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{ Failure, Success, Try }
import scala.language.postfixOps

/**
  * Actions used in tests.
  */
package object lambdatesttiming {

  private def timeRound(micros: Long): FiniteDuration = {
    val min = 20 // displayed count should be greater than this
    val millis = micros / 1000
    val secs = millis / 1000
    if (secs > min) {
      secs.seconds
    } else if (millis > min) {
      millis.millis
    } else {
      micros.microsecond
    }
  }

  /**
    * This action times its body and reports how long it ran.
    *
    * @param info a string to be reported
    * @param body the actions to be timed.
    * @param pos  the source position (usually defaulted).
    * @return the LambdaAct.
    */
  def timer(info: String, pos: String = srcPos())(body: ⇒ LambdaAct): LambdaAct = {
    SingleLambdaAct {
      t ⇒
        val time0 = System.nanoTime()
        val t1 = t.label(s"Start timer: $info", body, false, "")
        val time1 = System.nanoTime()
        val total = (time1 - time0) / 1000
        t1.label(s"End timer: $info [${timeRound(total)}]", exec {}, false, "")
    }
  }

  /**
    * Ths assertion runs its body multiple time and reports the mean, max and standard deviation.
    * It fails if the specified mean or max are exceeded.
    *
    * @param info    a string to be reported.
    * @param warmup  the number of time to run its body before starting timing.
    * @param repeat  the number of times to run and time the body.
    * @param mean    the assertion fails if the mean exceeds this value.
    * @param max     the assertion fails if the max exceeds this value.
    * @param timeout the maximum time to wait for the body execution to complete.
    * @param pos     the source position (usually defaulted).
    * @param body    the code to be timed.
    * @return the LambdaAct.
    */
  def assertPerf(
    info: String,
    warmup: Int = 10,
    repeat: Int = 100,
    mean: FiniteDuration = 10 millis,
    max: FiniteDuration = 15 millis,
    timeout: FiniteDuration = 1 second,
    pos: String = srcPos()
  )(body: ⇒ Unit): LambdaAct = {
    import scala.concurrent.ExecutionContext.Implicits.global

    SingleLambdaAct {
      case t ⇒
        try {
          for (i ← 1 to warmup) {
            Await.result(Future(body), timeout)
          }
          val times = for (i ← 1 to repeat) yield {
            val t0 = System.nanoTime()
            Await.result(Future(body), timeout)
            val t1 = System.nanoTime()
            (t1 - t0) / 1000
          }
          val meanMicros = times.sum / times.length
          val maxMicros = times.max
          val stdDev = Math.sqrt((times.map(_ - meanMicros)
            .map(v ⇒ v * v).sum) / times.length).toInt
          val info1 = s"[mean:${
            timeRound(meanMicros)
          } max:${timeRound(maxMicros)} stdev:${
            timeRound(stdDev)
          }] $info"

          if (meanMicros.micros > mean) {
            t.fail(s"exceeds mean $info1 $meanMicros.micros $mean", pos)
          } else if (maxMicros.micros > max) {
            t.fail(s"exceeds max $info1", pos)
          } else {
            t.success(s"$info1", pos)
          }
        } catch {
          case ex: TimeoutException ⇒ t.fail(s"timeout: $info", pos)
          case ex: Throwable ⇒ t.unExpected(ex, pos)
        }
    }
  }

  /**
    * This assertion times its body and fails if it takes longer than max.
    *
    * @param body    the actions to be timed.
    * @param info    a string to be reported.
    * @param max     the assertion fails if the body takes longer than this.
    * @param timeout the maximum time to wait for the body execution to complete.
    * @param pos     the source position (usually defaulted).
    * @return the LambdaAct.
    */
  def assertTiming(body: ⇒ LambdaAct)(
    info: String,
    max: FiniteDuration = 100 millis,
    timeout: FiniteDuration = 1 second,
    pos: String = srcPos()
  ): LambdaAct = {
    import scala.concurrent.ExecutionContext.Implicits.global

    SingleLambdaAct {
      case t ⇒
        val t0 = System.nanoTime()
        val f1 = Future(Try(body.eval(t)))
        val r = Try(Await.result(f1, timeout))
        val t1 = System.nanoTime()
        val micros = (t1 - t0) / 1000
        val elapsed = timeRound(micros)
        val part: Int = ((elapsed / max) * 100).toInt
        val times = s"$elapsed $part%"
        r match {
          case Failure(ex) ⇒ t.fail(s"exceeded timeout [$times] $info", pos)
          case Success(Success(t1: LambdaState)) ⇒
            if (micros.micros > max) {
              t1.fail(s"exceeded max [$times] $info", pos)
            } else {
              t1.success(s"[$times] $info", pos)
            }
          case Success(Failure(ex: Throwable)) ⇒ t.unExpected(ex, pos)
        }
    }
  }

}
