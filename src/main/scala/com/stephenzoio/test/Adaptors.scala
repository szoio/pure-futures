package com.stephenzoio.test

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.clients.{producer => kafka}

trait Adaptors {
  def kafkaCallbackAdaptor(cb: Callback[RecordMetadata]): kafka.Callback = (metadata: RecordMetadata, exception: Exception) => {
    exception match {
      case null =>
        cb.apply(Right(metadata))
      case error =>
        cb.apply(Left(error))
    }
  }

  def kafkaCallbackAdaptorF[F: Unsafe](cb: CallbackF[F, RecordMetadata]): kafka.Callback = (metadata: RecordMetadata, exception: Exception) => {
    Unsafe[F].unsafeRunSync {
      exception match {
        case null =>
          cb.apply(Right(metadata))
        case error =>
          cb.apply(Left(error))
      }
    }
  }
}
