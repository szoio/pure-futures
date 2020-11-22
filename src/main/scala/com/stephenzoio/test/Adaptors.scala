package com.stephenzoio.test

import org.apache.kafka.clients.producer.RecordMetadata

trait Adaptors {
  val kafkaCallbackAdaptor: KafkaCallbackAdaptor = {
    cb => (metadata: RecordMetadata, exception: Exception) =>
      {
        exception match {
          case null =>
            cb.apply(Right(metadata))
          case error =>
            cb.apply(Left(error))
        }
      }
  }

  def kafkaCallbackAdaptorF[F[_]: Unsafe]: KafkaCallbackAdaptorF[F] = {
    cb => (metadata: RecordMetadata, exception: Exception) =>
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
