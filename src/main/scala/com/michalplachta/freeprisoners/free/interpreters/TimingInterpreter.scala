package com.michalplachta.freeprisoners.free.interpreters

import cats.~>
import com.michalplachta.freeprisoners.free.algebras.TimingOps.{Pause, Timing}

import scala.concurrent.Future

object TimingInterpreter extends (Timing ~> Future) {
  def apply[A](timing: Timing[A]) = timing match {
    case Pause(duration) =>
      Thread.sleep(duration.toMillis)
      Future.successful(())
  }
}