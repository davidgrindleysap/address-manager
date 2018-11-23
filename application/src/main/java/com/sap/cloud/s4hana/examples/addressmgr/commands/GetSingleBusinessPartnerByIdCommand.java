package com.sap.cloud.s4hana.examples.addressmgr.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sap.cloud.sdk.cloudplatform.cache.CacheKey;
import com.sap.cloud.sdk.s4hana.connectivity.CachingErpCommand;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartnerAddress;
import org.slf4j.Logger;

import com.sap.cloud.sdk.cloudplatform.logging.CloudLoggerFactory;
import com.sap.cloud.sdk.s4hana.datamodel.odata.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.s4hana.datamodel.odata.services.BusinessPartnerService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetSingleBusinessPartnerByIdCommand extends CachingErpCommand<BusinessPartner> {
    private static final Logger logger = CloudLoggerFactory.getLogger(GetSingleBusinessPartnerByIdCommand.class);

    private final BusinessPartnerService service;
    private final String id;

    private static final Cache<CacheKey, BusinessPartner> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(10)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();

    public GetSingleBusinessPartnerByIdCommand(final BusinessPartnerService service, final String id) {
        super(GetSingleBusinessPartnerByIdCommand.class);
        this.service = service;
        this.id = id;
    }

    @Nonnull
    @Override
    protected Cache<CacheKey, BusinessPartner> getCache() {
        return cache;
    }

    @Override
    protected CacheKey getCommandCacheKey() {
        return CacheKey.ofTenantIsolation().append(id);
    }

    @Override
    protected BusinessPartner getFallback() {
        logger.warn("Fallback caused because of exception", getExecutionException());
        return null;
    }


    @Override
    public BusinessPartner runCacheable() throws Exception {
        return service.getBusinessPartnerByKey(id)
                .select(BusinessPartner.BUSINESS_PARTNER,
                        BusinessPartner.LAST_NAME,
                        BusinessPartner.FIRST_NAME,
                        BusinessPartner.IS_MALE,
                        BusinessPartner.IS_FEMALE,
                        BusinessPartner.CREATION_DATE,
                        BusinessPartner.TO_BUSINESS_PARTNER_ADDRESS.select(
                                BusinessPartnerAddress.BUSINESS_PARTNER,
                                BusinessPartnerAddress.ADDRESS_ID,
                                BusinessPartnerAddress.COUNTRY,
                                BusinessPartnerAddress.POSTAL_CODE,
                                BusinessPartnerAddress.CITY_NAME,
                                BusinessPartnerAddress.STREET_NAME,
                                BusinessPartnerAddress.HOUSE_NUMBER
                        ))
                .execute();
   }

    public static void refreshCache(String id) {
        logger.info("Going to invalidate the Cache for id: " + id);
        cache.invalidate(CacheKey.ofTenantIsolation().append(id));
    }


}
