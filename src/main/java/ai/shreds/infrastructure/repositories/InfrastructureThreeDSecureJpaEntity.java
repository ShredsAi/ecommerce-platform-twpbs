package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainThreeDSecureEntity;
import ai.shreds.domain.value_objects.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "three_d_secure")
public class InfrastructureThreeDSecureJpaEntity {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_intent_id", nullable = false, unique = true, updatable = false)
    private UUID paymentIntentId;

    @Column(name = "challenge_url", columnDefinition = "TEXT")
    private String challengeUrl;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "authentication_result", columnDefinition = "jsonb")
    private String authenticationResult;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static InfrastructureThreeDSecureJpaEntity fromDomainEntity(DomainThreeDSecureEntity domain) {
        InfrastructureThreeDSecureJpaEntity jpa = new InfrastructureThreeDSecureJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPaymentIntentId(domain.getPaymentIntentId().getValue());
        jpa.setChallengeUrl(domain.getChallengeUrl());
        jpa.setStatus(domain.getStatus().name());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        
        // Serialize authentication result to JSON
        if (domain.getAuthenticationResult() != null && !domain.getAuthenticationResult().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                jpa.setAuthenticationResult(mapper.writeValueAsString(domain.getAuthenticationResult()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                jpa.setAuthenticationResult("{}");
            }
        }
        
        return jpa;
    }

    @SuppressWarnings("unchecked")
    public DomainThreeDSecureEntity toDomainEntity() {
        Map<String, Object> authResult;
        
        if (this.authenticationResult != null && !this.authenticationResult.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                authResult = (Map<String, Object>) mapper.readValue(this.authenticationResult, Map.class);
            } catch (Exception e) {
                // Create empty map on parsing error
                authResult = Map.of();
            }
        } else {
            authResult = Map.of();
        }
        
        return new DomainThreeDSecureEntity(
            this.id,
            new DomainPaymentIntentIdValue(this.paymentIntentId),
            this.challengeUrl,
            DomainThreeDSecureStatusEnum.valueOf(this.status),
            authResult,
            this.expiresAt,
            this.createdAt,
            this.updatedAt
        );
    }
}