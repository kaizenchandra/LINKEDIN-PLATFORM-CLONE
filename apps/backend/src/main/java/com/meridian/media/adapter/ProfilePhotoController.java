package com.meridian.media.adapter;
import com.meridian.media.application.ProfilePhotoUseCases;
import static com.meridian.shared.adapter.HttpUseCases.invoke;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1")
class ProfilePhotoController {
 static final int MAX_BYTES=com.meridian.media.domain.PhotoLimits.MAX_BYTES;
 private final ProfilePhotoUseCases photos;
 ProfilePhotoController(ProfilePhotoUseCases photos){this.photos=photos;}
 @PutMapping("/me/photo") ResponseEntity<Void> upload(@AuthenticationPrincipal OidcUser user,jakarta.servlet.http.HttpServletRequest request)throws IOException {
  if(request.getContentLengthLong()>MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Photo must be at most 2 MiB");
  byte[] input=request.getInputStream().readNBytes(MAX_BYTES+1);
  if(input.length>MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Photo must be at most 2 MiB");
  byte[] photo=normalize(input);
  photos.save(user.getSubject(),photo);
  return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
 }
 static byte[] normalize(byte[] input){
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(input))){
   var readers=ImageIO.getImageReaders(stream);
   if(!readers.hasNext())throw new IOException();
   var reader=readers.next();
   try{
    String format=reader.getFormatName();
    if(!format.equalsIgnoreCase("png")&&!format.equalsIgnoreCase("jpeg"))throw new IOException();
    reader.setInput(stream,true,true);
    int width=reader.getWidth(0),height=reader.getHeight(0);
    if(width<1||height<1||width>4096||height>4096||(long)width*height>16_000_000)throw new IOException();
    var decoded=reader.read(0);
    double scale=Math.min(1,512.0/Math.max(width,height));
    var safe=new BufferedImage(Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),BufferedImage.TYPE_INT_ARGB);
    var graphics=safe.createGraphics();
    try{graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(decoded,0,0,safe.getWidth(),safe.getHeight(),null);}finally{graphics.dispose();}
    var output=new ByteArrayOutputStream();ImageIO.write(safe,"png",output);return output.toByteArray();
   }finally{reader.dispose();}
  }catch(IOException|RuntimeException error){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Choose a valid JPEG or PNG, at most 4096 pixels per side and 16 megapixels");}
 }
 @DeleteMapping("/me/photo") ResponseEntity<Void> remove(@AuthenticationPrincipal OidcUser user){
  photos.remove(user.getSubject());
  return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
 }
 @GetMapping("/me/photo") ResponseEntity<byte[]> own(@AuthenticationPrincipal OidcUser user){
  return image(photos.owner(user.getSubject()));
 }
 @GetMapping("/profiles/{id}/photo") ResponseEntity<byte[]> shared(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  String subject=user==null?null:user.getSubject();
  return image(invoke(()->photos.shared(subject,id)));
 }
 private ResponseEntity<byte[]> image(byte[] photo){
  if(photo==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No profile photo");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox").body(photo);
 }
}
