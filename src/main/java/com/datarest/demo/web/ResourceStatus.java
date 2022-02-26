package com.datarest.demo.web;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.HttpHeadersPreparer;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Supplier;

public class ResourceStatus {

    private static final String INVALID_DOMAIN_OBJECT = "Domain object %s is not an instance of the given PersistentEntity of type %s!";

    private final HttpHeadersPreparer preparer;

    private ResourceStatus(HttpHeadersPreparer preparer) {

        Assert.notNull(preparer, "HttpHeadersPreparer must not be null!");

        this.preparer = preparer;
    }

    public static ResourceStatus of(HttpHeadersPreparer preparer) {
        return new ResourceStatus(preparer);
    }


    public StatusAndHeaders getStatusAndHeaders(HttpHeaders requestHeaders, Object domainObject,
                                                PersistentEntity<?, ?> entity) {

        Assert.notNull(requestHeaders, "Request headers must not be null!");
        Assert.notNull(domainObject, "Domain object must not be null!");
        Assert.notNull(entity, "PersistentEntity must not be null!");
        Assert.isTrue(entity.getType().isInstance(domainObject),
                () -> String.format(INVALID_DOMAIN_OBJECT, domainObject, entity.getType()));

        // Check ETag for If-Non-Match

        List<String> ifNoneMatch = requestHeaders.getIfNoneMatch();
        ETag eTag = ifNoneMatch.isEmpty() ? ETag.NO_ETAG : ETag.from(ifNoneMatch.get(0));
        HttpHeaders responseHeaders = preparer.prepareHeaders(entity, domainObject);

        // Check last modification for If-Modified-Since

        return eTag.matches(entity, domainObject) || preparer.isObjectStillValid(domainObject, requestHeaders)
                ? StatusAndHeaders.notModified(responseHeaders)
                : StatusAndHeaders.modified(responseHeaders);
    }

    public static class StatusAndHeaders {

        private final HttpHeaders headers;
        private final boolean modified;

        private StatusAndHeaders(HttpHeaders headers, boolean modified) {

            Assert.notNull(headers, "HttpHeaders must not be null!");

            this.headers = headers;
            this.modified = modified;
        }

        boolean isModified() {
            return this.modified;
        }

        private static StatusAndHeaders notModified(HttpHeaders headers) {
            return new StatusAndHeaders(headers, false);
        }

        private static StatusAndHeaders modified(HttpHeaders headers) {
            return new StatusAndHeaders(headers, true);
        }

        /**
         * Creates a {@link ResponseEntity} based on the given {@link PersistentEntityResource}.
         *
         * @param supplier a {@link Supplier} to provide a {@link PersistentEntityResource} eventually, must not be
         *                 {@literal null}.
         * @return
         */
        public ResponseEntity<?> toResponseEntity(Supplier<?> supplier) {

            return modified //
                    ? new ResponseEntity<>(supplier.get(), headers, HttpStatus.OK) //
                    : new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
        }
    }
}