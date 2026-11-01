package com.meridian.notifications.application;
/** Invoked inside the mutation transaction; transport delivery must occur after commit. */
public interface NotificationSignals {void changed(String subject);}
