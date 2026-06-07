package com.capitec.fraud.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class RsaPublicKeyLoaderTest
{

    private static final String LOCAL_PUBLIC_KEY_LOCATION = "classpath:security/local-dev-public-key.pem";

    @Test
    void loadsConfiguredLocalDevelopmentPublicKey()
    {
        var keyLoader = new RsaPublicKeyLoader(new DefaultResourceLoader());

        var publicKey = keyLoader.load(LOCAL_PUBLIC_KEY_LOCATION);

        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
    }
}
