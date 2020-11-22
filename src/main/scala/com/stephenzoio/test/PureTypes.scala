package com.stephenzoio.test
import org.apache.kafka.clients.{producer => kafka}

trait PureTypes {
  type CallbackF[F[_], -A] = Either[Throwable, A] => F[Unit]
  type CallbackHandlerF[F[_], A] = CallbackF[F, A] => F[Unit]

  type CallbackAdaptorF[F[_], A] = CallbackF[F, A] => kafka.Callback
}
