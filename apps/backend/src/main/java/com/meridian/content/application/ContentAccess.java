package com.meridian.content.application;
import java.util.UUID;
/** Adapter must lock pair before post and recheck current visibility while locked. */
public interface ContentAccess {
 boolean visible(String subject,UUID id);
 boolean lockVisible(String subject,UUID id);
}
