/*
 * Copyright 2022 OPS4J.
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
package org.apache.coyote.http2;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicHpackDecoder extends HpackDecoder {

	public static final Logger LOG = LoggerFactory.getLogger(PublicHpackDecoder.class);
	private final Map<String, String> headers = new LinkedHashMap<>();

	@Override
	public void decode(ByteBuffer buffer) {
		setHeaderEmitter(new HeaderEmitter() {
			@Override
			public void emitHeader(String name, String value) {
				headers.put(name, value);
			}

			@Override
			public void setHeaderException(StreamException streamException) {
			}

			@Override
			public void validateHeaders() {
			}
		});
		try {
			super.decode(buffer);
		} catch (HpackException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void reset() {
		headers.clear();
	}

}
