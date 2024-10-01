/*
 *   Copyright 2024 Alan Littleford
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


import com.mentalresonance.dust.core.actors.ActorSystem
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.actors.lib.LogActor
import com.mentalresonance.dust.core.actors.lib.PipelineActor
import com.mentalresonance.dust.core.services.FSTPersistenceService
import com.mentalresonance.dust.feeds.crawler.PageCrawlMsg
import com.mentalresonance.dust.feeds.crawler.SiteCrawlerPipeActor
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class WebCrawlerPipelineTest extends Specification {

	/**
	 * Crawl the given website looking for links to pages on the site and following them. Again
	 * a simple two-stage pipe with a Logger at the end.
	 */
	def "SiteCrawlPipeline"() {

		when:
			ActorSystem system = new ActorSystem("Test")
			system.setPersistenceService(FSTPersistenceService.create())
			/**
			 * Just pattern match on hrefs for now - anything
			 */
			Props crawlProps = SiteCrawlerPipeActor.props(), logProps = LogActor.props()
			List propsList = [crawlProps, logProps], nameList = ['site-crawl', 'log']

			system
				.context
				.actorOf(PipelineActor.props(propsList, nameList), 'crawl-pipe')
				.tell(new PageCrawlMsg(null, 'https://cnn.com', SiteCrawlerPipeActor.ROOT), null)

			Thread.sleep(60 * 1000L)
		then:
			true
	}
}