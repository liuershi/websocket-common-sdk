package com.hikvision.websocket.netty.exchange;

import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

/**
 * ResultCollector
 *
 * @author zhangwei151
 * @date 2022/9/18 14:13
 */
public class ResultCollector {

    WeakHashMap<ResponseFuture<Request, Response>, Response> container = new WeakHashMap();

    public void received(Object obj) {
        Iterator<ResponseFuture<Request, Response>> iterator = container.keySet().iterator();
        if (iterator.hasNext()) {
            ResponseFuture<Request, Response> future = iterator.next();
            if (obj instanceof Response) {
                Response response = (Response) obj;
                future.trySuccess(response);
            }
        }
    }

    public Future<Response> createFuture(Request request) {
        ResponseFuture<Request, Response> future = new ResponseFuture<>(request, this);
        container.put(future, null);
        return future;
    }

    public void clear(ResponseFuture<?, ?> future) {
        container.remove(future);
    }
}
