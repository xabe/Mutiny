package com.xabe.mutiny;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PageInfo {

  private final AtomicInteger page;

  private String nextCursor;

  public PageInfo(final int page) {
    this.page = new AtomicInteger(page);
    this.nextCursor = "";
  }

  public PageInfo(final int page, final String nextCursor) {
    this.page = new AtomicInteger(page);
    this.nextCursor = nextCursor;
  }

  public static PageInfo of(final int page) {
    return new PageInfo(page);
  }

  public static PageInfo of(final int page, final String nextCursor) {
    return new PageInfo(page, nextCursor);
  }

  public AtomicInteger getPage() {
    return this.page;
  }

  public String getNextCursor() {
    return this.nextCursor;
  }

  public PageInfo getAndIncrement() {
    this.page.getAndIncrement();
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof PageInfo) {

      final PageInfo pageInfo = (PageInfo) o;

      return new EqualsBuilder()
          .append(this.page.get(), pageInfo.page.get())
          .append(this.nextCursor, pageInfo.nextCursor)
          .isEquals();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(this.page.get())
        .append(this.nextCursor)
        .toHashCode();
  }

  public void updatePageInfo(final PageInfo pageInfo) {
    this.nextCursor = pageInfo.getNextCursor();
  }
}
