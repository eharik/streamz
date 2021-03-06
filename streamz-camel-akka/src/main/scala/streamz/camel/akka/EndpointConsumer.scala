/*
 * Copyright 2014 - 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package streamz.camel.akka

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request

import streamz.camel.{ StreamContext, StreamMessage }

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

private[akka] object EndpointConsumer {
  case object ConsumeTimeout
  case class ConsumeSuccess(m: Any)
  case class ConsumeFailure(t: Throwable)

  def props[A](uri: String)(implicit streamContext: StreamContext, tag: ClassTag[A]): Props =
    Props(new EndpointConsumer[A](uri))
}

private[akka] class EndpointConsumer[A](uri: String)(implicit streamContext: StreamContext, tag: ClassTag[A]) extends ActorPublisher[StreamMessage[A]] {
  import EndpointConsumer._

  def waiting: Receive = {
    case r: Request =>
      consume()
      context.become(consuming)
  }

  def consuming: Receive = {
    case ConsumeSuccess(m) =>
      onNext(m.asInstanceOf[StreamMessage[A]])
      if (!isCanceled && totalDemand > 0) consume() else context.become(waiting)
    case ConsumeTimeout =>
      if (!isCanceled) consume()
    case ConsumeFailure(e) =>
      onError(e)
  }

  def receive = waiting

  private def consume()(implicit tag: ClassTag[A]): Unit = {
    import streamContext._
    Try(consumerTemplate.receive(uri, 500)) match {
      case Success(null) =>
        self ! ConsumeTimeout
      case Success(ce) if ce.getException != null =>
        self ! ConsumeFailure(ce.getException)
        consumerTemplate.doneUoW(ce)
      case Success(ce) =>
        Try(StreamMessage.from[A](ce.getIn)) match {
          case Success(m) => self ! ConsumeSuccess(m)
          case Failure(e) => self ! ConsumeFailure(e)
        }
        consumerTemplate.doneUoW(ce)
      case Failure(ex) =>
        self ! ConsumeFailure(ex)
    }
  }
}
