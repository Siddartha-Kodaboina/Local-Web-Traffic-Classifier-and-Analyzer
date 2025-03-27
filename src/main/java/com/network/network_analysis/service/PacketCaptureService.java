package com.network.network_analysis.service;

import com.network.network_analysis.model.NetworkTraffic;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PacketCaptureService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "network-traffic";

    @PostConstruct
    public void startCapture() {
        List<PcapIf> allDevices = new ArrayList<>();
        StringBuilder errbuf = new StringBuilder();

        // Get list of network devices
        int r = Pcap.findAllDevs(allDevices, errbuf);
        if (r != Pcap.OK || allDevices.isEmpty()) {
            log.error("Can't read list of network interfaces: {}", errbuf);
            return;
        }

        PcapIf device = allDevices.get(0); // Select first device
        log.info("Using network device: {}", device.getName());

        // Open device for packet capture
        Pcap pcap = Pcap.openLive(device.getName(), 65536, Pcap.MODE_PROMISCUOUS, 10 * 1000, errbuf);
        if (pcap == null) {
            log.error("Error opening device for capture: {}", errbuf);
            return;
        }

        // Packet handler
        PcapPacketHandler<String> packetHandler = (packet, user) -> processPacket(packet);

        // Start capturing packets
        log.info("🔹 Packet capturing started...");
        pcap.loop(Pcap.LOOP_INFINITE, packetHandler, "PacketCapture");
        pcap.close();
    }

    private void processPacket(PcapPacket packet) {
        // Extract protocol name
        String protocol = extractProtocol(packet);

        if (!protocol.equals("unknown")) {
            log.info("🟢 Captured Protocol: {}", protocol);

            // Send to Kafka
            kafkaTemplate.send(TOPIC, protocol, protocol);
        } else {
            log.debug("Skipping packet - No recognizable protocol.");
        }
    }

    private String extractProtocol(PcapPacket packet) {
        if (packet.hasHeader(new org.jnetpcap.protocol.tcpip.Tcp())) {
            return "TCP";
        } else if (packet.hasHeader(new org.jnetpcap.protocol.tcpip.Udp())) {
            return "UDP";
        } else if (packet.hasHeader(new org.jnetpcap.protocol.network.Ip4())) {
            return "IPv4";
        } else if (packet.hasHeader(new org.jnetpcap.protocol.network.Ip6())) {
            return "IPv6";
        }
        return "unknown";
    }
}
