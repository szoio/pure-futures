package com.stephenzoio.test

trait PureTypes {
  type CallbackF[F[_], -A] = Either[Throwable, A] => F[Unit]
  type CallbackHandlerF[F[_], A] = CallbackF[F, A] => F[Unit]
}
