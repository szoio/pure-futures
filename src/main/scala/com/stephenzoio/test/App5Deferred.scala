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

object App5Deferred {

  import Implicits._
  val topicName = "test.topic"
  val serializer = new StringSerializer()
  val config = Config(
    Map[String, Object](
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092"
    ),
    serializer,
    serializer
  )
  //implicit val materializer = Materializer.m
  // atFromSystem(ActorSystem("QuickStart"))
  def main(args: Array[String]): Unit =
    time {

      val producer = Producer5PureFuture.apply[Task, String, String](config)
      val runner = Observable
        .fromIterable(0 to 10000000)
        .map(i => new ProducerRecord[String, String](topicName, s"key $i", s"value $i"))
        .mapEval(producerRecord => producer.produce(producerRecord))
        .mapParallelOrdered(1000) { _.get }
        .completedL

      time {
        runner.void
          .runSyncUnsafe()
      }
    }

  private def time(thunk: => Unit): Unit = {
    val start = System.currentTimeMillis()
    thunk
    val end = System.currentTimeMillis()
    println(s"Executed in ${end - start} milliseconds")
  }
}
