package com.hikvision.websocket.autoconfigure;

import com.hikvision.websocket.service.IDeviceCommunicationService;
import com.hikvision.websocket.service.impl.DeviceCommunicationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoload configuration bean
 *
 * @author zhangwei151
 * @date 2022/9/17 23:36
 */
@Configuration
@ConditionalOnClass({DeviceCommunicationServiceImpl.class})
public class WebsocketSdkAutoConfig {

    @Value("${websocket.client.timeout:10000}")
    private Integer requestTimeout;

    @Bean("deviceCommunicationService")
    public IDeviceCommunicationService deviceCommunicationService(){
        IDeviceCommunicationService deviceCommunicationService = new DeviceCommunicationServiceImpl(requestTimeout);
        return deviceCommunicationService;
    }
}
