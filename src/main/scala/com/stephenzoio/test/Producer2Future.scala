package com.stephenzoio.test

import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.{producer => kafka}

import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters._

trait Producer2Future[K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): Future[RecordMetadata]
}

object Producer2Future {

  def apply[K, V](spec: Config[K, V]): Producer2Future[K, V] = {

    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => {
      val promise = Promise[RecordMetadata]()

      val callback: kafka.Callback = ???

      kafkaProducer.send(record, callback)
      promise.future
    }
  }
}
