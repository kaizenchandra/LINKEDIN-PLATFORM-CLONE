package com.meridian;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
record ResumeText(@NotBlank @Size(max=120) String fileName,@NotBlank @Size(max=20480) String text){
 String validated(){
  boolean badName=!fileName.toLowerCase(Locale.ROOT).endsWith(".txt")||fileName.length()<5||fileName.contains("/")||fileName.contains("\\")||fileName.codePoints().anyMatch(Character::isISOControl);
  boolean badText=text.isBlank()||text.getBytes(StandardCharsets.UTF_8).length>20480||text.codePoints().anyMatch(c->c>=0xD800&&c<=0xDFFF||Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t');
  if(badName||badText)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a nonblank UTF-8 .txt resume up to 20 KiB");
  return text;
 }
}
