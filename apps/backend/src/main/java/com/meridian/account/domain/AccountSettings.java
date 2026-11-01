package com.meridian.account.domain;
import java.util.UUID;
public final class AccountSettings {
 private AccountSettings(){}
 public enum Theme {system,light,dark}
 public record Preferences(Theme theme,long version){}
 public record Visibility(UUID publicId,boolean memberVisible,boolean publicVisible,long version){}
 public record Summary(String name,String headline,String location,String about){}
}
