package ai.shreds.infrastructure.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jms.support.converter.MessageConverter;

@Configuration
@EnableJms
public class InfrastructureJMSConfig {

    @Value("${spring.jms.activemq.broker-url}")
    private String brokerUrl;
    
    @Value("${spring.jms.activemq.user:}")
    private String username;
    
    @Value("${spring.jms.activemq.password:}")
    private String password;
    
    @Value("${spring.jms.listener.concurrency:5-10}")
    private String concurrency;
    
    @Value("${spring.jms.template.delivery-mode:PERSISTENT}")
    private String deliveryMode;
    
    @Value("${spring.jms.template.time-to-live:36000000}")
    private long timeToLive;

    @Bean
    public ConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        if (!username.isEmpty()) {
            connectionFactory.setUserName(username);
            connectionFactory.setPassword(password);
        }
        connectionFactory.setTrustAllPackages(true);
        connectionFactory.setOptimizeAcknowledge(true);
        return connectionFactory;
    }
    
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory());
        cachingConnectionFactory.setSessionCacheSize(10);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setCacheConsumers(true);
        cachingConnectionFactory.setReconnectOnException(true);
        return cachingConnectionFactory;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDeliveryPersistent("PERSISTENT".equals(deliveryMode));
        template.setTimeToLive(timeToLive);
        template.setExplicitQosEnabled(true);
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency(concurrency);
        factory.setErrorHandler(throwable -> {
            // Log the error
            System.err.println("Error in JMS listener: " + throwable.getMessage());
        });
        factory.setAutoStartup(true);
        return factory;
    }
}