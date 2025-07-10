/*
 * Created by Eugene Sokolov 15.10.2024, 10:45.
 */

package org.gs.kcusers.service;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.gs.kcusers.configs.yamlobjects.ConsulTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "consul.enabled", havingValue = "true")
@Component
@EnableScheduling
public class ConsulClient {
    final ConsulTags consulTags;
    Logger logger = LoggerFactory.getLogger(ConsulClient.class);
    @Value("${consul.url}")
    String consulUrl;
    @Value("${consul.service.host}")
    String serviceHost;
    @Value("${consul.service.port}")
    int servicePort;
    @Value("${consul.service.ttl}")
    Long serviceTtl;
    @Value("${consul.acltoken}")
    String aclToken;
    @Value("${consul.service.id}")
    String serviceId;
    @Value("${consul.service.name}")
    String serviceName;
    @Value("${consul.service.note}")
    String noteToPass;
    private AgentClient agentClient;

    ConsulClient(ConsulTags consulTags) {
        this.consulTags = consulTags;
    }

    private AgentClient getAgentClient() {
        try {
            Consul.Builder consulBuilder = Consul.builder().withUrl(consulUrl);
            Consul consulClient = aclToken != null
                    ? consulBuilder.withAclToken(aclToken).build()
                    : consulBuilder.build();

            AgentClient aClient = consulClient.agentClient();

            Registration service = ImmutableRegistration.builder()
                    .address(serviceHost)
                    .port(servicePort)
                    .id(serviceId)
                    .name(serviceName)
                    .check(Registration.RegCheck.ttl(serviceTtl))
                    .tags(consulTags.getTags())
                    //.meta(Collections.singletonMap("version", "1.0"))
                    .build();

            aClient.register(service);
            logger.info("Registered in Consul");
            return aClient;
        } catch (Exception e) {
            logger.error("NOT connected to Consul: {}", e.getMessage());
        }
        return null;
    }

    @Scheduled(fixedDelayString = "${consul.service.delay}")
    void passToConsul() {
        if (agentClient == null) {
            agentClient = getAgentClient();
        } else {
            try {
                if (agentClient.isRegistered(serviceId)) {
                    agentClient.pass(serviceId, noteToPass);
                    logger.info("Passed \"{}\" to Consul", noteToPass);
                }
            } catch (Exception e) {
                logger.error("Pass to Consul failed: {}", e.getMessage());
                agentClient = null;
            }
        }
    }
}
