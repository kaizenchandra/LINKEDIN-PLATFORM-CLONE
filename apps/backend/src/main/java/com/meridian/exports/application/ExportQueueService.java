package com.meridian.exports.application;
import com.meridian.exports.domain.ExportWork;
import com.meridian.shared.application.Transactions;
public final class ExportQueueService implements ExportQueueUseCases {
 private final ExportQueueStore store;private final Transactions transactions;
 public ExportQueueService(ExportQueueStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public ExportWork claim(){return transactions.write(()->{store.expire();store.failExhausted();var work=store.lockNext();if(work!=null)store.lease(work);return work;});}
 public boolean complete(ExportWork work,byte[] data){return store.complete(work,data);}
 public void fail(ExportWork work,boolean tooLarge){store.fail(work,work.terminal(tooLarge)?"FAILED":"QUEUED",work.failure(tooLarge));}
}
