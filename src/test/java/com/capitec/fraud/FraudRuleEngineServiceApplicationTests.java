package com.capitec.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FraudRuleEngineServiceApplicationTests {

    @Test
    void applicationEntryPointExists() {
        assertThat(FraudRuleEngineServiceApplication.class).isNotNull();
    }
}
