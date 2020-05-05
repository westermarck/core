package com.dotcms.publishing.listener;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.security.CompanyKeyResetEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.Encryptor;
import java.security.Key;
import java.util.List;

/**
 * Once registered this listener takes care or recreating the Push-Publish Auth-Keys
 * First decrypting the old content using the original Key then re-inserting it using the new Key.
 */
public class SecurityKeyResetEventListener implements EventSubscriber<CompanyKeyResetEvent> {

    private final PublishingEndPointAPI publishingEndPointAPI;

    @VisibleForTesting
    SecurityKeyResetEventListener(final PublishingEndPointAPI publishingEndPointAPI) {
        this.publishingEndPointAPI = publishingEndPointAPI;
    }

    private SecurityKeyResetEventListener() {
       this(APILocator.getPublisherEndPointAPI());
    }

    @Override
    public void notify(final CompanyKeyResetEvent event) {
        try {
            final Key originalKey = event.getOriginalKey();
            final Key newKey = event.getResetKey();
            final List<PublishingEndPoint> allEndPoints = publishingEndPointAPI.getAllEndPoints();
            for (final PublishingEndPoint publishingEndPoint : allEndPoints) {
                try {
                    final StringBuilder originalAuthKey = publishingEndPoint.getAuthKey();
                    final String authKey = Encryptor.decrypt(originalKey, originalAuthKey.toString());
                    final String encryptedKey = Encryptor.encrypt(newKey,authKey);
                    publishingEndPoint.setAuthKey(new StringBuilder(encryptedKey));
                } catch (Exception e) {
                    Logger.error(SecurityKeyResetEventListener.class, String.format("Failed updating endpoint with id `%s`", publishingEndPoint.getId()), e);
                }
            }
        } catch (DotDataException e) {
            Logger.error(SecurityKeyResetEventListener.class, "Error gathering data to update push-publish endpoints.", e);
            throw new DotRuntimeException(e);
        }
    }

    public enum INSTANCE {
        SINGLETON;
        final SecurityKeyResetEventListener provider = new SecurityKeyResetEventListener();

        public static SecurityKeyResetEventListener get() {
            return SINGLETON.provider;
        }

    }

}
