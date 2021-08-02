package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class UnicastProcessorTest {

  @Test
  public void shouldCreateUnicastProcessor() throws Exception {
    //Given
    final UnicastProcessor<String> processor = UnicastProcessor.create();

    //When
    final AssertSubscriber<String> result = processor
        .onItem().transform(item -> {
          if ("9".equals(item)) {
            throw new RuntimeException();
          }
          return item;
        })
        .onFailure().recoverWithItem("d'oh").subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    CompletableFuture.runAsync(() -> {
      for (int i = 0; i < 10; i++) {
        processor.onNext(Integer.toString(i));
      }
      processor.onComplete();
      return;
    });
    assertThat(result, is(notNullValue()));
    result.awaitCompletion();
    assertThat(result.getItems(), hasSize(10));

  }

}
