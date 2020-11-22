package com.stephenzoio.test

import cats.Monad
import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.{producer => kafka}

import scala.jdk.CollectionConverters._

trait Producer4Deferred[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): F[Deferred[F, Attempt[RecordMetadata]]]
}

object Producer4Deferred {

  def apply[F[_]: Concurrent: Monad: Unsafe, K, V](spec: Config[K, V]): Producer4Deferred[F, K, V] = {
    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => {
      Deferred.apply[F, Attempt[RecordMetadata]].flatMap { deferred =>
        val callback: kafka.Callback = (metadata: RecordMetadata, exception: Exception) =>
          Unsafe[F].unsafeRunSync {
            exception match {
              case null =>
                deferred.complete(Right(metadata))
              case error =>
                deferred.complete(Left(error))
            }
          }

        Sync[F]
          .delay(kafkaProducer.send(record, callback))
          .map(_ => deferred)
      }
    }
  }
}
