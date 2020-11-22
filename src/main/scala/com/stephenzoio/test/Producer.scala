package com.stephenzoio.test

import cats.Monad
import cats.effect.concurrent.Deferred
import cats.effect.{Async, Concurrent, Sync}
import cats.implicits._
import org.apache.kafka.clients.producer._

import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters._

trait Producer[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): RecordMetadata
  def produceToFuture(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata]
  def produceAsync(producerRecord: ProducerRecord[K, V]): F[RecordMetadata]
  def produceToDeferred(producerRecord: ProducerRecord[K, V]): F[Deferred[F, Either[Throwable, RecordMetadata]]]
}

object Producer {

  def apply[F[_]: Async: Concurrent: Monad: Unsafe, K, V](spec: Config[K, V]): Producer[F, K, V] = {
    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    new Producer[F, K, V] {

      override def produce(record: ProducerRecord[K, V]): RecordMetadata = {
        kafkaProducer.send(record).get()
      }

      override def produceToFuture(record: ProducerRecord[K, V]): Future[RecordMetadata] = {
        val promise = Promise[RecordMetadata]()

        val callback: Callback = (metadata, exception) => {
          exception match {
            case null =>
              promise.success(metadata)
            case error =>
              promise.failure(error)
          }
        }

        kafkaProducer.send(record, callback)
        promise.future
      }

      override def produceAsync(record: ProducerRecord[K, V]): F[RecordMetadata] = {
        val result: F[RecordMetadata] = Async[F].async[RecordMetadata] { cb =>
          val callback: Callback = (metadata: RecordMetadata, exception: Exception) => {
            exception match {
              case null =>
                cb.apply(Right(metadata))
              case error =>
                cb.apply(Left(error))
            }
          }

          kafkaProducer.send(record, callback)
          ()
        }
        result
      }

      override def produceToDeferred(
          record: ProducerRecord[K, V]
      ): F[Deferred[F, Either[Throwable, RecordMetadata]]] = {
        for {
          deferred <- Deferred[F, Either[Throwable, RecordMetadata]]
          callback: Callback = (metadata: RecordMetadata, exception: Exception) => {
            implicitly[Unsafe[F]].unsafeRunSync(
              deferred.complete(if (exception != null) Left(exception) else Right(metadata))
            )
          }
          _ <- Sync[F].delay {
            kafkaProducer.send(record, callback)
          }
        } yield deferred
      }
    }
  }
}
