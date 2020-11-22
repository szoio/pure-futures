package com.stephenzoio.test

import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import cats.{Functor, Monad, MonadError}

trait PureFuture[F[_], A] { self =>
  def isCompleted(implicit F: Functor[F]): F[Boolean] = self.tryGet.map(_.nonEmpty)
  def tryGet: F[Option[A]]
  def get: F[A]

  def map[B](f: A => B)(implicit F: Functor[F]): PureFuture[F, B] =
    new PureFuture[F, B] {
      override def tryGet: F[Option[B]] = self.tryGet.map(_.map(f))
      override def get: F[B] = self.get.map(f)
    }

  def flatMap[B](fp: A => PureFuture[F, B])(implicit M: Monad[F]): PureFuture[F, B] =
    new PureFuture[F, B] {
      override def tryGet: F[Option[B]] =
        self.tryGet.flatMap {
          case Some(value) => fp.apply(value).tryGet
          case None => Option.empty[B].pure[F]
        }
      override def get: F[B] = self.get.flatMap { value => fp.apply(value).get }
    }
}

object PureFuture {

  def apply[F[_]: Concurrent: MonadError[*[_], Throwable], A](
      callbackHandler: CallbackHandlerF[F, A]
  ): F[PureFuture[F, A]] = {
    Deferred.tryable[F, Either[Throwable, A]].flatMap { deferred =>
      val completer: CallbackF[F, A] = deferred.complete

      callbackHandler.apply(completer) >>
        Sync[F].delay {
          new PureFuture[F, A] {
            override def tryGet: F[Option[A]] =
              deferred.tryGet.flatMap {
                case Some(value) => value.pure[F].rethrow.map(Option.apply)
                case None => Option.empty[A].pure[F]
              }

            override def get: F[A] = deferred.get.rethrow
          }
        }
    }
  }
}
