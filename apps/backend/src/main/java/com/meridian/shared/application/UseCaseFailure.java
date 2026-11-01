package com.meridian.shared.application;
/** Transport-independent failure; HTTP translation belongs to an inbound adapter. */
public final class UseCaseFailure extends RuntimeException {
 public enum Reason { GONE, INVALID, UNPROCESSABLE, CONFLICT, UNAUTHENTICATED, NOT_FOUND, FORBIDDEN, RATE_LIMITED }
 private final Reason reason;
 public UseCaseFailure(Reason reason,String message){super(message);this.reason=reason;}
 public Reason reason(){return reason;}
}
