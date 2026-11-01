package com.meridian;
import java.util.Set;
/** In-process post-commit invalidation only; never includes message content. */
record RealtimeChanged(Set<String> subjects) {}
