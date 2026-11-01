package com.meridian.media.application;

/** Owner-scoped photo retrieval for trusted application workflows. */
public interface OwnerPhoto {
 byte[] owner(String subject);
}
