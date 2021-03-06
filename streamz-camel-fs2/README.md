Camel DSL for FS2
-----------------

[Apache Camel endpoints](http://camel.apache.org/components.html) can be integrated into [FS2](https://github.com/functional-streams-for-scala/fs2) applications with a DSL that is provided by the `streamz-camel-fs2` artifact.

The DSL can be imported with:

```scala
import streamz.camel.fs2.dsl._
```

Its usage requires an implicit [`StreamContext`](http://krasserm.github.io/streamz/scala-2.12/unidoc/streamz/camel/StreamContext.html) in scope. A `StreamContext` uses a `CamelContext` to manage the endpoints that are created and referenced by applications. A `StreamContext` with an internally managed `CamelContext` can be created with `StreamContext()`:

```scala
import streamz.camel.StreamContext

// contains an internally managed CamelContext 
implicit val streamContext: StreamContext = StreamContext()
```

Applications that want to re-use an existing, externally managed `CamelContext` should create a `StreamContext` with  `StreamContext(camelContext: CamelContext)`: 

```scala
import org.apache.camel.CamelContext
import streamz.camel.StreamContext

// externally managed CamelContext
val camelContext: CamelContext = ...

// re-uses the externally managed CamelContext
implicit val streamContext: StreamContext = StreamContext(camelContext)
```

After usage, a `StreamContext` should be stopped with `streamContext.stop()`. 

#### Consuming from an endpoint

An FS2 stream that emits messages consumed from a Camel endpoint can be created with `receive`. Endpoints are referenced by their [endpoint URI](http://camel.apache.org/uris.html). For example,

```scala
import streamz.camel.StreamMessage

val s1: Stream[Task, StreamMessage[String]] = receive[String]("seda:q1")
```

creates an FS2 stream that consumes messages from the [SEDA endpoint](http://camel.apache.org/seda.html) `seda:q1` and converts them to `StreamMessage[String]`s. A [`StreamMessage[A]`](http://krasserm.github.io/streamz/scala-2.12/unidoc/streamz/camel/StreamMessage.html) contains a message `body` of type `A` and message `headers`. Calling `receive` with a `String` type parameter creates an FS2 stream that converts consumed message bodies to type `String` before emitting them as `StreamMessage[String]`. Type conversion internally uses a Camel [type converter](http://camel.apache.org/type-converter.html). An FS2 stream that only emits the converted message bodies can be created with `receiveBody`:

```scala
val s1b: Stream[Task, String] = receiveBody[String]("seda:q1")
```

This is equivalent to `receive[String]("seda:q1").map(_.body)`.

#### Sending to an endpoint

For sending a `StreamMessage` to a Camel endpoint, the `send` combinator should be used:

```scala
val s2: Stream[Task, StreamMessage[String]] = s1.send("seda:q2")
```

This initiates an in-only message [exchange](http://camel.apache.org/exchange.html) with an endpoint and continues the stream with the sent `StreamMessage`. 

The `send` combinator is not only available for streams of type `Stream[Task, StreamMessage[A]]` but more generally for any stream of type `Stream[Task, A]`.

```scala
val s2b: Stream[Task, String] = s1b.send("seda:q2")
```

If `A` is not a `StreamMessage`, `send` automatically wraps the message into a `StreamMessage[A]` before sending it to the endpoint and continues the stream with the unwrapped `A`.

#### Requesting from an endpoint

For requesting a reply from an endpoint to an input `StreamMessage`, the `request` combinator should be used:

```scala
val s3: Stream[Task, StreamMessage[Int]] = s2.request[Int]("bean:service?method=weight")
```

This initiates an in-out message exchange with the endpoint and continues the stream with the output `StreamMessage`. Here, a [Bean endpoint](https://camel.apache.org/bean.html) is used to call the `weight(String): Int` method on an object that is registered in the `CamelContext` under the name `service`. The input message body is used as `weight` call argument, the output message body is assigned the return value. The `receive` type parameter (`Int`) specifies the expected output value type. The output message body can also be converted to another type provided that an appropriate Camel type converter is available (`Double`, for example). 

The `request` combinator is not only available for streams of type `Stream[Task, StreamMessage[A]]` but more generally for any stream of type `Stream[Task, A]`:

```scala
val s3b: Stream[Task, Int] = s2b.request[Int]("bean:service?method=weight")
```

If `A` is not a `StreamMessage`, `request` automatically wraps the message into a `StreamMessage[A]` before sending it to the endpoint and continues the stream with the unwrapped message body `B` of the output `StreamMessage[B]`.