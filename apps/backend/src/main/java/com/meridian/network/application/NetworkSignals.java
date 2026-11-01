package com.meridian.network.application;
import java.util.Set;
/** Schedule invalidation within the transaction; the adapter delivers only after commit. */
public interface NetworkSignals {void changed(Set<String> subjects);}
