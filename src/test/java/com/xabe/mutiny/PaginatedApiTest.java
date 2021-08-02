package com.xabe.mutiny;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PaginatedApiTest {

  private RestService restService;

  private PaginatedApi paginatedApi;

  @BeforeEach
  public void setUp() throws Exception {
    this.restService = mock(RestService.class);
    this.paginatedApi = new PaginatedApi(this.restService);
  }

  @Test
  public void shouldGetAllItemsPaginatedCompletionStage() throws Exception {
    //Given

    //When
    when(this.restService.getPage(PageInfo.of(1)))
        .thenReturn(PaginatedPayload.of(List.of(Item.of("one"), Item.of("two")), PageInfo.of(2, "next2")));
    when(this.restService.getPage(PageInfo.of(2, "next2")))
        .thenReturn(PaginatedPayload.of(List.of(Item.of("three"), Item.of("four")), PageInfo.of(3, "next3")));
    when(this.restService.getPage(PageInfo.of(3, "next3"))).thenReturn(PaginatedPayload.of(List.of()));

    final AssertSubscriber<Item> result = Multi.createBy().repeating()
        .completionStage(() -> PageInfo.of(0), this.paginatedApi::getPageCompletionStage)
        .until(List::isEmpty)
        .onItem().<Item>disjoint().subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    assertThat(result, is(notNullValue()));
    result.awaitCompletion();
    assertThat(result.getItems(), is(hasSize(4)));
    verify(this.restService, times(3)).getPage(any());
  }

  @Test
  public void shouldGetAllItemsPaginatedUni() throws Exception {
    //Given

    //When
    when(this.restService.getPage(PageInfo.of(1)))
        .thenReturn(PaginatedPayload.of(List.of(Item.of("one"), Item.of("two")), PageInfo.of(2, "next2")));
    when(this.restService.getPage(PageInfo.of(2, "next2")))
        .thenReturn(PaginatedPayload.of(List.of(Item.of("three"), Item.of("four")), PageInfo.of(3, "next3")));
    when(this.restService.getPage(PageInfo.of(3, "next3"))).thenReturn(PaginatedPayload.of(List.of()));

    final AssertSubscriber<Item> result = Multi.createBy().repeating()
        .uni(() -> PageInfo.of(0), this.paginatedApi::getPageUni)
        .whilst(item -> !item.isEmpty())
        .onItem().<Item>disjoint().subscribe().withSubscriber(AssertSubscriber.create(10));

    //Then
    assertThat(result, is(notNullValue()));
    result.awaitCompletion();
    assertThat(result.getItems(), is(hasSize(4)));
    verify(this.restService, times(3)).getPage(any());
  }

}
