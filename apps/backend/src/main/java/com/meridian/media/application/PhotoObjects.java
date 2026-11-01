package com.meridian.media.application;
import java.util.UUID;
/** Private normalized objects; storage keys and credentials remain in adapters. */
public interface PhotoObjects {
 final class Missing extends RuntimeException{}
 void put(UUID id,byte[] photo);
 byte[] get(UUID id);
 void delete(UUID id);
}
