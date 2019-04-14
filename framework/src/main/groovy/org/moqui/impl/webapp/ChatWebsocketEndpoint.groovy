/*
 * This software is in the public domain under CC0 1.0 Universal plus a
 * Grant of Patent License.
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package org.moqui.impl.webapp

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.websocket.CloseReason
import javax.websocket.EndpointConfig
import javax.websocket.Session
import javax.websocket.RemoteEndpoint

@CompileStatic
class ChatWebsocketEndpoint extends MoquiAbstractEndpoint {
    private final static Logger logger = LoggerFactory.getLogger(ChatWebsocketEndpoint.class)

    final static String subscribePrefix = "subscribe:"
    final static String unsubscribePrefix = "unsubscribe:"

    private Set<String> subscribedTopics = new HashSet<>()

    ChatWebsocketEndpoint() { super() }

    Set<String> getSubscribedTopics() { subscribedTopics }

    @Override
    void onOpen(Session session, EndpointConfig config) {
        super.onOpen(session, config)
        logger.info("ChatWebsocketEndpoint::onOpen() config: " + config);
        logger.info("ChatWebsocketEndpoint::onOpen() session: " + session);
        getEcf().getChatWebSocketListener().registerEndpoint(this)
    }

    @Override
    void onMessage(String message) {
        logger.info("ChatWebsocketEndpoint::onMessage() message: ${message}");
        
        if (message.startsWith(subscribePrefix)) {
            String topics = message.substring(subscribePrefix.length(), message.length())
            for (String topic in topics.split(",")) {
                String trimmedTopic = topic.trim()
                if (trimmedTopic) subscribedTopics.add(trimmedTopic)
            }
            logger.info("ChatWebsocketEndpoint::onMessage() Notification subscribe user ${getUserId()} topics ${subscribedTopics} session ${session?.id}")
        } else if (message.startsWith(unsubscribePrefix)) {
            String topics = message.substring(unsubscribePrefix.length(), message.length())
            for (String topic in topics.split(",")) {
                String trimmedTopic = topic.trim()
                if (trimmedTopic) subscribedTopics.remove(trimmedTopic)
            }
            logger.info("ChatWebsocketEndpoint::onMessage() Notification unsubscribe for user ${getUserId()} in session ${session?.id}, current topics: ${subscribedTopics}")
        } else {
            int mlen = message.length() - 1;
            String msg = message.substring(1,mlen)
            logger.info("ChatWebsocketEndpoint::onMessage() message: ${msg}");
            logger.info("ChatWebsocketEndpoint::onMessage() subscribePrefix: ${subscribePrefix}");
            // "sender":"stephen","receiver":"sophia","message":"hello stephen"
            def msgmap = [:]
            msg.split(",").each {param ->
                                 def nameAndValue = param.split(":")
                                 msgmap[nameAndValue[0]] = nameAndValue[1]
                                }
            logger.info("ChatWebsocketEndpoint::onMessage() msgmap: ${msgmap}");
            logger.info("ChatWebsocketEndpoint::onMessage() msgmap.keySet(): ${msgmap.keySet()}");
            logger.info("ChatWebsocketEndpoint::onMessage() msgmap.values(): ${msgmap.values()}");
            for ( String key : msgmap.keySet() ) {
                String val = msgmap.get(key);
                logger.info("ChatWebsocketEndpoint::onMessage() key: ${key}");
                logger.info("ChatWebsocketEndpoint::onMessage() val: ${val}");
            }
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.sender: ${msgmap.sender}");
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.receiver: ${msgmap.receiver}");
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.message: ${msgmap.message}");
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.sender: ${msgmap['sender']}");
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.receiver: ${msgmap['receiver']}");
            //logger.info("ChatWebsocketEndpoint::onMessage() msgmap.message: ${msgmap['message']}");
            logger.info("ChatWebsocketEndpoint::onMessage() session: ${session}");
            if ( session ) {
                final RemoteEndpoint remote = session.getBasicRemote();
                session.asyncRemote.sendText(message)
            }
            logger.info("ChatWebsocketEndpoint::onMessage() Unknown command prefix for message to ChatWebsocketEndpoint in session ${session?.id}: ${message}")
        }
    }

    @Override
    void onClose(Session session, CloseReason closeReason) {
        logger.info("ChatWebsocketEndpoint::onClose() closeReason: " + closeReason);
        logger.info("ChatWebsocketEndpoint::onClose() session: " + session);
        getEcf().getChatWebSocketListener().deregisterEndpoint(this)
        super.onClose(session, closeReason)
    }
}
