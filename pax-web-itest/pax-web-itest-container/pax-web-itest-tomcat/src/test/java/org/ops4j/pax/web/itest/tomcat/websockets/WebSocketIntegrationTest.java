/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.tomcat.websockets;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.websockets.AbstractWebSocketIntegrationTest;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends AbstractWebSocketIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebTomcat());
		Option[] websocketOptions = combine(serverOptions, tomcatWebSockets());
		return combine(websocketOptions, paxWebExtenderWar());
	}

	@Override
	protected String getContainerSpecificWebSocketsBundleSN() {
		return "org.ops4j.pax.web.pax-web-tomcat-websocket";
	}

}
