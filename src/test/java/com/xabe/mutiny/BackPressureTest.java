package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class BackPressureTest {

  @Test
  public void shouldThrowBackPressureFailureWithBroadcastProcessor() throws Exception {
    //Given
    final BroadcastProcessor<Long> processor = BroadcastProcessor.create();

    //When
    final AssertSubscriber<Long> result = processor
        .emitOn(Infrastructure.getDefaultExecutor())
        .onItem().transform(this::canOnlyConsumeOneItemPerSecond)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    CompletableFuture.runAsync(() -> {
      for (int i = 0; i < 1000; i++) {
        processor.onNext((long) i);
      }
      return;
    });
    assertThat(result, is(notNullValue()));
    result.awaitFailure();
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getItems(), is(hasSize(1)));
  }

  @Test
  public void shouldThrowBackPressureFailureWithBroadcastProcessorOverFlow() throws Exception {
    //Given
    final BroadcastProcessor<Long> processor = BroadcastProcessor.create();

    //When
    final AssertSubscriber<Long> result = processor
        .onOverflow().buffer(250)
        .emitOn(Infrastructure.getDefaultExecutor())
        .onItem().transform(this::canOnlyConsumeOneItemPerSecond)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    CompletableFuture.runAsync(() -> {
      for (int i = 0; i < 1000; i++) {
        processor.onNext((long) i);
      }
      return;
    });
    assertThat(result, is(notNullValue()));
    result.awaitFailure();
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getItems(), is(hasSize(1)));
  }

  @Test
  public void shouldThrowBackPressureFailureMultiOverFlow() throws Exception {

    final AssertSubscriber<Long> result = Multi.createFrom().ticks().every(Duration.ofMillis(10))
        .onOverflow().buffer(250)
        .emitOn(Infrastructure.getDefaultExecutor())
        .onItem().transform(this::canOnlyConsumeOneItemPerSecond)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    assertThat(result, is(notNullValue()));
    result.awaitFailure();
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getItems(), is(hasSize(3)));
  }

  @Test
  public void shouldThrowBackPressureFailureMulti() throws Exception {

    final AssertSubscriber<Long> result = Multi.createFrom().ticks().every(Duration.ofMillis(10))
        .emitOn(Infrastructure.getDefaultExecutor())
        .onItem().transform(this::canOnlyConsumeOneItemPerSecond)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    assertThat(result, is(notNullValue()));
    result.awaitFailure();
    assertThat(result.getFailure(), is(notNullValue()));
    assertThat(result.getItems(), is(hasSize(1)));
  }

  @Test
  public void shouldDropMessage() throws Exception {
    final CountDownLatch drop = new CountDownLatch(1);

    final AssertSubscriber<Long> result = Multi.createFrom().ticks().every(Duration.ofMillis(10))
        .onOverflow().invoke(x -> {
          System.out.println("Dropping item " + x);
          drop.countDown();
        }).drop()
        .emitOn(Infrastructure.getDefaultExecutor())
        .onItem().transform(this::canOnlyConsumeOneItemPerSecond)
        .select().first(10)
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    assertThat(result, is(notNullValue()));
    drop.await();
    result.awaitItems(10);
    assertThat(result.getItems(), is(hasSize(10)));
  }

  private Long canOnlyConsumeOneItemPerSecond(final Long aLong) {
    try {
      TimeUnit.SECONDS.sleep(1L);
    } catch (final InterruptedException e) {
    }
    return aLong;
  }

}
