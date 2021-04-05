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

/**
 * @author lero4ka16
 */
public final class EventSubscription<E extends Event> implements EventListener<E>, Comparable<EventSubscription<E>> {

    private final EventBus bus;
    private final EventNamespace namespace;

    private final Class<E> type;

    private final EventPriority priority;
    private final EventListener<E> listener;

    private final boolean ignoreCancelled;

    public EventSubscription(EventBus bus, EventNamespace namespace, EventPriority priority,
                             Class<E> type, EventListener<E> listener,
                             boolean ignoreCancelled) {
        this.bus = bus;
        this.namespace = namespace;
        this.type = type;
        this.priority = priority;
        this.listener = listener;
        this.ignoreCancelled = ignoreCancelled;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public EventBus getBus() {
        return bus;
    }

    public EventNamespace getNamespace() {
        return namespace;
    }

    public Class<E> getType() {
        return type;
    }

    public void unsubscribe() {
        bus.unsubscribe(this);
    }


    @Override
    public void handle(E event) {
        if (ignoreCancelled && event instanceof Cancellable) {
            Cancellable cancellable = (Cancellable) event;

            if (cancellable.isCancelled()) {
                return;
            }
        }

        listener.handle(event);
    }

    @Override
    public int compareTo(EventSubscription<E> o) {
        return priority.compareTo(o.priority);
    }
}
