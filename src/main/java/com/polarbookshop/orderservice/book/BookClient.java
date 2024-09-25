package com.polarbookshop.orderservice.book;

import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class BookClient {
    private static final String BOOKS_ROOT_API = "/books/";
    private final WebClient webClient;

    public BookClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient
                .get()
                .uri(BOOKS_ROOT_API + isbn)
                .retrieve()
                .bodyToMono(Book.class)
                /*
                GET 요청에 대해 3초의 타임아웃을 설정한다.
                폴백으로 빈 모노 객체를 반환한다.
	              */
                .timeout(Duration.ofSeconds(3), Mono.empty())
                /*
                404 응답을 받으면 빈 객체를 반환한다.
                */
                .onErrorResume(WebClientResponseException.NotFound.class,
                        exception -> Mono.empty())
                /*
                지수 백오프 전략을 사용해 재시도 횟수가 늘어남에 따라 지연시간을 늘린다.
                총 3회까지 재시도를 하며, 각 재시도마다
                지연 시간이 시도 횟수에 100밀리초를 곱한 값으로 계산되도록 설정.
                */
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                /*
                3회의 재시도 동안 오류가 발생하면 예외를 포착하고 빈 객체를 반환한다.
                */
                .onErrorResume(Exception.class, exception -> Mono.empty());
    }
}
