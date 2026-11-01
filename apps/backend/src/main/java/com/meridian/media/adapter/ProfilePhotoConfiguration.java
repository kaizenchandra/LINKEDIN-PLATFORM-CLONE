package com.meridian.media.adapter;
import com.meridian.media.application.*;
import org.springframework.context.annotation.*;
@Configuration
public class ProfilePhotoConfiguration {
 @Bean ProfilePhotoUseCases profilePhotoUseCases(ProfilePhotoStore store){return new ProfilePhotoService(store);}
}
