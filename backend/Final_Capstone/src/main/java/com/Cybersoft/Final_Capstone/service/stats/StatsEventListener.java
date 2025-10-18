package com.Cybersoft.Final_Capstone.service.stats;

import com.Cybersoft.Final_Capstone.Entity.SystemStats;
import com.Cybersoft.Final_Capstone.events.HostCreatedEvent;
import com.Cybersoft.Final_Capstone.events.PropertyCreatedEvent;
import com.Cybersoft.Final_Capstone.events.UserCreatedEvent;
import com.Cybersoft.Final_Capstone.repository.SystemStatsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StatsEventListener {

    private final SystemStatsRepository statsRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onUserCreated(UserCreatedEvent e) {
        ensureRow();           // phòng khi quên chạy SQL init
        statsRepo.incUsers();  // UPDATE atomic
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onHostCreated(HostCreatedEvent e) {
        ensureRow();
        statsRepo.incHosts();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onPropertyCreated(PropertyCreatedEvent e) {
        ensureRow();
        statsRepo.incProperties();
    }

    private void ensureRow() {
        statsRepo.findById(1L).orElseGet(() -> statsRepo.save(new SystemStats()));
    }
}