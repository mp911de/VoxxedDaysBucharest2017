package com.example;

import java.time.Duration;
import java.util.Random;
import java.util.stream.Stream;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.InfiniteStream;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import rx.Observable;

@SpringBootApplication
@EnableReactiveMongoRepositories(considerNestedRepositories = true)
@RequiredArgsConstructor
public class DemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	final ReactivePersonRepository people;
	final MongoOperations operations;


	@Override
	public void run(String... strings) throws Exception {

		operations.createCollection(Person.class, new CollectionOptions(1000,
				1000, true));

		String[] names = {"Victor", "Marija", "Andreea", "Marius"};
		Random r = new Random();

		Flux<Person> personFlux = Flux.fromStream(Stream.generate(() -> names[r.nextInt(4)])
				.map(Person::new));

		Flux.interval(Duration.ofSeconds(1))
				.zipWith(personFlux)
				.map(Tuple2::getT2)
				.flatMap(people::save)
				.doOnNext(System.out::println)
				.subscribe();

		System.out.println("Go on!");
	}

	interface ReactivePersonRepository extends ReactiveCrudRepository<Person, String> {

		Observable<Person> findByName(String name);

		@InfiniteStream
		Observable<Person> findBy();
	}

	@RestController
	@RequiredArgsConstructor
	static class PersonController {

		final ReactivePersonRepository people;

		@GetMapping("/")
		Observable<Person> getPeople(String name) {
			return people.findByName(name);
		}

		@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		Observable<Person> getStream() {
			return people.findBy();
		}
	}

	@Document
	@Data
	@RequiredArgsConstructor
	static class Person {

		@Id String id;
		final String name;
	}
}
