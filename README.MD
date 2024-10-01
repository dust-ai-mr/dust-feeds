# Dust Feeds
In Dust, a 'feed' is a catch-all term for 'something that watches something external and returns a stream of useful 
messages' (not a very precise definition ...).

The repo contains a small (hopefully growing) library of Actors (primarily Dust pipeline stages where they find most use)
for creating feeds. Currently supported are:

1. RSS feeds (RSS, Atom etc). Periodically visit an RSS feed and return the latest published documents (or their meta info).
2. Web Spider Actor for *sites* (*not* the whole internet). Crawl a web site, filter onsite links etc etc and send 
HTMLDocumentMsgs on for further processing.
3. Send queries to search engine aggregator SearxNG and send on responses.

Option 3 probably requires that a local instance of SearxNG be installed unless you can find a public one that reliably supports
returning JSON.

Dust-feeds uses the dust-core, dust-http and dust-html libraries, so a prior familiarity with those is probably a good idea.
