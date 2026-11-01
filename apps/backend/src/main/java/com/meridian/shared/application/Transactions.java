package com.meridian.shared.application;
import java.util.function.Supplier;
/** Executes a complete use case atomically; snapshot reads must be repeatable. */
public interface Transactions {
 <T> T write(Supplier<T> operation);
 <T> T snapshot(Supplier<T> operation);
}
