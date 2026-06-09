package com.capitec.fraud.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

@Component
public class DataSourcePoolStartupLogger implements ApplicationListener<ApplicationReadyEvent>
{

    private static final Logger LOG = LoggerFactory.getLogger(DataSourcePoolStartupLogger.class);

    private final DataSource dataSource;

    public DataSourcePoolStartupLogger(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)
    {
        if (dataSource instanceof HikariDataSource hikariDataSource)
        {
            LOG.info(
                    "Datasource pool configured: poolName={}, driverClassName={}, minimumIdle={}, "
                            + "maximumPoolSize={}, autoCommit={}, transactionIsolation={}",
                    hikariDataSource.getPoolName(),
                    hikariDataSource.getDriverClassName(),
                    hikariDataSource.getMinimumIdle(),
                    hikariDataSource.getMaximumPoolSize(),
                    hikariDataSource.isAutoCommit(),
                    hikariDataSource.getTransactionIsolation());
            return;
        }

        LOG.info("Datasource configured: type={}", dataSource.getClass().getName());
    }
}
