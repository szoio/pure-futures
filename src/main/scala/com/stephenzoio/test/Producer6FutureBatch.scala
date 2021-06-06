package com.stephenzoio.test

import java.util

import com.stephenzoio.test.Producer6FutureBatch.Result
import monix.eval.Task
import monix.execution.atomic.{Atomic, AtomicAny}
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.clients.{producer => k}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

trait Producer6FutureBatch[K, V] {

  def produce(producerRecords: Seq[k.ProducerRecord[K, V]]): Future[List[Result[K, V]]]
}

object Producer6FutureBatch {
  case class Result[K, V](record: k.ProducerRecord[K, V], value: Try[k.RecordMetadata])
  case class Error[K, V](record: k.ProducerRecord[K, V], cause: Throwable) extends Throwable {
    override def getMessage: String = cause.getMessage
  }

  def apply[K, V](spec: Config[K, V])(
      isFatal: Throwable => Boolean = _ => true
  ): Producer6FutureBatch[K, V] = {

    val kafkaProducer = new k.KafkaProducer[K, V](spec.props.asJava, spec.keySerializer, spec.valueSerializer)

    (records: Seq[k.ProducerRecord[K, V]]) => {
      val promise = Promise[List[Result[K, V]]]()

      case class ResultHolder(
          record: k.ProducerRecord[K, V],
          maybeValue: Option[Try[RecordMetadata]],
          future: util.concurrent.Future[RecordMetadata]
      )

      val atomic = Atomic(Map.empty[Int, ResultHolder])

      def cancelAll() = {
        atomic.getAndSet(Map.empty).values.foreach { holder =>
          if (holder.maybeValue.isEmpty) holder.future.cancel(true)
        }
      }

      def callback(index: Int): k.Callback =
        (metadata, exception) => {

          def update(tryResult: Try[RecordMetadata]) =
            atomic.transform { map =>
              map
                .get(index)
                .fold(map)(resultHolder => {
                  val enrichedResult = tryResult match {
                    case Failure(exception) => Failure(Error[K,V](resultHolder.record, exception))
                    case Success(value) =>Success(value)
                  }
                  map.updated(index, resultHolder.copy(maybeValue = Some(enrichedResult)))
                })
            }

          def collectResults(holders: Iterable[ResultHolder]): List[Result[K, V]] =
            holders.collect {
              case ResultHolder(record, Some(triedMetadata), _) => Result(record, triedMetadata)
            }.toList

          def tryComplete(): Unit = {
            val values = atomic.get().values
            if (values.forall(_.maybeValue.nonEmpty)) {
              promise.success(collectResults(values))
            }
          }

          exception match {
            case null =>
              update(Success(metadata))
              tryComplete()

            case error =>
              if (isFatal(error)) {
                atomic.set(Map.empty)
                promise.failure(error)
                cancelAll()
              } else {
                update(Failure(error))
                tryComplete()
              }
          }
        }

      records.zipWithIndex.foreach {
        case (record, index) =>
          val future: util.concurrent.Future[RecordMetadata] = kafkaProducer.send(record, callback(index))
          atomic.transform(_.updated(index, ResultHolder(record, None, future)))
      }

      promise.future
    }
  }

}
