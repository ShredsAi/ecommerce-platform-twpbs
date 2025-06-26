package ai.shreds.infrastructure.config;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;

@Configuration
public class InfrastructureJmsConfig {

    @Value("${spring.artemis.host:localhost}")
    private String host;

    @Value("${spring.artemis.port:61616}")
    private int port;

    @Value("${spring.artemis.user:}")
    private String user;

    @Value("${spring.artemis.password:}")
    private String password;

    @Value("${spring.jms.template.receive-timeout:5000}")
    private long receiveTimeout;

    @Bean
    public ActiveMQConnectionFactory artemisConnectionFactory() {
        String url = "tcp://" + host + ":" + port;
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        if (user != null && !user.isEmpty()) {
            factory.setUser(user);
        }
        if (password != null && !password.isEmpty()) {
            factory.setPassword(password);
        }
        return factory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory(ActiveMQConnectionFactory artemisFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(artemisFactory);
        cachingConnectionFactory.setSessionCacheSize(10);
        cachingConnectionFactory.setCacheConsumers(true);
        return cachingConnectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setSessionTransacted(true);
        jmsTemplate.setReceiveTimeout(receiveTimeout);
        return jmsTemplate;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("3-10");
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);
        return factory;
    }
}