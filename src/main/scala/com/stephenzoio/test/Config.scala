package com.stephenzoio.test

import org.apache.kafka.common.serialization.Serializer

final case class Config[K, V](
    props: Map[String, Object],
    keySerializer: Serializer[K],
    valueSerializer: Serializer[V]
)
