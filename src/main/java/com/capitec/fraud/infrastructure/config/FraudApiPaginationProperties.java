package com.capitec.fraud.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fraud.api.pagination")
public record FraudApiPaginationProperties(
        @NotNull @PositiveOrZero Integer minimumPage,
        @NotNull @Positive Integer minimumSize,
        @NotNull @PositiveOrZero Integer defaultPage,
        @NotNull @Positive Integer defaultSize,
        @NotNull @Positive Integer maxSize)
{

    public FraudApiPaginationProperties
    {
        if (defaultPage < minimumPage)
        {
            throw new IllegalArgumentException("defaultPage must be greater than or equal to minimumPage");
        }
        if (defaultSize < minimumSize)
        {
            throw new IllegalArgumentException("defaultSize must be greater than or equal to minimumSize");
        }
        if (defaultSize > maxSize)
        {
            throw new IllegalArgumentException("defaultSize must be less than or equal to maxSize");
        }
    }

    public int resolvePage(Integer requestedPage)
    {
        return requestedPage == null ? defaultPage : requestedPage;
    }

    public int resolveSize(Integer requestedSize)
    {
        return requestedSize == null ? defaultSize : requestedSize;
    }

    public boolean validPage(int page)
    {
        return page >= minimumPage;
    }

    public boolean validSize(int size)
    {
        return size >= minimumSize && size <= maxSize;
    }
}
