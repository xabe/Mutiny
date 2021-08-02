package com.xabe.mutiny;

import java.util.List;

public class PaginatedPayload {

  private final List<Item> items;

  private final PageInfo pageInfo;

  public PaginatedPayload(final List<Item> items, final PageInfo pageInfo) {
    this.items = items;
    this.pageInfo = pageInfo;
  }

  public static PaginatedPayload of(final List<Item> items, final PageInfo pageInfo) {
    return new PaginatedPayload(items, pageInfo);
  }

  public static PaginatedPayload of(final List<Item> items) {
    return new PaginatedPayload(items, new PageInfo(1));
  }

  public List<Item> getItems() {
    return this.items;
  }

  public PageInfo getPageInfo() {
    return this.pageInfo;
  }
}
