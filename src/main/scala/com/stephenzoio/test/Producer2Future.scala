package com.stephenzoio.test

import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.{producer => kafka}

import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters._

trait Producer2Future[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata]
}

object Producer2Future {

  def apply[F[_], K, V](spec: Config[K, V]): Producer2Future[F, K, V] = {

    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => {
      val promise = Promise[RecordMetadata]()

      val callback: kafka.Callback = (metadata, exception) => {
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
  }
}
