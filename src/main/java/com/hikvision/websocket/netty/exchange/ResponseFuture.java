package com.hikvision.websocket.netty.exchange;

import javax.naming.CommunicationException;
import java.util.concurrent.*;

/**
 * asynchronous response future
 *
 * @author zhangwei151
 * @date 2022/9/18 14:02
 */
public class ResponseFuture<T, R> implements Future<R> {

    T request;

    volatile R response;

    ResultCollector resultCollector;

    private final CountDownLatch lock = new CountDownLatch(1);

    public ResponseFuture(T request, ResultCollector resultCollector) {
        this.request = request;
        this.resultCollector = resultCollector;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return response != null;
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        lock.await();
        if (isDone()) {
            resultCollector.clear(this);
            return response;
        }
        throw new ExecutionException(new CommunicationException());
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        lock.await(timeout, unit);
        if (isDone()) {
            resultCollector.clear(this);
            return response;
        }
        throw new TimeoutException("Timeout waiting for response result");
    }

    public void trySuccess(R response) {
        this.response = response;
        lock.countDown();
    }
}
