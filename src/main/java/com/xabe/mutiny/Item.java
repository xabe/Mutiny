package com.xabe.mutiny;

public class Item {

  private final String value;

  public Item(final String value) {
    this.value = value;
  }

  public static Item of(final String value) {
    return new Item(value);
  }

  public String getValue() {
    return this.value;
  }
}
