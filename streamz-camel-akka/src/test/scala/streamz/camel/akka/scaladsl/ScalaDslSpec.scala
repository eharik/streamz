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

package streamz.camel.akka.scaladsl

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }

import org.apache.camel.TypeConversionException
import org.apache.camel.impl.{ DefaultCamelContext, SimpleRegistry }
import org.scalatest._

import streamz.camel.StreamContext

import scala.collection.immutable.Seq
import scala.concurrent._
import scala.concurrent.duration._

object ScalaDslSpec {
  implicit class AwaitHelper[A](f: Future[A]) {
    def await: A = Await.result(f, 3.seconds)
  }
}

class ScalaDslSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  import ScalaDslSpec._

  val camelRegistry = new SimpleRegistry
  val camelContext = new DefaultCamelContext()

  camelContext.setRegistry(camelRegistry)
  camelRegistry.put("service", new Service)

  implicit val streamContext = new StreamContext(camelContext)
  implicit val actorSystem = ActorSystem("test")
  implicit val actorMaterializer = ActorMaterializer()

  import streamContext._

  override protected def beforeAll(): Unit = {
    camelContext.start()
    streamContext.start()
  }

  override protected def afterAll(): Unit = {
    streamContext.stop()
    camelContext.stop()
    actorSystem.terminate()
  }

  class Service {
    def plusOne(i: Int): Int =
      if (i == -1) throw new Exception("test") else i + 1
  }

  "receive" must {
    "create a source" in {
      1 to 3 foreach { i => producerTemplate.sendBody("seda:q1", i) }
      receiveBody[Int]("seda:q1").take(3).runWith(Sink.seq[Int]).await should be(Seq(1, 2, 3))
    }
    "complete with an error if type conversion fails" in {
      producerTemplate.sendBody("seda:q2", "a")
      intercept[TypeConversionException](receiveBody[Int]("seda:q2").runWith(Sink.ignore).await)
    }
  }

  "send" must {
    "send to an endpoint and continue with the sent message" in {
      val result = Source(Seq(1, 2, 3)).send("seda:q3").take(3).runWith(Sink.seq[Int])
      1 to 3 foreach { i => consumerTemplate.receiveBody("seda:q3") should be(i) }
      result.await should be(Seq(1, 2, 3))
    }
  }

  "request" must {
    "request from an endpoint and continue with the response message" in {
      Source(Seq(1, 2, 3)).request("bean:service?method=plusOne").runWith(Sink.seq[Int]).await should be(Seq(2, 3, 4))
    }
    "convert response message types using a Camel type converter" in {
      Source(Seq(1, 2, 3)).request[String]("bean:service?method=plusOne").runWith(Sink.seq[String]).await should be(Seq("2", "3", "4"))
    }
    "complete with an error if the request fails" in {
      intercept[Exception](Source(Seq(-1, 2, 3)).request("bean:service?method=plusOne").runWith(Sink.ignore).await).getMessage should be("test")
    }
  }
}
