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
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.actors.lib.LogActor
import com.mentalresonance.dust.core.actors.lib.PipelineActor
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.core.services.FSTPersistenceService
import com.mentalresonance.dust.feeds.rss.RssFeedPipeActor
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class RssPipelineTest extends Specification {

	/**
	 * Build a simple two stage pipe - RSS Feed and Logging Actor inside a Pipeline so we should see
	 * a log of visited pages gleaned from the RSS Feed
	 */

	def "RssPipelineWithContent"() {

		when:
			ActorSystem system = new ActorSystem("RssPipelineTest")
			system.setPersistenceService(FSTPersistenceService.create())

			Props rssProps = RssFeedPipeActor.props("https://fortune.com/feed", 100000),
				  logProps = LogActor.props();

			List propsList = [rssProps, logProps], nameList = ['rss', 'log']

			ActorRef pipe = system.context.actorOf(PipelineActor.props(propsList, nameList), 'rsspipe')
			pipe.tell(new StartMsg(), null)
			Thread.sleep(5000)
			// Stop the pipe, which will stop th RssFeedPipeActor so it will delete its snapshot.
			system.context.stop(pipe)
			// Give this stop() time to happen otherwise it might think we are in shutdown and not delete its snapshot
			Thread.sleep(1000)
			system.stop()
		then:
			true
	}

	/**
	 * Build a simple two stage pipe - RSS Feed and Logging Actor inside a Pipeline so we should see
	 * a log of syndicated entries from the feed
	 */
	def "RssPipelineWithMeta"() {

		when:
		ActorSystem system = new ActorSystem("RssPipelineTest")
		system.setPersistenceService(FSTPersistenceService.create())

		Props rssProps = RssFeedPipeActor.props("https://fortune.com/feed", 100000, false),
			  logProps = LogActor.props();

		List propsList = [rssProps, logProps], nameList = ['rss', 'log']

		ActorRef pipe = system.context.actorOf(PipelineActor.props(propsList, nameList), 'rsspipe')
		pipe.tell(new StartMsg(), null)
		Thread.sleep(5000)
		// Stop the pipe, which will stop th RssFeedPipeActor so it will delete its snapshot.
		system.context.stop(pipe)
		// Give this stop() time to happen otherwise it might think we are in shutdown and not delete its snapshot
		Thread.sleep(1000)
		system.stop()
		then:
		true
	}
}