package com.capitec.fraud.infrastructure.persistence.entity;

final class SchemaColumns
{

    static final int EVENT_ID_LENGTH = 128;
    static final int TRANSACTION_ID_LENGTH = 128;
    static final int CUSTOMER_ID_LENGTH = 128;
    static final int ACCOUNT_ID_LENGTH = 128;
    static final int CURRENCY_LENGTH = 3;
    static final int COUNTRY_LENGTH = 2;
    static final int MERCHANT_CATEGORY_LENGTH = 80;
    static final int CHANNEL_LENGTH = 64;
    static final int MERCHANT_ID_LENGTH = 128;
    static final int MERCHANT_NAME_LENGTH = 255;
    static final int DEVICE_ID_LENGTH = 128;
    static final int RULE_CODE_LENGTH = 100;
    static final int RULE_NAME_LENGTH = 255;
    static final int STATUS_LENGTH = 32;
    static final int SEVERITY_LENGTH = 32;
    static final int SCORE_CLASSIFICATION_LENGTH = 32;
    static final int MONEY_PRECISION = 19;
    static final int MONEY_SCALE = 4;
    static final String JSONB_COLUMN = "jsonb";
    static final String TEXT_COLUMN = "text";

    private SchemaColumns()
    {
    }
}
