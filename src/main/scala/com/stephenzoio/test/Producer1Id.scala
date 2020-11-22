package com.stephenzoio.test

import org.apache.kafka.clients.producer._

import scala.jdk.CollectionConverters._

trait Producer1Id[F[_], K, V] {
  def produce(producerRecord: ProducerRecord[K, V]): RecordMetadata
}

object Producer1Id {
  def apply[F[_], K, V](spec: Config[K, V]): Producer1Id[F, K, V] = {
    val kafkaProducer = new KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (record: ProducerRecord[K, V]) => kafkaProducer.send(record).get()
  }
}
