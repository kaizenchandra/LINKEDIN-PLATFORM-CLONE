package com.meridian.shared.adapter;
import com.meridian.shared.application.UseCaseFailure;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/** Retains Spring's existing error pipeline and response conventions. */
public final class HttpUseCases {
 private HttpUseCases(){}
 public static <T> T invoke(Supplier<T> operation){
  try{return operation.get();}catch(UseCaseFailure failure){
   HttpStatus status=switch(failure.reason()){
    case FORBIDDEN->HttpStatus.FORBIDDEN;case RATE_LIMITED->HttpStatus.TOO_MANY_REQUESTS;
    case UNPROCESSABLE->HttpStatus.UNPROCESSABLE_ENTITY;case INVALID->HttpStatus.BAD_REQUEST;case CONFLICT->HttpStatus.CONFLICT;
    case UNAUTHENTICATED->HttpStatus.UNAUTHORIZED;case NOT_FOUND->HttpStatus.NOT_FOUND;
   };
   throw new ResponseStatusException(status,failure.getMessage());
  }
 }
}
