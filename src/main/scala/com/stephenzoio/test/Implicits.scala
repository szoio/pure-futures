package com.stephenzoio.test

import cats.effect.{ContextShift, IO, Timer}
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext

object Implicits {
  implicit def cs(implicit ec: ExecutionContext): ContextShift[IO] = IO.contextShift(ec)
  implicit def ti(implicit ec: ExecutionContext): Timer[IO] = IO.timer(ec)

  implicit val unsafeIO: Unsafe[IO] = new Unsafe[IO] {
    override def unsafeRunSync[A](fa: IO[A]): A = fa.unsafeRunSync()
  }

  implicit def unsafeTask(implicit scheduler: Scheduler): Unsafe[Task] =
    new Unsafe[Task] {
      override def unsafeRunSync[A](fa: Task[A]): A = fa.runSyncUnsafe()
    }

  implicit class MapModifier[K, V](map: Map[K, V]) {
    def modify(key: K, defaultValue: V)(modifier: V => V): Map[K, V] = {
      map.updated(key, modifier(map.getOrElse(key, defaultValue)))
    }
  }
}
