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

import java.util.Arrays;

/**
 * @author lero4ka16
 */
final class EventSubscriptionStorage<E extends Event> {

    private volatile InternalStorage<E> storage = new SingletonStorage<>();

    public synchronized int getSize() {
        return storage.getSize();
    }

    public void publish(E event) {
        storage.publish(event);
    }

    public void postPublish(E event) {
        storage.postPublish(event);
    }

    public synchronized void add(EventSubscription<E> subscription) {
        int size = storage.getSize();

        if (size == 1) {
            InternalStorage<E> oldStorage = storage;
            storage = new ArrayStorage<>();
            oldStorage.migrate(storage);
        }

        storage.add(subscription);
    }

    public synchronized void remove(EventSubscription<E> subscription) {
        storage.remove(subscription);

        int size = storage.getSize();

        if (size <= 1) {
            InternalStorage<E> oldStorage = storage;
            storage = new SingletonStorage<>();

            if (size == 1) {
                oldStorage.migrate(storage);
            }
        }
    }

    interface InternalStorage<E extends Event> {
        int getSize();

        void add(EventSubscription<E> subscription);

        void remove(EventSubscription<E> subscription);

        void migrate(InternalStorage<E> storage);

        void publish(E event);

        void postPublish(E event);

        default boolean isEmpty() {
            return getSize() == 0;
        }
    }

    @SuppressWarnings("unchecked")
    static class ArrayStorage<E extends Event> implements InternalStorage<E> {
        private volatile EventSubscription<E>[] content = new EventSubscription[8];

        private volatile int size;
        private volatile int monitorOffset;

        @Override
        public void publish(E event) {
            for (int i = 0; i < monitorOffset; i++) {
                EventSubscription<E> listener = content[i];

                if (listener != null) {
                    listener.handle(event);
                }
            }
        }

        @Override
        public void postPublish(E event) {
            for (int i = monitorOffset; i < size; i++) {
                EventSubscription<E> listener = content[i];

                if (listener != null) {
                    listener.handle(event);
                }
            }
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public synchronized void add(EventSubscription<E> subscription) {
            if (size >= content.length) {
                content = Arrays.copyOf(content, size + 1);
            }

            for (int i = 0; i < size; i++) {
                EventSubscription<E> at = content[i];

                if (content != null && at.compareTo(subscription) < 0) {
                    continue;
                }

                System.arraycopy(content, i, content, i + 1, content.length - i - 1);

                setSubscription(i, subscription);
                return;
            }

            setSubscription(size, subscription);
        }

        private synchronized void setSubscription(int index, EventSubscription<E> subscription) {
            content[index] = subscription;

            if (subscription.getPriority() != EventPriority.MONITOR) {
                monitorOffset++;
            }

            size++;
        }

        @Override
        public synchronized void remove(EventSubscription<E> subscription) {
            if (content == null) {
                return;
            }

            for (int i = 0; i < size; i++) {
                EventListener<E> listener = content[i];

                if (listener == subscription) {
                    size--;
                    content[i] = null;

                    if (i != size) {
                        System.arraycopy(content, i + 1, content, i, content.length - i - 1);
                    }

                    if (subscription.getPriority() != EventPriority.MONITOR) {
                        monitorOffset--;
                    }

                    break;
                }
            }
        }

        @Override
        public synchronized void migrate(InternalStorage<E> storage) {
            for (int i = 0; i < size; i++) {
                EventSubscription<E> subscription = content[i];

                if (subscription != null) {
                    storage.add(subscription);
                }
            }
        }
    }

    static class SingletonStorage<E extends Event> implements InternalStorage<E> {

        private volatile EventSubscription<E> subscription;

        @Override
        public void publish(E event) {
            if (subscription != null && subscription.getPriority() != EventPriority.MONITOR) {
                subscription.handle(event);
            }
        }

        @Override
        public void postPublish(E event) {
            if (subscription != null && subscription.getPriority() == EventPriority.MONITOR) {
                subscription.handle(event);
            }
        }

        @Override
        public synchronized int getSize() {
            return subscription == null ? 0 : 1;
        }

        @Override
        public synchronized void add(EventSubscription<E> subscription) {
            if (this.subscription != null) {
                throw new IllegalStateException("Singleton is full");
            }

            this.subscription = subscription;
        }

        @Override
        public synchronized void remove(EventSubscription<E> subscription) {
            if (this.subscription == subscription) {
                this.subscription = null;
            }
        }

        @Override
        public synchronized void migrate(InternalStorage<E> storage) {
            if (subscription != null) {
                storage.add(subscription);
            }
        }
    }
}
