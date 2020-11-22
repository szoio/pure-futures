package com.stephenzoio.test

trait Types {
  type Attempt[A] = Either[Throwable, A]
  type Callback[-A] = Either[Throwable, A] => Unit
}
