package com.meridian.exports.adapter;
import com.meridian.exports.application.ExportGenerationUseCases;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
@Component @EnableScheduling
@ConditionalOnProperty(name="meridian.exports.worker-enabled",havingValue="true",matchIfMissing=true)
public class AccountExportWorker {
 private final ExportGenerationUseCases exports;
 public AccountExportWorker(ExportGenerationUseCases exports){this.exports=exports;}
 @Scheduled(fixedDelay=5000) public void tick(){exports.generateNext();}
}
