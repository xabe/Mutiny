package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import io.smallrye.mutiny.subscription.Cancellable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UniTest {

  private SubscriberFake subscriberFake;

  @BeforeEach
  public void setUp() throws Exception {
    this.subscriberFake = spy(new SubscriberFake());
    Infrastructure.setDroppedExceptionHandler(err -> System.out.println("Mutiny dropped exception: " + err));
  }

  @Test
  public void shouldCreateUniCompletion() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().item("hello");

    //When
    final UniAssertSubscriber<String> result = uni
        .onItem().transform(item -> item + " mutiny")
        .onItem().transform(String::toUpperCase)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItem(), is("HELLO MUTINY"));
  }

  @Test
  public void shouldCreateUniFailure() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().failure(IOException::new);

    //When
    final UniAssertSubscriber<String> result = uni
        .onItem().transform(item -> item + " mutiny")
        .onItem().transform(String::toUpperCase)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getFailure(), is(instanceOf(IOException.class)));
  }

  @Test
  public void shouldCancelUni() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().item(() -> "cancel").onItem().delayIt().by(Duration.ofSeconds(5));

    //When
    final Cancellable resultSubscriberFake =
        uni.onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
            .subscribe().with(this.subscriberFake::onItem, this.subscriberFake::onFailure);

    final UniAssertSubscriber<String> resultUniAssertSubscriber =
        uni.onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
            .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

    //Then
    resultSubscriberFake.cancel();
    verify(this.subscriberFake, never()).onFailure(any());
    verify(this.subscriberFake, never()).onItem(any());

    resultUniAssertSubscriber.cancel();
    resultUniAssertSubscriber.assertTerminated();
    resultUniAssertSubscriber.assertFailed();
    assertThat(resultUniAssertSubscriber.getSignals().size(), is(2));
  }

  @Test
  public void shouldCreateUniFromEmitter() throws Exception {
    //Given
    final AtomicInteger counter = new AtomicInteger();
    final Uni<Integer> uni = Uni.createFrom().emitter(emitter -> {
      // Called for each subscriber and decide
      // when to emit the item or the failure.
      emitter.complete(counter.getAndIncrement());
    });

    //When
    final UniAssertSubscriber<Integer> result = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
    uni.subscribe().with(this.subscriberFake::onItem, this.subscriberFake::onFailure);

    //Then
    verify(this.subscriberFake, never()).onFailure(any());
    verify(this.subscriberFake).onItem(any());

    result.assertCompleted();
    result.assertTerminated();
    assertThat(result.getItem(), is(0));
  }

  @Test
  public void shouldCombineUni() throws Exception {
    //Given
    final Uni<String> hello = Uni.createFrom().item("hello");
    final Uni<String> munity = Uni.createFrom().item("munity");

    //When
    final UniAssertSubscriber<String> result = Uni.combine().all().unis(hello, munity).asTuple()
        .onItem().transform(tuple -> tuple.getItem1() + " " + tuple.getItem2())
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItem(), is("hello munity"));
  }

  @Test
  public void shouldUniRecovery() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().failure(new IOException("boom"));

    //When
    final UniAssertSubscriber<String> resultRecover = uni.onFailure().recoverWithItem("hello!")
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    final UniAssertSubscriber<String> resultFromUni = uni.onFailure().recoverWithUni(() -> Uni.createFrom().item("hello!"))
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(resultRecover, is(notNullValue()));
    assertThat(resultRecover.getItem(), is("hello!"));
    assertThat(resultFromUni, is(notNullValue()));
    assertThat(resultFromUni.getItem(), is("hello!"));

  }

  @Test
  public void shouldRetry() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().nullItem().onItem().transformToUni(this.subscriberFake::invokeService);

    //When
    final UniAssertSubscriber<String> resultRetry =
        uni.onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(3)).atMost(5)
            .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(resultRetry, is(notNullValue()));
    resultRetry.awaitFailure();
    resultRetry.assertFailed();
    verify(this.subscriberFake, times(5)).invokeService(any());
    assertThat(resultRetry.getFailure(), is(instanceOf(IllegalStateException.class)));
  }

  @Test
  public void shouldTransform() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().item("mutiny!");

    //When
    final UniAssertSubscriber<String> resultUni = uni.onItem().transformToUni(item -> Uni.createFrom().item(item.toUpperCase()))
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    final AssertSubscriber<String> resultMulti = uni.onItem().transformToMulti(item -> Multi.createFrom().items(item, item))
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    assertThat(resultUni, is(notNullValue()));
    assertThat(resultUni.getItem(), is("MUTINY!"));
    assertThat(resultMulti, is(notNullValue()));
    resultMulti.assertCompleted();
    assertThat(resultMulti.getItems(), is(hasSize(2)));
    assertThat(resultMulti.getItems(), is(contains("mutiny!", "mutiny!")));
  }

  @Test
  public void events() throws Exception {
    //Given
    final Uni<String> uni = Uni.createFrom().item("mutiny!");

    //When
    final UniAssertSubscriber<String> result = uni
        .onSubscription().invoke(() -> System.out.println("⬇️ Subscribed"))
        .onItem().invoke(i -> System.out.println("⬇️ Received item: " + i))
        .onFailure().invoke(f -> System.out.println("⬇️ Failed with " + f))
        .onCancellation().invoke(() -> System.out.println("⬆️ Cancelled"))
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    //Then
    assertThat(result, is(notNullValue()));
    assertThat(result.getItem(), is(notNullValue()));

  }
}
