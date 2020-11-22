package com.stephenzoio.test

trait Unsafe[F[_]] {
  def unsafeRunSync[A](fa: F[A]): A
}

object Unsafe {
  def apply[F[_]: Unsafe]: Unsafe[F] = implicitly[Unsafe[F]]
}
