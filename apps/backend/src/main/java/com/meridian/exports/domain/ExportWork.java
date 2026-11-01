package com.meridian.exports.domain;
import java.util.UUID;
public record ExportWork(UUID id,String subject,int attempt){
 public boolean terminal(boolean tooLarge){return tooLarge||attempt>=3;}
 public String failure(boolean tooLarge){return terminal(tooLarge)?(tooLarge?"TOO_LARGE":"GENERATION_FAILED"):null;}
}
