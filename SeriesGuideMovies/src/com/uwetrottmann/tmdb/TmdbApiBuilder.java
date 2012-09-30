/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.uwetrottmann.tmdb;

import com.google.myjson.GsonBuilder;
import com.google.myjson.JsonElement;
import com.google.myjson.JsonObject;
import com.google.myjson.JsonParseException;
import com.google.myjson.reflect.TypeToken;
import com.jakewharton.apibuilder.ApiBuilder;
import com.jakewharton.apibuilder.ApiException;
import com.uwetrottmann.tmdb.entities.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * TMDb-specific API builder extension which provides helper methods for adding
 * fields, parameters, and post-parameters commonly used in the API.
 * 
 * @param <T> Native class type of the HTTP method call result.
 */
public abstract class TmdbApiBuilder<T> extends ApiBuilder {
    /** API key field name. */
    protected static final String FIELD_API_KEY = API_URL_DELIMITER_START + "api_key"
            + API_URL_DELIMITER_END;

    protected static final String FIELD_ID = API_URL_DELIMITER_START + "id" + API_URL_DELIMITER_END;
    protected static final String FIELD_PAGE = API_URL_DELIMITER_START + "page"
            + API_URL_DELIMITER_END;
    protected static final String FIELD_LANGUAGE = API_URL_DELIMITER_START + "language"
            + API_URL_DELIMITER_END;

    /** Format for encoding a {@link java.util.Date} in a URL. */
    private static final SimpleDateFormat URL_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /** API URL base. */
    private static final String BASE_URL = "http://private-18ab-themoviedb.apiary.io/3";

    /** Number of milliseconds in a single second. */
    protected static final long MILLISECONDS_IN_SECOND = 1000;

    /** Valued-list seperator. */
    private static final char SEPERATOR = ',';

    /** Valid HTTP request methods. */
    protected static enum HttpMethod {
        Get, Post
    }

    /** Service instance. */
    private final TmdbApiService service;

    /** Type token of return type. */
    private final TypeToken<T> token;

    /** HTTP request method to use. */
    private final HttpMethod method;

    /** String representation of JSON POST body. */
    private JsonObject postBody;

    /**
     * Initialize a new builder for an HTTP GET call.
     * 
     * @param service Service to bind to.
     * @param token Return type token.
     * @param methodUri URI method format string.
     */
    public TmdbApiBuilder(TmdbApiService service, TypeToken<T> token, String methodUri) {
        this(service, token, methodUri, HttpMethod.Get);
    }

    /**
     * Initialize a new builder for the specified HTTP method call.
     * 
     * @param service Service to bind to.
     * @param token Return type token.
     * @param urlFormat URL format string.
     * @param method HTTP method.
     */
    public TmdbApiBuilder(TmdbApiService service, TypeToken<T> token, String urlFormat,
            HttpMethod method) {
        super(BASE_URL + urlFormat);

        this.service = service;

        this.token = token;
        this.method = method;
        this.postBody = new JsonObject();

        this.parameter(FIELD_API_KEY, this.service.getApiKey());
    }

    /**
     * Execute remote API method and unmarshall the result to its native type.
     * 
     * @return Instance of result type.
     * @throws ApiException if validation fails.
     */
    public final T fire() {
        this.preFireCallback();

        try {
            this.performValidation();
        } catch (Exception e) {
            throw new ApiException(e);
        }

        T result = this.service.unmarshall(this.token, this.execute());
        this.postFireCallback(result);

        return result;
    }

    /**
     * Perform any required actions before validating the request.
     */
    protected void preFireCallback() {
        // Override me!
    }

    /**
     * Perform any required validation before firing off the request.
     */
    protected void performValidation() {
        // Override me!
    }

    /**
     * Perform any required actions before returning the request result.
     * 
     * @param result Request result.
     */
    protected void postFireCallback(T result) {
        // Override me!
    }

    /**
     * <p>
     * Execute the remote API method and return the JSON object result.
     * <p>
     * <p>
     * This method can be overridden to select a specific subset of the JSON
     * object. The overriding implementation should still call 'super.execute()'
     * and then perform the filtering from there.
     * </p>
     * 
     * @return JSON object instance.
     */
    protected final JsonElement execute() {
        String url = this.buildUrl();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        try {
            switch (this.method) {
                case Get:
                    return this.service.get(url);
                case Post:
                    return this.service.post(url, this.postBody.toString());
                default:
                    throw new IllegalArgumentException("Unknown HttpMethod type "
                            + this.method.toString());
            }
        } catch (ApiException ae) {
            try {
                Response response = this.service.unmarshall(new TypeToken<Response>() {
                }, ae.getMessage());
                if (response != null) {
                    throw new TmdbException(url, this.postBody, ae, response);
                }
            } catch (JsonParseException jpe) {
            }

            throw new TmdbException(url, this.postBody, ae);
        }
    }

    /**
     * Print the HTTP request that would be made
     */
    public final void print() {
        this.preFireCallback();

        try {
            this.performValidation();
        } catch (Exception e) {
            throw new ApiException(e);
        }

        String url = this.buildUrl();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        System.out.println(this.method.toString().toUpperCase() + " " + url);
        for (String name : this.service.getRequestHeaderNames()) {
            System.out.println(name + ": " + this.service.getRequestHeader(name));
        }

        switch (this.method) {
            case Post:
                System.out.println();
                System.out.println(new GsonBuilder().setPrettyPrinting().create()
                        .toJson(this.postBody));
                break;
        }
    }

    /**
     * Set the API key.
     * 
     * @param apiKey API key string.
     * @return Current instance for builder pattern.
     */
    /* package */final ApiBuilder api(String apiKey) {
        return this.field(FIELD_API_KEY, apiKey);
    }

    /**
     * Add a URL parameter value.
     * 
     * @param name Name.
     * @param value Value.
     * @return Current instance for builder pattern.
     */
    protected final ApiBuilder parameter(String name, Date value) {
        return this.parameter(name, Long.toString(TmdbApiBuilder.dateToUnixTimestamp(value)));
    }

    /**
     * Add a URL parameter value.
     * 
     * @param name Name.
     * @param value Value.
     * @return Current instance for builder pattern.
     */
    protected final <K extends TraktEnumeration> ApiBuilder parameter(String name, K value) {
        if ((value == null) || (value.toString() == null) || (value.toString().length() == 0)) {
            return this.parameter(name, "");
        } else {
            return this.parameter(name, value.toString());
        }
    }

    /**
     * Add a URL parameter value.
     * 
     * @param name Name.
     * @param valueList List of values.
     * @return Current instance for builder pattern.
     */
    protected final <K extends Object> ApiBuilder parameter(String name, List<K> valueList) {
        StringBuilder builder = new StringBuilder();
        Iterator<K> iterator = valueList.iterator();
        while (iterator.hasNext()) {
            builder.append(encodeUrl(iterator.next().toString()));
            if (iterator.hasNext()) {
                builder.append(SEPERATOR);
            }
        }
        return this.parameter(name, builder.toString());
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param date Value.
     * @return Current instance for builder pattern.
     */
    protected final ApiBuilder field(String name, Date date) {
        return this.field(name, URL_DATE_FORMAT.format(date));
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param valueList List of values.
     * @return Current instance for builder pattern.
     */
    protected final <K extends TraktEnumeration> ApiBuilder field(String name, K[] valueList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < valueList.length; i++) {
            builder.append(encodeUrl(valueList[i].toString()));
            if (i < valueList.length - 1) {
                builder.append(SEPERATOR);
            }
        }
        return this.field(name, builder.toString());
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param valueList List of values.
     * @return Current instance for builder pattern.
     */
    protected final ApiBuilder field(String name, int[] valueList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < valueList.length; i++) {
            builder.append(valueList[i]);
            if (i < valueList.length - 1) {
                builder.append(SEPERATOR);
            }
        }
        return this.field(name, builder.toString());
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param valueList List of values.
     * @return Current instance for builder pattern.
     */
    protected final ApiBuilder field(String name, String[] valueList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < valueList.length; i++) {
            builder.append(encodeUrl(valueList[i]));
            if (i < valueList.length - 1) {
                builder.append(SEPERATOR);
            }
        }
        return this.field(name, builder.toString(), false);
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param value Value.
     * @return Current instance for builder pattern.
     */
    protected final <K extends TraktEnumeration> ApiBuilder field(String name, K value) {
        if ((value == null) || (value.toString() == null) || (value.toString().length() == 0)) {
            return this.field(name);
        } else {
            return this.field(name, value.toString());
        }
    }

    /**
     * Add a URL field value.
     * 
     * @param name Name.
     * @param value Value.
     * @return Current instance for builder pattern.
     */
    protected final ApiBuilder field(String name, long value) {
        // TODO move to api builder
        return this.field(name, Long.toString(value));
    }

    protected final boolean hasPostParameter(String name) {
        return this.postBody.has(name);
    }

    protected final TmdbApiBuilder<T> postParameter(String name, String value) {
        this.postBody.addProperty(name, value);
        return this;
    }

    protected final TmdbApiBuilder<T> postParameter(String name, int value) {
        return this.postParameter(name, Integer.toString(value));
    }

    protected final <K extends TraktEnumeration> TmdbApiBuilder<T> postParameter(String name,
            K value) {
        if ((value != null) && (value.toString() != null) && (value.toString().length() > 0)) {
            return this.postParameter(name, value.toString());
        }
        return this;
    }

    protected final TmdbApiBuilder<T> postParameter(String name, JsonElement value) {
        this.postBody.add(name, value);
        return this;
    }

    protected final TmdbApiBuilder<T> postParameter(String name, boolean value) {
        this.postBody.addProperty(name, value);
        return this;
    }

    /**
     * Convert a {@link Date} to its Unix timestamp equivalent.
     * 
     * @param date Date value.
     * @return Unix timestamp value.
     */
    protected static final long dateToUnixTimestamp(Date date) {
        return date.getTime() / MILLISECONDS_IN_SECOND;
    }
}
