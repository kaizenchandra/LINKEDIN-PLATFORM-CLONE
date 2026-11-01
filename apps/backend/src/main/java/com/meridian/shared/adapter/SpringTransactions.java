package com.meridian.shared.adapter;
import com.meridian.shared.application.Transactions;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
@Component
public final class SpringTransactions implements Transactions {
 private final TransactionTemplate writes,reads;
 public SpringTransactions(PlatformTransactionManager manager){
  writes=new TransactionTemplate(manager);reads=new TransactionTemplate(manager);
  reads.setReadOnly(true);reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
 }
 public <T> T write(Supplier<T> operation){return writes.execute(status->operation.get());}
 public <T> T snapshot(Supplier<T> operation){return reads.execute(status->operation.get());}
}
