package com.dispatchops.infrastructure.notification;

import com.dispatchops.domain.model.LanRelaySub;
import com.dispatchops.domain.model.Notification;
import com.dispatchops.infrastructure.persistence.mapper.LanRelaySubMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Optional LAN message relay for notifications.
 * Sends UDP datagrams to subscribed LAN endpoints.
 * Gated by notification.lan-relay.enabled property.
 * Fail-safe: errors are logged, never block the notification pipeline.
 */
@Component
public class LanRelayChannel {

    private static final Logger log = LoggerFactory.getLogger(LanRelayChannel.class);

    private final boolean enabled;
    private final String relayHost;
    private final int relayPort;
    private final LanRelaySubMapper lanRelaySubMapper;

    public LanRelayChannel(
            @Value("${notification.lan-relay.enabled:false}") boolean enabled,
            @Value("${notification.lan-relay.host:127.0.0.1}") String relayHost,
            @Value("${notification.lan-relay.port:9999}") int relayPort,
            LanRelaySubMapper lanRelaySubMapper) {
        this.enabled = enabled;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.lanRelaySubMapper = lanRelaySubMapper;
        if (enabled) {
            log.info("LAN relay channel enabled: {}:{}", relayHost, relayPort);
        } else {
            log.info("LAN relay channel disabled");
        }
    }

    /**
     * Relay a notification to LAN subscribers.
     * Fail-safe: exceptions are caught and logged.
     */
    public void relay(Notification notification) {
        if (!enabled) return;

        try {
            String topic = (notification.getLinkType() != null)
                    ? notification.getLinkType().toLowerCase() + ":*"
                    : "system:*";

            List<LanRelaySub> subscribers = lanRelaySubMapper.findByTopic(topic);
            if (subscribers.isEmpty()) {
                log.debug("No LAN relay subscribers for topic '{}'", topic);
                return;
            }

            String message = String.format(
                    "{\"type\":\"%s\",\"title\":\"%s\",\"recipientId\":%d,\"linkType\":\"%s\",\"linkId\":%s}",
                    notification.getType(),
                    notification.getTitle().replace("\"", "\\\""),
                    notification.getRecipientId(),
                    notification.getLinkType() != null ? notification.getLinkType() : "",
                    notification.getLinkId() != null ? notification.getLinkId() : "null"
            );

            byte[] data = message.getBytes(StandardCharsets.UTF_8);

            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress address = InetAddress.getByName(relayHost);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, relayPort);
                socket.send(packet);
                log.debug("LAN relay sent to {}:{} for {} subscribers", relayHost, relayPort, subscribers.size());
            }
        } catch (IOException e) {
            log.warn("LAN relay delivery failed (non-blocking): {}", e.getMessage());
        } catch (Exception e) {
            log.warn("LAN relay unexpected error (non-blocking): {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
