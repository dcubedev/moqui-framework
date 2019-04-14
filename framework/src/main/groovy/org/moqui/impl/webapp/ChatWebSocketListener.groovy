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
import org.moqui.context.ExecutionContextFactory
import org.moqui.context.NotificationMessage
import org.moqui.context.NotificationMessageListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
class ChatWebSocketListener implements NotificationMessageListener {
    private final static Logger logger = LoggerFactory.getLogger(ChatWebSocketListener.class)

    private ExecutionContextFactory ecf = null
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ChatWebsocketEndpoint>> endpointsByUser = new ConcurrentHashMap<>()

    void registerEndpoint(ChatWebsocketEndpoint endpoint) {
        logger.warn("ChatWebSocketListener::registerEndpoint() endpoint ${endpoint}")
        logger.warn("ChatWebSocketListener::registerEndpoint() endpoint.userId ${endpoint.userId}")
        logger.warn("ChatWebSocketListener::registerEndpoint() endpoint.session.id ${endpoint.session.id}")
        String userId = endpoint.userId
        if (userId == null) return
        String sessionId = endpoint.session.id
        ConcurrentHashMap<String, ChatWebsocketEndpoint> registeredEndPoints = endpointsByUser.get(userId)
        if (registeredEndPoints == null) {
            registeredEndPoints = new ConcurrentHashMap<>()
            ConcurrentHashMap<String, ChatWebsocketEndpoint> existing = endpointsByUser.putIfAbsent(userId, registeredEndPoints)
            if (existing != null) registeredEndPoints = existing
        }
        ChatWebsocketEndpoint existing = registeredEndPoints.putIfAbsent(sessionId, endpoint)
        logger.warn("ChatWebSocketListener::registerEndpoint() ChatWebsocketEndpoint existing: ${existing}")
        if (existing != null) logger.warn("Found existing ChatWebsocketEndpoint for user ${endpoint.userId} (${existing.username}) session ${sessionId}; not registering additional endpoint")
    }
    void deregisterEndpoint(ChatWebsocketEndpoint endpoint) {
        String userId = endpoint.userId
        if (userId == null) return
        String sessionId = endpoint.session.id
        ConcurrentHashMap<String, ChatWebsocketEndpoint> registeredEndPoints = endpointsByUser.get(userId)
        if (registeredEndPoints == null) {
            logger.warn("Tried to deregister endpoing for user ${endpoint.userId} but no endpoints found")
            return
        }
        registeredEndPoints.remove(sessionId)
        if (registeredEndPoints.size() == 0) endpointsByUser.remove(userId, registeredEndPoints)
    }

    @Override
    void init(ExecutionContextFactory ecf) {
        logger.warn("ChatWebSocketListener::init() ecf ${ecf}")
        this.ecf = ecf
    }

    @Override
    void destroy() {
        logger.warn("ChatWebSocketListener::destroy() endpointsByUser ${endpointsByUser}")
        endpointsByUser.clear()
        this.ecf = null
    }

    @Override
    void onMessage(NotificationMessage nm) {
        logger.warn("ChatWebSocketListener::onMessage() endpointsByUser ${endpointsByUser}")
        logger.warn("ChatWebSocketListener::onMessage() NotificationMessage nm ${nm}")
        logger.warn("ChatWebSocketListener::onMessage() nm.topic ${nm.topic}")
        String messageWrapperJson = nm.getWrappedMessageJson()
        logger.warn("ChatWebSocketListener::onMessage() messageWrapperJson ${messageWrapperJson}")
        for (String userId in nm.getNotifyUserIds()) {
            logger.warn("ChatWebSocketListener::onMessage() userId ${userId}")
            ConcurrentHashMap<String, ChatWebsocketEndpoint> registeredEndPoints = endpointsByUser.get(userId)
            logger.warn("ChatWebSocketListener::onMessage() registeredEndPoints ${registeredEndPoints}")
            if (registeredEndPoints == null) continue
            for (ChatWebsocketEndpoint endpoint in registeredEndPoints.values()) {
                if (endpoint.session != null && endpoint.session.isOpen() &&
                        (endpoint.subscribedTopics.contains("ALL") || endpoint.subscribedTopics.contains(nm.topic))) {
                    endpoint.session.asyncRemote.sendText(messageWrapperJson)
                    nm.markSent(userId)
                }
            }
        }
    }
}
