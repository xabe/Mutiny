package com.xabe.mutiny;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class PaginatedApi {

  private final RestService restService;

  public PaginatedApi(final RestService restService) {
    this.restService = restService;
  }

  public CompletionStage<List<Item>> getPageCompletionStage(final PageInfo pageInfo) {
    return CompletableFuture.supplyAsync(this.getPage(pageInfo));
  }

  public Uni<List<Item>> getPageUni(final PageInfo pageInfo) {
    return Uni.createFrom().item(this.getPage(pageInfo));
  }

  private Supplier<List<Item>> getPage(final PageInfo pageInfo) {
    return () -> {
      pageInfo.getAndIncrement();
      final PaginatedPayload paginatedPayload = this.restService.getPage(pageInfo);
      pageInfo.updatePageInfo(paginatedPayload.getPageInfo());
      return paginatedPayload.getItems();
    };
  }
}
