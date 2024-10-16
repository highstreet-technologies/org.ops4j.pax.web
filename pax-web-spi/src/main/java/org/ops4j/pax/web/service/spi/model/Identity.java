/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Auto generated id.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public abstract class Identity {

	private static final AtomicInteger NEXT = new AtomicInteger(0);

	private final int nid;
	private final String id;

	protected Identity() {
		nid = NEXT.incrementAndGet();
		id = (getIdPrefix() + "-" + nid).intern();
	}

	protected String getIdPrefix() {
		return this.getClass().getSimpleName();
	}

	public String getId() {
		return id;
	}

	protected int getNumericId() {
		return nid;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{id=" + getId() + "}";
	}

	@Override
	@SuppressWarnings("StringEquality")
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Identity identity = (Identity) o;
		// can be compared, because ID is intern()ed.
		return id == identity.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
