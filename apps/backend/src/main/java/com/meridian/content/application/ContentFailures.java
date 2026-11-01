package com.meridian.content.application;
import com.meridian.shared.application.UseCaseFailure;
import com.meridian.content.domain.Content;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
final class ContentFailures {
 private ContentFailures(){}
 static UseCaseFailure unavailable(){return new UseCaseFailure(NOT_FOUND,"Post unavailable");}
 static void cursor(Long before){if(before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid cursor");}
 static void text(String text,int max,boolean required){try{Content.text(text,max,required);}catch(IllegalArgumentException failure){throw new UseCaseFailure(INVALID,failure.getMessage());}}
}
