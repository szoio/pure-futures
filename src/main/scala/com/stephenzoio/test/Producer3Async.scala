package com.stephenzoio.test

import cats.effect.Async
import org.apache.kafka.clients.producer.{RecordMetadata, ProducerRecord, KafkaProducer}
import org.apache.kafka.clients.{producer => kafka}

import scala.jdk.CollectionConverters._

trait Producer3Async[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): F[RecordMetadata]
}

object Producer3Async {

  def apply[F[_]: Async, K, V](spec: Config[K, V]): Producer3Async[F, K, V] = {
    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => {
      val result: F[RecordMetadata] = Async[F].async[RecordMetadata] { cb: Callback[RecordMetadata] =>
        val kafkaCallback: kafka.Callback = (metadata: RecordMetadata, exception: Exception) => {
          exception match {
            case null =>
              cb.apply(Right(metadata))
            case error =>
              cb.apply(Left(error))
          }
        }

        kafkaProducer.send(record, kafkaCallback)
        ()
      }
      result
    }

//      (record: ProducerRecord[K, V]) =>
//        Async[F].async[RecordMetadata] { cb =>kafkaProducer.send(record, kafkaCallbackAdaptor(cb)) }
  }
}
