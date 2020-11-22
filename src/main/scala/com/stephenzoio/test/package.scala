package com.stephenzoio

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.clients.{producer => kafka}

package object test {
  type Attempt[A] = Either[Throwable, A]
  type Callback[-A] = Either[Throwable, A] => Unit

  type CallbackF[F[_], -A] = Either[Throwable, A] => F[Unit]
  type CallbackHandlerF[F[_], A] = CallbackF[F, A] => F[Unit]

  def kafkaCallbackAdaptor(cb: Callback[RecordMetadata]): kafka.Callback = (metadata: RecordMetadata, exception: Exception) => {
    exception match {
      case null =>
        cb.apply(Right(metadata))
      case error =>
        cb.apply(Left(error))
    }
  }
}
