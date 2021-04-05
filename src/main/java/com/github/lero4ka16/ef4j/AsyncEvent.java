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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lero4ka16
 */
public abstract class AsyncEvent<Self extends AsyncEvent<Self>> extends Event {

	private final AsyncCallback<Self> callback;

	private final AtomicInteger intents = new AtomicInteger();
	private final AtomicBoolean published = new AtomicBoolean();

	public AsyncEvent(AsyncCallback<Self> callback) {
		this.callback = callback;
	}

	public void addIntent() {
		if (published.get()) {
			throw new IllegalStateException("Event already published");
		}

		intents.incrementAndGet();
	}

	@SuppressWarnings("unchecked")
	public void doneIntent() {
		if (intents.get() == 0) {
			throw new IllegalStateException("No remaining intents");
		}

		int now = intents.decrementAndGet();

		if (now == 0 && published.get()) {
			callback.done((Self) this);
			postDone();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void postPublish(EventBus bus) {
		published.set(true);

		if (intents.get() == 0) {
			callback.done((Self) this);
			postDone();
		}
	}

	protected void postDone() {
	}
}
