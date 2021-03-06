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

package streamz.camel.akka.javadsl;

import akka.NotUsed;
import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.javadsl.Source;
import streamz.camel.StreamContext;
import streamz.camel.StreamMessage;

public interface JavaDsl {
    /** Returns the {@link StreamContext} in scope. */
    StreamContext streamContext();

    /** Delegates to {@link streamz.camel.akka.scaladsl.receive scaladsl.receive} */
    default <A> Source<StreamMessage<A>, NotUsed> receive(String uri, Class<A> clazz) {
        return new JavaDslDef(streamContext()).receive(uri, clazz);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.receiveBody scaladsl.receiveBody} */
    default <A> Source<A, NotUsed> receiveBody(String uri, Class<A> clazz) {
        return new JavaDslDef(streamContext()).receiveBody(uri, clazz);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.send scaladsl.send} */
    default <A> Graph<FlowShape<StreamMessage<A>, StreamMessage<A>>, NotUsed> send(String uri, int parallelism) {
        return new JavaDslDef(streamContext()).send(uri, parallelism);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.send scaladsl.send} */
    default <A> Graph<FlowShape<StreamMessage<A>, StreamMessage<A>>, NotUsed> send(String uri) {
        return send(uri, 1);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.sendBody scaladsl.sendBody} */
    default <I> Graph<FlowShape<I, I>, NotUsed> sendBody(String uri, int parallelism) {
        return new JavaDslDef(streamContext()).sendBody(uri, parallelism);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.sendBody scaladsl.sendBody} */
    default <A> Graph<FlowShape<A, A>, NotUsed> sendBody(String uri) {
        return sendBody(uri, 1);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.request scaladsl.request} */
    default <A, B> Graph<FlowShape<StreamMessage<A>, StreamMessage<B>>, NotUsed> request(String uri, int parallelism, Class<B> clazz) {
        return new JavaDslDef(streamContext()).request(uri, parallelism, clazz);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.request scaladsl.request} */
    default <A, B> Graph<FlowShape<StreamMessage<A>, StreamMessage<B>>, NotUsed> request(String uri, Class<B> clazz) {
        return request(uri, 1, clazz);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.requestBody scaladsl.requestBody} */
    default <A, B> Graph<FlowShape<A, B>, NotUsed> requestBody(String uri, int parallelism, Class<B> clazz) {
        return new JavaDslDef(streamContext()).requestBody(uri, parallelism, clazz);
    }

    /** Delegates to {@link streamz.camel.akka.scaladsl.requestBody scaladsl.requestBody} */
    default <A, B> Graph<FlowShape<A, B>, NotUsed> requestBody(String uri, Class<B> clazz) {
        return requestBody(uri, 1 , clazz);
    }
}
