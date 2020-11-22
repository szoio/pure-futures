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

}
