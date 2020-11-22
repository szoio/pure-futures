package com.stephenzoio.test

import cats.Monad
import cats.effect.{Concurrent, Sync}
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.{producer => kafka}
import scala.jdk.CollectionConverters._

trait Producer5PureFuture[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): F[PureFuture[F, RecordMetadata]]
}

object Producer5PureFuture {

  def apply[F[_]: Concurrent: Monad: Sync: Unsafe, K, V](spec: Config[K, V]): Producer5PureFuture[F, K, V] = {
    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => {
      PureFuture.apply[F, RecordMetadata] { callbackF =>
        Sync[F]
          .delay {
            val callback: kafka.Callback = (metadata: RecordMetadata, exception: Exception) =>
              Unsafe[F].unsafeRunSync {
                exception match {
                  case null =>
                    callbackF.apply(Right(metadata))
                  case error =>
                    callbackF.apply(Left(error))
                }
              }
            kafkaProducer.send(record, callback)
            ()
          }
      }
    }
  }
}
