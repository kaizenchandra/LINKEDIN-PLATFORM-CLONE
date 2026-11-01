package com.meridian.media.adapter;
import com.meridian.media.application.PhotoMaintenance;
/** Isolated from Spring's event/socket scheduler; one bounded cleanup batch at a time. */
final class PhotoCleanup implements AutoCloseable {
 private final java.util.concurrent.ScheduledExecutorService executor=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("media-cleanup").factory());
 PhotoCleanup(PhotoMaintenance photos){executor.scheduleWithFixedDelay(photos::cleanup,60,60,java.util.concurrent.TimeUnit.SECONDS);}
 public void close(){
  executor.shutdown();
  try{if(!executor.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS))executor.shutdownNow();}
  catch(InterruptedException interrupted){executor.shutdownNow();Thread.currentThread().interrupt();}
 }
}
