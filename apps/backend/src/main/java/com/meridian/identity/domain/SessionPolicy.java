package com.meridian.identity.domain;
import java.time.*;
import java.util.UUID;
/** Time and safe-return policies; no servlet, Spring or token implementation types. */
public final class SessionPolicy {
 private final long maximumMillis;private final Clock clock;
 public SessionPolicy(Duration maximum,Clock clock){
  if(maximum.compareTo(Duration.ofMinutes(1))<0||maximum.compareTo(Duration.ofDays(7))>0)throw new IllegalArgumentException("Session maximum must be between one minute and seven days");
  maximumMillis=maximum.toMillis();this.clock=clock;
 }
 public long now(){return clock.millis();}
 public long cutoff(){return now()-maximumMillis;}
 public long deadline(long creation){return creation+maximumMillis;}
 public boolean expired(long creation){return creation<=cutoff();}
 public boolean freshLogout(Instant issued){Instant now=clock.instant();return issued!=null&&!issued.isBefore(now.minusSeconds(300))&&!issued.isAfter(now.plusSeconds(60));}
 public boolean recentlyAuthenticated(Instant authenticated){Instant now=clock.instant();return authenticated!=null&&!authenticated.isBefore(now.minusSeconds(300))&&!authenticated.isAfter(now.plusSeconds(30));}
 public static String returnPath(boolean privacy,Object target){return privacy?"/settings#privacy":target instanceof UUID id?"/members/"+id:"";}
}
