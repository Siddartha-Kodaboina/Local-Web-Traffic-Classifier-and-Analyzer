package com.network.network_analysis.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NetworkTraffic {
    private String websiteName;
    private long packetSize;
    private String sourceIp;
    private String destinationIp;
    private String protocol;
    private long timestamp;
    private String direction; // INBOUND or OUTBOUND
}
