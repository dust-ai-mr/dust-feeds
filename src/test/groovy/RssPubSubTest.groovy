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


import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.ActorSystem
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.msgs.PubSubMsg
import com.mentalresonance.dust.core.services.FSTPersistenceService
import com.mentalresonance.dust.feeds.rss.RssPubSubActor
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class RssPubSubTest extends Specification {

	@Slf4j
	static class Client extends Actor {

		static Props props() {
			Props.create(Client)
		}

		@Override
		void preStart() {
			actorSelection("/user/pubsub").tell(
				new PubSubMsg( HtmlDocumentMsg.class, true),
				self
			)
		}

		ActorBehavior createBehavior() {
			(message) -> {
				log.info "Client got: $message"
			}
		}
	}

	/**
	 * Build a simple two stage pipe - RSS Feed and Logging Actor inside a Pipeline so we should see
	 * a log of visited pages gleaned from the RSS Feed
	 */

	def "RssPubSubWithContent"() {

		when:
			ActorSystem system = new ActorSystem("RssPubSubTest")
			system.setPersistenceService(FSTPersistenceService.create())

			ActorRef pubSubRef = system.context.actorOf(
				RssPubSubActor.props("https://fortune.com/feed", 100000),
				'pubsub'
			)
			system.context.actorOf(Client.props())
			Thread.sleep(5000)
			// Stop the pipe, which will stop th RssFeedPipeActor so it will delete its snapshot.
			system.context.stop(pubSubRef)
			// Give this stop() time to happen otherwise it might think we are in shutdown and not delete its snapshot
			Thread.sleep(1000)
			system.stop()
		then:
			true
	}


}