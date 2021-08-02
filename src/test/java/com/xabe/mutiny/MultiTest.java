package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultiTest {

  private SubscriberFake subscriberFake;

  @BeforeEach
  public void setUp() throws Exception {
    this.subscriberFake = spy(new SubscriberFake());
    Infrastructure.setDroppedExceptionHandler(err -> System.out.println("Mutiny dropped exception: " + err));
  }

  @Test
  public void shouldCreateMultiCompletion() throws Exception {
    //Given
    final Multi<String> multi = Multi.createFrom().items("a", "b", "c", "d");

    //When
    final AssertSubscriber<String> result = multi
        .select().where(s -> s.length() > 0)
        .onItem().transform(item -> item + " mutiny")
        .onItem().transform(String::toUpperCase)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItems(), is(hasSize(4)));
  }

  @Test
  public void shouldCreateMultiFailure() throws Exception {
    //Given
    final Multi<String> uni = Multi.createFrom().failure(IOException::new);

    //When
    final AssertSubscriber<String> result = uni
        .onItem().transform(item -> item + " mutiny")
        .onItem().transform(String::toUpperCase)
        .subscribe().withSubscriber(AssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getFailure(), is(instanceOf(IOException.class)));
  }

  @Test
  public void shouldCancelMulti() throws Exception {
    //Given
    final CountDownLatch count = new CountDownLatch(2);
    final Multi<Long> uni = Multi.createFrom().ticks().every(Duration.ofMillis(50));

    //When
    final AssertSubscriber<Long> resultAssertSubscriber =
        uni.onSubscription().invoke(() -> System.out.println("⬇️ Subscribed"))
            .onItem().invoke(i -> {
          System.out.println("⬇️ Received item: " + i);
          count.countDown();
        })
            .onFailure().invoke(f -> System.out.println("⬇️ Failed with " + f))
            .onCompletion().invoke(() -> System.out.println("⬇️ Completed"))
            .onRequest().invoke(l -> System.out.println("⬆️ Requested: " + l))
            .onCancellation().invoke(() -> System.out.println("⬆️ Cancelled"))
            .onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
            .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    if (count.getCount() == 0 || count.await(2, TimeUnit.SECONDS)) {
      resultAssertSubscriber.cancel();
      assertThat(resultAssertSubscriber.getItems(), is(notNullValue()));
      assertThat(resultAssertSubscriber.getItems(), is(hasSize(2)));
      Assertions.assertThrows(AssertionError.class, () -> resultAssertSubscriber.awaitNextItem(Duration.ofMillis(100)));
    }
  }

  @Test
  public void shouldCreateMultiFromEmitter() throws Exception {
    //Given
    final AtomicInteger counter = new AtomicInteger();
    final Multi<Integer> uni = Multi.createFrom().emitter(emitter -> {
      emitter.emit(counter.getAndIncrement());
      emitter.emit(counter.getAndIncrement());
      emitter.emit(counter.getAndIncrement());
      emitter.complete();
    });

    //When
    final AssertSubscriber<Integer> result = uni.subscribe().withSubscriber(AssertSubscriber.create(3));
    uni.subscribe().with(this.subscriberFake::onItem, this.subscriberFake::onFailure, this.subscriberFake::completed);

    //Then
    verify(this.subscriberFake, never()).onFailure(any());
    verify(this.subscriberFake, times(3)).onItem(any());

    result.assertCompleted();
    result.assertTerminated();
    assertThat(result.getItems(), is(hasSize(3)));
  }

  @Test
  public void shouldGetCollect() throws Exception {
    //Given
    final Multi<String> multi = Multi.createFrom().items("a", "b", "c", "d");

    //When
    final UniAssertSubscriber<List<String>> itemsAsList = multi.collect().asList().subscribe().withSubscriber(UniAssertSubscriber.create());
    final UniAssertSubscriber<Map<String, String>> itemsAsMap =
        multi.collect().asMap(Function.identity()).subscribe().withSubscriber(UniAssertSubscriber.create());
    final UniAssertSubscriber<Long> count =
        multi.collect().with(Collectors.counting()).subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(itemsAsList, is(notNullValue()));
    assertThat(itemsAsList.getItem(), is(hasSize(4)));
    assertThat(itemsAsMap, is(notNullValue()));
    assertThat(itemsAsMap.getItem().size(), is(4));
    assertThat(count, is(notNullValue()));
    assertThat(count.getItem(), is(4L));
  }

  @Test
  public void shouldCombineMulti() throws Exception {
    //Given
    final Multi<String> hello = Multi.createFrom().item("hello");
    final Multi<String> munity = Multi.createFrom().item("munity");

    //When
    final AssertSubscriber<String> result = Multi.createBy().combining().streams(hello, munity).asTuple()
        .onItem().transform(tuple -> tuple.getItem1() + " " + tuple.getItem2())
        .subscribe().withSubscriber(AssertSubscriber.create(1));

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItems().get(0), is("hello munity"));
  }

  @Test
  public void shouldMergeMulti() throws Exception {
    //Given
    final Random random = new Random();
    final Multi<String> multi1 = Multi.createFrom().items("a", "b", "c")
        .onItem().call(() ->
            Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(random.nextInt(100)))
        );
    final Multi<String> multi2 = Multi.createFrom().items("d", "e", "f")
        .onItem().call(() ->
            Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(random.nextInt(100)))
        );

    //When
    final AssertSubscriber<String> result =
        Multi.createBy().merging().streams(multi1, multi2).subscribe().withSubscriber(AssertSubscriber.create(6));

    //Then
    assertThat(result, is(notNullValue()));
    result.awaitCompletion(Duration.ofSeconds(1));
    assertThat(result.getItems(), is(hasSize(6)));
  }

  @Test
  public void shouldConcatenatedMulti() throws Exception {
    //Given
    final Random random = new Random();
    final Multi<String> multi1 = Multi.createFrom().items("a", "b", "c")
        .onItem().call(() ->
            Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(random.nextInt(100)))
        );
    final Multi<String> multi2 = Multi.createFrom().items("d", "e", "f")
        .onItem().call(() ->
            Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(random.nextInt(100)))
        );

    //When
    final AssertSubscriber<String> result =
        Multi.createBy().concatenating().streams(multi1, multi2).subscribe().withSubscriber(AssertSubscriber.create(6));

    //Then
    assertThat(result, is(notNullValue()));
    result.awaitCompletion(Duration.ofMillis(1000));
    assertThat(result.getItems(), is(hasSize(6)));
  }

  @Test
  public void shouldMultiRecovery() throws Exception {
    //Given
    final Multi<String> uni = Multi.createFrom().failure(new IOException("boom"));

    //When
    final AssertSubscriber<String> resultRecover = uni.onFailure().recoverWithItem("hello!")
        .subscribe().withSubscriber(AssertSubscriber.create(1));

    final AssertSubscriber<String> resultFromMulti = uni.onFailure().recoverWithMulti(() -> Multi.createFrom().item("hello!"))
        .subscribe().withSubscriber(AssertSubscriber.create(1));

    //Then
    assertThat(resultRecover, is(notNullValue()));
    resultRecover.assertCompleted();
    assertThat(resultRecover.getItems(), is(containsInAnyOrder("hello!")));
    assertThat(resultFromMulti, is(notNullValue()));
    resultFromMulti.assertCompleted();
    assertThat(resultFromMulti.getItems(), is(containsInAnyOrder("hello!")));

  }

  @Test
  public void shouldRetry() throws Exception {
    //Given
    final Multi<String> multi = Multi.createFrom().item("multi").onItem().transformToUniAndMerge(this.subscriberFake::invokeService);

    //When
    final AssertSubscriber<String> resultRetry =
        multi.onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(3)).atMost(5)
            .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    assertThat(resultRetry, is(notNullValue()));
    resultRetry.awaitFailure();
    verify(this.subscriberFake, times(5)).invokeService(any());
    assertThat(resultRetry.getFailure(), is(instanceOf(IllegalStateException.class)));
  }

  @Test
  public void shouldTransform() throws Exception {
    //Given
    final Multi<String> uni = Multi.createFrom().items("hello", "mutiny!");

    //When
    final AssertSubscriber<String> resultMultiMerge =
        uni.onItem().transformToMultiAndMerge(item -> Multi.createFrom().item(item.toUpperCase()))
            .subscribe().withSubscriber(AssertSubscriber.create(2));

    final AssertSubscriber<String> resultMultiConcatenate =
        uni.onItem().transformToMultiAndConcatenate(item -> Multi.createFrom().items(item, item))
            .subscribe().withSubscriber(AssertSubscriber.create(4));

    //Then
    assertThat(resultMultiMerge, is(notNullValue()));
    resultMultiMerge.assertCompleted();
    assertThat(resultMultiMerge.getItems(), is(hasSize(2)));
    assertThat(resultMultiMerge.getItems(), is(containsInAnyOrder("HELLO", "MUTINY!")));

    assertThat(resultMultiConcatenate, is(notNullValue()));
    resultMultiConcatenate.assertCompleted();
    assertThat(resultMultiConcatenate.getItems(), is(hasSize(4)));
    assertThat(resultMultiConcatenate.getItems(), is(hasItems("hello", "mutiny!")));
  }

  @Test
  public void events() throws Exception {
    //Given
    final Multi<String> multi = Multi.createFrom().items("hello", "mutiny!");

    //When
    final AssertSubscriber<String> result = multi
        .onSubscription().invoke(() -> System.out.println("⬇️ Subscribed"))
        .onItem().invoke(i -> System.out.println("⬇️ Received item: " + i))
        .onFailure().invoke(f -> System.out.println("⬇️ Failed with " + f))
        .onCompletion().invoke(() -> System.out.println("⬇️ Completed"))
        .onCancellation().invoke(() -> System.out.println("⬆️ Cancelled"))
        .onRequest().invoke(l -> System.out.println("⬆️ Requested: " + l))
        .subscribe().withSubscriber(AssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItems(), is(notNullValue()));

  }
}
