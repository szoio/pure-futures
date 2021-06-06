package com.stephenzoio.test

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import cats.effect.ExitCode
import cats.implicits._
import monix.eval.{Fiber, Task, TaskApp}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import scala.concurrent.duration._

object App6Batch {

  import Implicits._
  val topicName = "test.topic"
  val serializer = new StringSerializer()
  val config = Config(
    Map[String, Object](
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092",
      ProducerConfig.RETRIES_CONFIG -> "10",
      ProducerConfig.RETRY_BACKOFF_MS_CONFIG -> "300"
    ),
    serializer,
    serializer
  )

  def main(args: Array[String]): Unit = {
      val producer: Producer6FutureBatch[String, String] = Producer6FutureBatch.apply[String, String](config)()
      val runner = Observable
        .fromIterable(0 to 1000000)
        .map(i => new ProducerRecord[String, String](topicName, s"key ${i % 1}", s"key ${i % 100} value $i"))
        .bufferTimedWithPressure(10.millis, kafkaMaxRequestSize, messageSize)
        .mapEval(batch => Task(producer.produce(batch)))
        .mapParallelOrdered(30)(batch => Task.deferFuture(batch))
        .flatTap(meta => Observable.delay(println(s"${meta.length} records.")))
        .completedL

      time {
        runner.void.runSyncUnsafe()
      }
    }

  private def messageSize(message: ProducerRecord[String, String]): Int =
    (message.key.length + message.value.length).toInt
  private val kafkaMaxRequestSize = 10000000

  private def time(thunk: => Unit): Unit = {
    val start = System.currentTimeMillis()
    thunk
    val end = System.currentTimeMillis()
    println(s"Executed in ${end - start} milliseconds")
  }
}
