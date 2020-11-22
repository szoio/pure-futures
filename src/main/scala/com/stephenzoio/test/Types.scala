package com.stephenzoio.test
import org.apache.kafka.clients.{producer => kafka}

trait Types {
  type Attempt[A] = Either[Throwable, A]
  type Callback[-A] = Either[Throwable, A] => Unit

  type KafkaCallbackAdaptor = Callback[kafka.RecordMetadata] => kafka.Callback
}
