package com.appsdeveloperblog.reactive.ws.users;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class SinkExampleApplication {

    public static void main(String[] args) {

        // Create a sink that can emit many elements
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        // Get a Flux from the sink
        Flux<String> flux = sink.asFlux();

        // Subscribe to the Flux
        flux.subscribe(data -> System.out.println("flux as subscriber 1 received: " + data));
        flux.subscribe(data -> System.out.println("flux as subscriber 2 received: " + data));

        // Emit some data
        sink.tryEmitNext("Hello");
        sink.tryEmitNext("Reactive");
        sink.tryEmitNext("World");

        // Complete the sink
        sink.tryEmitComplete();

    }
}
