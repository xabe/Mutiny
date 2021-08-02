package com.xabe.mutiny;

import java.util.Objects;

public class Row {

  private final String value;

  public Row(final String value) {
    this.value = value;
  }

  public static Row of(final String value) {
    return new Row(value);
  }

  public boolean isEmpty() {
    return Objects.isNull(this.value);
  }
}
