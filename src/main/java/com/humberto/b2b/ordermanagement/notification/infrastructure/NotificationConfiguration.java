package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.inbox.NotificationInbox;
import com.humberto.b2b.ordermanagement.notification.inbox.PartnerNotifier;
import com.humberto.b2b.ordermanagement.notification.inbox.ProcessedEventStore;
import com.humberto.b2b.ordermanagement.notification.outbox.NotificationPublisher;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxRelay;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class NotificationConfiguration {

    @Bean
    OutboxRelay outboxRelay(OutboxStore outboxStore,
                            NotificationPublisher notificationPublisher,
                            TransactionOperations transactionOperations,
                            Clock clock,
                            @Value("${notification.outbox.batch-size:50}") int batchSize) {
        return new OutboxRelay(outboxStore, notificationPublisher, transactionOperations, clock, batchSize);
    }

    @Bean
    NotificationInbox notificationInbox(ProcessedEventStore processedEventStore,
                                        PartnerNotifier partnerNotifier,
                                        Clock clock) {
        return new NotificationInbox(processedEventStore, partnerNotifier, clock);
    }
}
