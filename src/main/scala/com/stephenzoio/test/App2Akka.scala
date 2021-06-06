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

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object App2Akka {

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
  implicit val materializer = Materializer.matFromSystem(ActorSystem("QuickStart"))

  def main(args: Array[String]): Unit = {
    val producer = Producer2Future.apply[String, String](config)
    val runner = Source(0 to 1000000)
      .map(i => new ProducerRecord[String, String](topicName, s"key $i", s"value $i"))
      .mapAsync(1000)(producerRecord => producer.produce(producerRecord))
      .runFold(0)((x, _) => x + 1)

    time {
      println(Await.result(runner, Duration.Inf))
    }
  }


  private def time(thunk: => Unit): Unit = {
    val start = System.currentTimeMillis()
    thunk
    val end = System.currentTimeMillis()
    println(s"Executed in ${end - start} milliseconds")
  }
}
