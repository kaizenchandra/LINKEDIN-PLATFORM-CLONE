package com.meridian;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
final class RecruitingAccess {
 private RecruitingAccess(){}
 // Correlated organization alias o; exactly one caller-subject placeholder.
 static final String SQL=com.meridian.organizations.adapter.OrganizationRecruitingSql.SQL;
 static String lock(JdbcTemplate db,UUID organization){var owners=db.queryForList("select owner_subject from organization where id=? for update",String.class,organization);if(owners.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Company unavailable");return owners.getFirst();}
 static boolean allowed(JdbcTemplate db,UUID organization,String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization o where o.id=? and "+SQL+")",Boolean.class,organization,subject));}
 static Set<String> audience(JdbcTemplate db,UUID organization,String applicant){var members=new HashSet<>(db.queryForList("select owner_subject from organization where id=? union select member from recruiter_access where organization_id=? and state='ACTIVE'",String.class,organization,organization));members.add(applicant);return members;}
}
