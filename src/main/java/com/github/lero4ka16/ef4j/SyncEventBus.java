/*
 *    Copyright 2021 Lero4ka16
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.lero4ka16.ef4j;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author lero4ka16
 */
public final class SyncEventBus extends AbstractEventBus {

    public SyncEventBus() {
        super(true, new HashMap<>(), new HashMap<>(), HashSet::new);
    }

    @Override
    public synchronized void unsubscribe(EventSubscription<?> subscription) {
        super.unsubscribe(subscription);
    }

    @Override
    public synchronized void unsubscribeAll(EventNamespace namespace) {
        super.unsubscribeAll(namespace);
    }

    @Override
    public synchronized void unsubscribeAll() {
        super.unsubscribeAll();
    }

    @Override
    public synchronized void publish(Event event) {
        super.publish(event);
    }

    @Override
    protected synchronized void register(EventSubscription<? extends Event> subscription) {
        super.register(subscription);
    }

    @Override
    public synchronized EventObjectSubscription subscribe(EventNamespace namespace, Object listener) {
        return super.subscribe(namespace, listener);
    }

    @Override
    public synchronized EventObjectSubscription subscribe(Object listener) {
        return super.subscribe(listener);
    }

    @Override
    public synchronized void unsubscribe(EventObjectSubscription objectSubscription) {
        super.unsubscribe(objectSubscription);
    }
}
