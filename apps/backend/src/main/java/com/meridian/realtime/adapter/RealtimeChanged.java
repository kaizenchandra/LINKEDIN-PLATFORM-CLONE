package com.meridian.realtime.adapter;
import java.util.Set;
/** In-process post-commit invalidation only; never includes message content. */
public record RealtimeChanged(Set<String> subjects) {
 public RealtimeChanged {subjects=Set.copyOf(subjects);}
}
