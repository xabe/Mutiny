package com.xabe.mutiny;

import io.smallrye.mutiny.Uni;
import java.io.IOException;

public class SubscriberFake {

  public void onFailure(final Throwable failure) {
    System.out.println("Failed with " + failure);
  }

  public <T> void onItem(final T item) {
    System.out.println(item);
  }

  public <T, R> Uni<R> invokeService(final T item) {
    return Uni.createFrom().failure(new IOException("boom"));
  }

  public void completed() {
    System.out.println("Completed");
  }
}
