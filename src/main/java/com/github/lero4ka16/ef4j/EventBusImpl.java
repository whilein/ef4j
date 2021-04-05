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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lero4ka16
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class EventBusImpl implements EventBus {

    private final Map<EventNamespace, Set<EventSubscription<?>>> byNamespace
            = new ConcurrentHashMap<>();

    private final Map<Type, EventSubscriptionStorage<?>> byEvent
            = new ConcurrentHashMap<>();

    @Override
    public EventObjectSubscription subscribe(EventNamespace namespace, Object listener) {
        Class<?> cls = listener.getClass();

        List<EventSubscription<?>> subscriptions = new ArrayList<>();

        for (Method method : cls.getMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);

            if (handler == null) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();

            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                throw new IllegalStateException("Wrong parameter types");
            }

            Type param = method.getGenericParameterTypes()[0];

            if (param instanceof ParameterizedType) {
                throw new IllegalStateException("Generic as parameter is illegal");
            }

            Class<? extends Event> eventType = (Class<? extends Event>) params[0];

            EventListener createdListener;

            try {
                MethodHandles.Lookup lookup = PrivateLookup.privateIn(method.getDeclaringClass());

                MethodType type = MethodType.methodType(void.class, eventType);
                MethodHandle handle = lookup.findVirtual(method.getDeclaringClass(), method.getName(), type);

                CallSite callSite = LambdaMetafactory.metafactory(
                        lookup, "handle",
                        MethodType.methodType(EventListener.class, method.getDeclaringClass()),
                        MethodType.methodType(void.class, Event.class),
                        handle, type
                );

                createdListener = (EventListener<?>) callSite.getTarget().bindTo(listener).invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            if (!handler.concurrent() && !isSynchronized()) {
                createdListener = new EventListener.Sync(this, createdListener);
            }

            EventSubscription<? extends Event> subscription = new EventSubscription<>(
                    this, namespace, handler.value(), eventType, createdListener, handler.ignoreCancelled()
            );

            addByEvent(subscription);
            addByNamespace(subscription);

            subscriptions.add(subscription);
        }

        return new EventObjectSubscription(this, Collections.unmodifiableList(subscriptions));
    }

    protected boolean isSynchronized() {
        return false;
    }

    @Override
    public EventObjectSubscription subscribe(Object listener) {
        return subscribe(this, listener);
    }

    @Override
    public void unsubscribe(EventSubscription<?> subscription) {
        removeByEvent(subscription);
        removeByNamespace(subscription);
    }

    private void addByNamespace(EventSubscription<?> subscription) {
        Set<EventSubscription<?>> subscriptions = byNamespace.computeIfAbsent(
                subscription.getNamespace(),
                $ -> ConcurrentHashMap.newKeySet()
        );

        subscriptions.add(subscription);
    }

    private void addByEvent(EventSubscription<?> subscription) {
        EventSubscriptionStorage subscriptions = byEvent.computeIfAbsent(
                subscription.getType(), $ -> new EventSubscriptionStorage<>()
        );

        subscriptions.add(subscription);
    }

    private void removeByEvent(EventSubscription<?> subscription) {
        EventSubscriptionStorage subscriptions = byEvent.get(subscription.getType());

        if (subscriptions == null) {
            return;
        }

        subscriptions.remove(subscription);

        if (subscriptions.getSize() == 0) {
            byEvent.remove(subscription.getType());
        }
    }

    private void removeByNamespace(EventSubscription<?> subscription) {
        Set<EventSubscription<?>> subscriptions = byNamespace.get(subscription.getNamespace());

        if (subscriptions == null) {
            return;
        }

        subscriptions.remove(subscription);

        if (subscriptions.isEmpty()) {
            byNamespace.remove(subscription.getNamespace());
        }
    }

    @Override
    public void unsubscribeAll(EventNamespace namespace) {
        Set<EventSubscription<?>> listeners = byNamespace.remove(namespace);

        if (listeners != null) {
            for (EventSubscription<?> subscription : listeners) {
                removeByEvent(subscription);
            }
        }
    }

    @Override
    public void unsubscribeAll() {
        byEvent.clear();
        byNamespace.clear();
    }

    @Override
    public void unsubscribe(EventObjectSubscription objectSubscription) {
        for (EventSubscription<?> subscription : objectSubscription.getSubscriptions()) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void publish(Event event) {
        EventSubscriptionStorage subscriptions = byEvent.get(event.getClass());

        if (subscriptions != null) {
            subscriptions.publish(event);
        }

        event.postPublish(this);

        if (subscriptions != null) {
            subscriptions.postPublish(event);
        }
    }

    private static class PrivateLookup {
        private static final MethodHandles.Lookup INTERNAL;
        private static final Method PRIVATE_LOOKUP_IN;

        static {
            MethodHandles.Lookup internal = null;
            Method privateLookupIn = null;

            try {
                privateLookupIn = MethodHandles.class.getDeclaredMethod("privateLookupIn",
                        Class.class, MethodHandles.Lookup.class);
            } catch (NoSuchMethodException e) {
                try {
                    Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                    field.setAccessible(true);

                    internal = (MethodHandles.Lookup) field.get(null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            INTERNAL = internal;
            PRIVATE_LOOKUP_IN = privateLookupIn;
        }

        public static MethodHandles.Lookup privateIn(Class<?> cls) {
            if (INTERNAL == null) {
                try {
                    return (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, cls, MethodHandles.lookup());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return INTERNAL.in(cls);
            }
        }
    }
}
