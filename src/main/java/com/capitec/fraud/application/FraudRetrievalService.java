package com.capitec.fraud.application;

import com.capitec.fraud.domain.TransactionEvaluation;

import java.util.Optional;
import java.util.UUID;

public interface FraudRetrievalService
{

    PageResult<FraudAlertView> findAlerts(FraudAlertSearchQuery query);

    Optional<FraudAlertView> findAlert(UUID alertId);

    Optional<TransactionEvaluation> findEvaluation(String transactionId);
}
