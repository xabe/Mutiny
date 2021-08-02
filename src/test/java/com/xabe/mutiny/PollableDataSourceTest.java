package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PollableDataSourceTest {

  private PollableDataSource pollableDataSource;

  @BeforeEach
  public void setUp() throws Exception {
    this.pollableDataSource = spy(new PollableDataSource());
  }

  @Test
  public void shouldGetAllFromDataSource() throws Exception {
    //Given

    //When
    final AssertSubscriber<Row> result = Uni.createFrom().item(this.pollableDataSource::poll)
        .runSubscriptionOn(Infrastructure.getDefaultExecutor()).repeat().indefinitely().subscribe()
        .withSubscriber(AssertSubscriber.create(10));

    //Then
    TimeUnit.MILLISECONDS.sleep(1100);
    result.cancel();
    this.pollableDataSource.close();
    assertThat(result.getItems(), is(hasSize(2)));
  }

  @Test
  public void shouldGetAllFromDataSourceUntilEmpty() throws Exception {
    //Given

    //When
    final AssertSubscriber<Row> result = Multi.createBy().repeating().supplier(this.pollableDataSource::poll)
        .until(Row::isEmpty).runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .onCompletion().call(() -> {
          this.pollableDataSource.close();
          return Uni.createFrom().nullItem();
        })
        .subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    result.awaitCompletion();
    assertThat(result.getItems(), is(hasSize(4)));
    verify(pollableDataSource).close();
  }
  
}
