package com.xabe.mutiny;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollableDataSource {

  private final List<String> values;

  private int position;

  public PollableDataSource() {
    this.values = List.of("one", "two", "three", "four");
    this.position = -1;
  }

  public Row poll() {
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
    this.position++;
    return (this.position >= 4) ? Row.of(null) : Row.of(this.values.get(this.position));
  }

  public void close() {
    System.out.println("Datasource close");
  }
}
