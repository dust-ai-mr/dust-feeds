/*
 *   Copyright 2024-2025 Alan Littleford
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */


import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.ActorSystem
import com.mentalresonance.dust.core.actors.lib.LogActor
import com.mentalresonance.dust.core.actors.lib.ServiceManagerActor
import com.mentalresonance.dust.feeds.searxng.actors.SearxNGServiceActor
import com.mentalresonance.dust.feeds.searxng.msgs.SearxNGRequestMsg
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class SearXNGServiceTest extends Specification {

	/**
	 * Submit query to SearchNXG and log the urls of the links returned. Most public instances of SearXNG do not support
	 * the json format in their responses (they are configured for browser interaction only), and those that do
	 * seem to have bot protectors installed so we use a local instance of SearXNG.
	 *
	 * <b>You must have a valid instance of SearXNG installed locally</b>
	 *
	 * This test simply queries SearXNG and logs the urls it returns.
	 */
	def "SearXNGService"() {

		when:
			ActorSystem system = new ActorSystem("Test")

			ActorRef service = system.context.actorOf(
				ServiceManagerActor.props(
					SearxNGServiceActor.props('http://localhost:8081/search'),
					1
				)
			)
			/*
				This will get and log a SearXNGResponseMsg. The default toString() on this message
				is simply to get the urls of the search results and display them .. so they will appear in the log.

				The actual results (a list of SeaxNGGResultMsg) contain much more information about the search results.
			 */
			ActorRef logger = system.context.actorOf(LogActor.props())

			service.tell(new SearxNGRequestMsg(
				q: 'SpaceX'
			), logger)
			// Wait for stuff to happen then die
			Thread.sleep(10000L)
			system.stop()
		then:
			true
	}
}