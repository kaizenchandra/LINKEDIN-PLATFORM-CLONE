package com.meridian.organizations.adapter;
/** Correlated alias o, with one caller-subject placeholder. */
public final class OrganizationRecruitingSql {
 private OrganizationRecruitingSql(){}
 public static final String SQL="(? in (select o.owner_subject union all select r.member from recruiter_access r where r.organization_id=o.id and r.state='ACTIVE'))";
}
