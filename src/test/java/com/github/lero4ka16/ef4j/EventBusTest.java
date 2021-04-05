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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author lero4ka16
 */
public class EventBusTest {

	private EventBus bus;

	@BeforeEach
	public void setup() {
		bus = EventBus.create();
	}

	@Test
	public void testGeneric() {
		AtomicReference<String> value = new AtomicReference<>();

		Object listener = new Object() {
			@EventHandler
			public void string(GenericEvent event) {
				if (event.value instanceof String) {
					value.set((String) event.value);
				}

				if (event.value instanceof Integer) {
					value.set("Int: " + event.value);
				}
			}
		};

		bus.subscribe(listener);

		assertNull(value.get());

		bus.publish(new GenericEvent<>("String"));
		assertEquals("String", value.get());

		bus.publish(new GenericEvent<>(123));
		assertEquals("Int: 123", value.get());
	}

	@Test
	public void testIgnoreCancelled() {
		AtomicReference<String> value = new AtomicReference<>();

		Object listener = new Object() {
			@EventHandler(ignoreCancelled = true)
			public void listenNotCancelled(CancellableEvent event) {
				value.set(event.value);
			}

			@EventHandler(EventPriority.LOWEST)
			public void cancel(CancellableEvent event) {
				if (event.value.equals("Cancel me!")) {
					event.setCancelled(true);
				}
			}
		};

		bus.subscribe(listener);

		assertNull(value.get());

		bus.publish(new CancellableEvent("Do not cancel me please"));
		assertEquals("Do not cancel me please", value.get());

		bus.publish(new CancellableEvent("Cancel me!"));
		assertEquals("Do not cancel me please", value.get());
	}

	@Test
	public void testObject() {
		AtomicReference<String> state = new AtomicReference<>();

		Object listener = new Object() {
			@EventHandler
			public void listen(UpdateStateEvent event) {
				state.set(event.state);
			}
		};

		bus.subscribe(listener);

		assertNull(state.get());

		bus.publish(new UpdateStateEvent("X state"));
		assertEquals("X state", state.get());

		bus.publish(new UpdateStateEvent("Y state"));
		assertEquals("Y state", state.get());

		bus.publish(new UpdateStateEvent("Z state"));
		assertEquals("Z state", state.get());
	}

	@Test
	public void testAsyncEvents() {
		AtomicReference<Thread> wait = new AtomicReference<>();

		Object listener = new Object() {
			@EventHandler
			public void listen(AsyncUpdateStateEvent event) {
				event.addIntent();

				Thread thread = new Thread(() -> {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					event.state = "Changed value";
					event.doneIntent();
				});

				thread.start();

				wait.set(thread);
			}
		};

		bus.subscribe(listener);

		AtomicReference<String> result = new AtomicReference<>();

		bus.publish(new AsyncUpdateStateEvent("Not changed initial value", event -> result.set(event.state)));

		assertNull(result.get());

		try {
			wait.get().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals("Changed value", result.get());
	}

	public static class CancellableEvent extends Event implements Cancellable {

		private final String value;
		private boolean cancelled;

		public CancellableEvent(String value) {
			this.value = value;
		}

		@Override
		public void setCancelled(boolean b) {
			cancelled = b;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}
	}

	public static class UpdateStateEvent extends Event {

		private final String state;

		public UpdateStateEvent(String state) {
			this.state = state;
		}

	}


	public static class GenericEvent<T> extends Event {

		private final T value;

		public GenericEvent(T value) {
			this.value = value;
		}

	}

	public static class AsyncUpdateStateEvent extends AsyncEvent<AsyncUpdateStateEvent> {

		private String state;

		public AsyncUpdateStateEvent(String initial, AsyncCallback<AsyncUpdateStateEvent> callback) {
			super(callback);

			this.state = initial;
		}

	}

}
