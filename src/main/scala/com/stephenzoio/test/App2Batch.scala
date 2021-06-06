package com.stephenzoio.test

import cats.implicits._
import com.stephenzoio.test.App6Batch.topicName
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future
import scala.concurrent.duration._

object App2Batch {
  val topicName = "test.topic"
  val serializer = new StringSerializer()
  val config = Config(
    Map[String, Object](
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092"
    ),
    serializer,
    serializer
  )

  def main(args: Array[String]): Unit = {
      val producer: Producer2Future[String, String] = Producer2Future.apply[String, String](config)
      val runner = Observable
        .fromIterable(0 to 1000000)
        .map(i => new ProducerRecord[String, String](topicName, s"key ${i % 3}", s"key ${i % 3} value $i"))
        .bufferTimedWithPressure(5.millis, kafkaMaxRequestSize, messageSize)
        .map(batch => Future.traverse(batch)(record => producer.produce(record)))
        .mapParallelOrdered(60)(batch => Task.deferFuture(batch))
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
