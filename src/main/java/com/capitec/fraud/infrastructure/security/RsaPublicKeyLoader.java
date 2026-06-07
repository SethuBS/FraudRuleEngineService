package com.capitec.fraud.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.ResourceLoader;

class RsaPublicKeyLoader
{

    private static final String KEY_ALGORITHM = "RSA";
    private static final String BEGIN_PUBLIC_KEY_MARKER = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY_MARKER = "-----END PUBLIC KEY-----";
    private static final String WHITESPACE_PATTERN = "\\s";
    private static final String EMPTY_REPLACEMENT = "";

    private final ResourceLoader resourceLoader;

    RsaPublicKeyLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    RSAPublicKey load(String publicKeyLocation)
    {
        var resource = resourceLoader.getResource(publicKeyLocation);

        try (var inputStream = resource.getInputStream())
        {
            var pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            var encoded = pem
                    .replace(BEGIN_PUBLIC_KEY_MARKER, EMPTY_REPLACEMENT)
                    .replace(END_PUBLIC_KEY_MARKER, EMPTY_REPLACEMENT)
                    .replaceAll(WHITESPACE_PATTERN, EMPTY_REPLACEMENT);
            var keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(encoded));

            return (RSAPublicKey) KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec);
        }
        catch (IOException | GeneralSecurityException ex)
        {
            throw new IllegalStateException("Unable to load RSA public key from " + publicKeyLocation, ex);
        }
    }
}
