/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.engine.http.header;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.Message;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.AuthenticationInfo;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ClientInfo;
import org.restlet.data.Conditions;
import org.restlet.data.CookieSetting;
import org.restlet.data.Digest;
import org.restlet.data.Dimension;
import org.restlet.data.Disposition;
import org.restlet.data.Encoding;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.data.Warning;
import org.restlet.engine.Engine;
import org.restlet.engine.util.DateUtils;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.util.Series;

/**
 * HTTP-style header utilities.
 * 
 * @author Jerome Louvel
 */
public class HeaderUtils {

    /**
     * Adds the entity headers based on the {@link Representation} to the
     * {@link Series}.
     * 
     * @param entity
     *            The source entity {@link Representation}.
     * @param headers
     *            The target headers {@link Series}.
     */
    public static void addEntityHeaders(Representation entity,
            Series<Parameter> headers) {
        if (entity == null || !entity.isAvailable()) {
            addHeader(HeaderConstants.HEADER_CONTENT_LENGTH, "0", headers);
        } else {
            if (!entity.getEncodings().isEmpty()) {
                if (entity.getEncodings().size() == 1) {
                    addHeader(HeaderConstants.HEADER_CONTENT_ENCODING, entity
                            .getEncodings().get(0).getName(), headers);
                } else {
                    StringBuilder value = new StringBuilder();

                    for (Encoding encoding : entity.getEncodings()) {
                        if (!encoding.equals(Encoding.IDENTITY)) {
                            if (value.length() > 0) {
                                value.append(", ");
                            }

                            value.append(encoding.getName());
                        }
                    }

                    if (value.length() > 0) {
                        addHeader(HeaderConstants.HEADER_CONTENT_ENCODING,
                                value.toString(), headers);
                    }
                }
            }

            if (!entity.getLanguages().isEmpty()) {
                if (entity.getLanguages().size() == 1) {
                    addHeader(HeaderConstants.HEADER_CONTENT_LANGUAGE, entity
                            .getLanguages().get(0).toString(), headers);
                } else {
                    StringBuilder value = new StringBuilder();

                    for (Language language : entity.getLanguages()) {
                        if (value.length() > 0) {
                            value.append(", ");
                        }

                        value.append(language.getName());
                    }

                    addHeader(HeaderConstants.HEADER_CONTENT_LANGUAGE, value
                            .toString(), headers);
                }
            }

            long size = entity.getAvailableSize();

            if (size != Representation.UNKNOWN_SIZE) {
                addHeader(HeaderConstants.HEADER_CONTENT_LENGTH, Long
                        .toString(size), headers);
            }

            if (entity.getLocationRef() != null) {
                addHeader(HeaderConstants.HEADER_CONTENT_LOCATION, entity
                        .getLocationRef().getTargetRef().toString(), headers);
            }

            // [ifndef gwt]
            if (entity.getDigest() != null
                    && Digest.ALGORITHM_MD5.equals(entity.getDigest()
                            .getAlgorithm())) {
                addHeader(HeaderConstants.HEADER_CONTENT_MD5, new String(
                        org.restlet.engine.util.Base64.encode(entity
                                .getDigest().getValue(), false)), headers);
            }
            // [enddef]

            if (entity.getRange() != null) {
                try {
                    addHeader(HeaderConstants.HEADER_CONTENT_RANGE, RangeWriter
                            .write(entity.getRange(), entity.getSize()),
                            headers);
                } catch (Exception e) {
                    Context
                            .getCurrentLogger()
                            .log(
                                    Level.WARNING,
                                    "Unable to format the HTTP Content-Range header",
                                    e);
                }
            }

            // Add the content type header
            if (entity.getMediaType() != null) {
                String contentType = entity.getMediaType().toString();

                // Specify the character set parameter if required
                if ((entity.getMediaType().getParameters().getFirstValue(
                        "charset") == null)
                        && (entity.getCharacterSet() != null)) {
                    contentType = contentType + "; charset="
                            + entity.getCharacterSet().getName();
                }

                addHeader(HeaderConstants.HEADER_CONTENT_TYPE, contentType,
                        headers);
            }

            // Add the expiration date header
            if (entity.getExpirationDate() != null) {
                addHeader(HeaderConstants.HEADER_EXPIRES, DateUtils
                        .format(entity.getExpirationDate()), headers);
            }

            // Add the last modification date header
            if (entity.getModificationDate() != null) {
                addHeader(HeaderConstants.HEADER_LAST_MODIFIED, HeaderWriter
                        .write(entity.getModificationDate(), false), headers);
            }

            // Add the E-Tag header
            if (entity.getTag() != null) {
                addHeader(HeaderConstants.HEADER_ETAG,
                        entity.getTag().format(), headers);
            }

            if (entity.getDisposition() != null
                    && !Disposition.TYPE_NONE.equals(entity.getDisposition()
                            .getType())) {
                addHeader(HeaderConstants.HEADER_CONTENT_DISPOSITION,
                        DispositionWriter.write(entity.getDisposition()),
                        headers);
            }
        }
    }

    /**
     * Adds extension headers if they are non-standard headers.
     * 
     * @param existingHeaders
     *            The headers to update.
     * @param additionalHeaders
     *            The headers to add.
     */
    public static void addExtensionHeaders(Series<Parameter> existingHeaders,
            Series<Parameter> additionalHeaders) {
        if (additionalHeaders != null) {
            for (final Parameter param : additionalHeaders) {
                if (param.getName().equalsIgnoreCase(
                        HeaderConstants.HEADER_ACCEPT)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ACCEPT_CHARSET)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ACCEPT_ENCODING)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ACCEPT_LANGUAGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ACCEPT_RANGES)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_AGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ALLOW)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_AUTHENTICATION_INFO)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_AUTHORIZATION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CACHE_CONTROL)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONNECTION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_DISPOSITION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_ENCODING)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_LANGUAGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_LENGTH)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_LOCATION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_MD5)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_RANGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_CONTENT_TYPE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_COOKIE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_DATE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_ETAG)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_EXPECT)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_EXPIRES)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_FROM)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_HOST)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_IF_MATCH)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_IF_MODIFIED_SINCE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_IF_NONE_MATCH)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_IF_RANGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_IF_UNMODIFIED_SINCE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_LAST_MODIFIED)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_LOCATION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_MAX_FORWARDS)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_PROXY_AUTHENTICATE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_PROXY_AUTHORIZATION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_RANGE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_REFERRER)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_RETRY_AFTER)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_SERVER)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_SET_COOKIE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_SET_COOKIE2)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_USER_AGENT)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_VARY)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_WARNING)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_WWW_AUTHENTICATE)) {
                    // Standard headers that can't be overridden
                    Context
                            .getCurrentLogger()
                            .warning(
                                    "Addition of the standard header \""
                                            + param.getName()
                                            + "\" is not allowed. Please use the equivalent property in the Restlet API.");
                } else if (param.getName().equalsIgnoreCase(
                        HeaderConstants.HEADER_PRAGMA)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_TRAILER)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_TRANSFER_ENCODING)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_TRANSFER_EXTENSION)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_UPGRADE)
                        || param.getName().equalsIgnoreCase(
                                HeaderConstants.HEADER_VIA)) {
                    // Standard headers that shouldn't be overridden
                    Context
                            .getCurrentLogger()
                            .info(
                                    "Addition of the standard header \""
                                            + param.getName()
                                            + "\" is discouraged as a future versions of the Restlet API will directly support it.");
                    existingHeaders.add(param);
                } else {
                    existingHeaders.add(param);
                }
            }
        }
    }

    /**
     * Writes the general headers from the {@link Representation} to the
     * {@link Series}.
     * 
     * @param message
     *            The source {@link Message}.
     * @param headers
     *            The target headers {@link Series}.
     */
    public static void addGeneralHeaders(Message message,
            Series<Parameter> headers) {

        // Add the Cache-control headers
        if (!message.getCacheDirectives().isEmpty()) {
            addHeader(HeaderConstants.HEADER_CACHE_CONTROL,
                    CacheDirectiveWriter.write(message.getCacheDirectives()),
                    headers);
        }

        // Add the date
        if (message.getDate() == null) {
            message.setDate(new Date());
        }

        addHeader(HeaderConstants.HEADER_DATE, DateUtils.format(message
                .getDate()), headers);

        // Add the warning headers
        if (!message.getWarnings().isEmpty()) {
            for (Warning warning : message.getWarnings()) {
                addHeader(HeaderConstants.HEADER_WARNING, WarningWriter
                        .write(warning), headers);
            }
        }

    }

    /**
     * Adds a header to the given list. Check for exceptions and logs them.
     * 
     * @param headerName
     *            The header name.
     * @param headerValue
     *            The header value.
     * @param headers
     *            The headers list.
     */
    public static void addHeader(String headerName, String headerValue,
            Series<Parameter> headers) {
        if ((headerName != null) && (headerValue != null)) {
            try {
                headers.add(headerName, headerValue);
            } catch (Throwable t) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to format the " + headerName + " header", t);
            }
        }
    }

    /**
     * Adds the headers based on the {@link Request} to the given {@link Series}
     * .
     * 
     * @param request
     *            The {@link Request} to copy the headers from.
     * @param headers
     *            The {@link Series} to copy the headers to.
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static void addRequestHeaders(Request request,
            Series<Parameter> headers) throws IllegalArgumentException {

        // --------------------------
        // 1) Add the general headers
        // --------------------------
        addGeneralHeaders(request, headers);

        // --------------------------------
        // 2) Add request specific headers
        // --------------------------------

        // Add the preferences
        ClientInfo client = request.getClientInfo();
        if (client.getAcceptedMediaTypes().size() > 0) {
            addHeader(HeaderConstants.HEADER_ACCEPT, PreferenceWriter
                    .write(client.getAcceptedMediaTypes()), headers);
        } else {
            addHeader(HeaderConstants.HEADER_ACCEPT, MediaType.ALL.getName(),
                    headers);
        }

        if (client.getAcceptedCharacterSets().size() > 0) {
            addHeader(HeaderConstants.HEADER_ACCEPT_CHARSET, PreferenceWriter
                    .write(client.getAcceptedCharacterSets()), headers);
        }

        if (client.getAcceptedEncodings().size() > 0) {
            addHeader(HeaderConstants.HEADER_ACCEPT_ENCODING, PreferenceWriter
                    .write(client.getAcceptedEncodings()), headers);
        }

        if (client.getAcceptedLanguages().size() > 0) {
            addHeader(HeaderConstants.HEADER_ACCEPT_LANGUAGE, PreferenceWriter
                    .write(client.getAcceptedLanguages()), headers);
        }

        // Add the from header
        if (request.getClientInfo().getFrom() != null) {
            addHeader(HeaderConstants.HEADER_FROM, request.getClientInfo()
                    .getFrom(), headers);
        }

        // Manually add the host name and port when it is potentially
        // different from the one specified in the target resource
        // reference.
        Reference hostRef = (request.getResourceRef().getBaseRef() != null) ? request
                .getResourceRef().getBaseRef()
                : request.getResourceRef();

        if (hostRef.getHostDomain() != null) {
            String host = hostRef.getHostDomain();
            int hostRefPortValue = hostRef.getHostPort();

            if ((hostRefPortValue != -1)
                    && (hostRefPortValue != request.getProtocol()
                            .getDefaultPort())) {
                host = host + ':' + hostRefPortValue;
            }

            addHeader(HeaderConstants.HEADER_HOST, host, headers);
        }

        // Add the conditions
        Conditions condition = request.getConditions();
        if (!condition.getMatch().isEmpty()) {
            StringBuilder value = new StringBuilder();

            for (int i = 0; i < condition.getMatch().size(); i++) {
                if (i > 0) {
                    value.append(", ");
                }
                value.append(condition.getMatch().get(i).format());
            }

            addHeader(HeaderConstants.HEADER_IF_MATCH, value.toString(),
                    headers);
        }

        if (condition.getModifiedSince() != null) {
            String imsDate = DateUtils.format(condition.getModifiedSince());
            addHeader(HeaderConstants.HEADER_IF_MODIFIED_SINCE, imsDate,
                    headers);
        }

        if (!condition.getNoneMatch().isEmpty()) {
            StringBuilder value = new StringBuilder();

            for (int i = 0; i < condition.getNoneMatch().size(); i++) {
                if (i > 0) {
                    value.append(", ");
                }
                value.append(condition.getNoneMatch().get(i).format());
            }

            addHeader(HeaderConstants.HEADER_IF_NONE_MATCH, value.toString(),
                    headers);
        }

        if (condition.getRangeTag() != null && condition.getRangeDate() != null) {
            Context
                    .getCurrentLogger()
                    .log(
                            Level.WARNING,
                            "Unable to format the HTTP If-Range header due to the presence of both entity tag and modification date.");
        } else {
            if (condition.getRangeTag() != null) {
                addHeader(HeaderConstants.HEADER_IF_RANGE, condition
                        .getRangeTag().format(), headers);
            } else if (condition.getRangeDate() != null) {
                String rDate = DateUtils.format(condition.getRangeDate(),
                        DateUtils.FORMAT_RFC_1123.get(0));
                addHeader(HeaderConstants.HEADER_IF_RANGE, rDate, headers);
            }
        }

        if (condition.getUnmodifiedSince() != null) {
            String iusDate = DateUtils.format(condition.getUnmodifiedSince(),
                    DateUtils.FORMAT_RFC_1123.get(0));
            addHeader(HeaderConstants.HEADER_IF_UNMODIFIED_SINCE, iusDate,
                    headers);
        }

        // Add the maxForwards header
        if (request.getMaxForwards() > -1) {
            addHeader(HeaderConstants.HEADER_MAX_FORWARDS, Integer
                    .toString(request.getMaxForwards()), headers);
        }

        // Add Range header
        if (!request.getRanges().isEmpty()) {
            addHeader(HeaderConstants.HEADER_RANGE,
                    org.restlet.engine.http.header.RangeWriter.write(request
                            .getRanges()), headers);
        }

        // Add the referrer header
        if (request.getReferrerRef() != null) {
            addHeader(HeaderConstants.HEADER_REFERRER, request.getReferrerRef()
                    .toString(), headers);
        }

        // Add the user agent header
        if (request.getClientInfo().getAgent() != null) {
            addHeader(HeaderConstants.HEADER_USER_AGENT, request
                    .getClientInfo().getAgent(), headers);
        } else {
            addHeader(HeaderConstants.HEADER_USER_AGENT, Engine.VERSION_HEADER,
                    headers);
        }

        // [ifndef gwt]
        if (client.getExpectations().size() > 0) {
            addHeader(HeaderConstants.HEADER_ACCEPT_ENCODING, PreferenceWriter
                    .write(client.getAcceptedEncodings()), headers);
        }
        // [enddef]

        // ----------------------------------
        // 3) Add supported extension headers
        // ----------------------------------

        // Add the cookies
        if (request.getCookies().size() > 0) {
            addHeader(HeaderConstants.HEADER_COOKIE, CookieWriter.write(request
                    .getCookies()), headers);
        }

        // -------------------------------------
        // 4) Add user-defined extension headers
        // -------------------------------------
        Series<Parameter> additionalHeaders = (Series<Parameter>) request
                .getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        addExtensionHeaders(headers, additionalHeaders);

        // ---------------------------------------
        // 5) Add authorization headers at the end
        // ---------------------------------------

        // [ifndef gwt]
        // Add the security headers. NOTE: This must stay at the end because
        // the AWS challenge scheme requires access to all HTTP headers
        ChallengeResponse challengeResponse = request.getChallengeResponse();
        if (challengeResponse != null) {
            addHeader(
                    HeaderConstants.HEADER_AUTHORIZATION,
                    org.restlet.engine.security.AuthenticatorUtils
                            .formatResponse(challengeResponse, request, headers),
                    headers);
        }

        ChallengeResponse proxyChallengeResponse = request
                .getProxyChallengeResponse();
        if (proxyChallengeResponse != null) {
            addHeader(HeaderConstants.HEADER_PROXY_AUTHORIZATION,
                    org.restlet.engine.security.AuthenticatorUtils
                            .formatResponse(proxyChallengeResponse, request,
                                    headers), headers);
        }
        // [enddef]
    }

    // [ifndef gwt] method
    /**
     * Adds the headers based on the {@link Response} to the given
     * {@link Series}.
     * 
     * @param response
     *            The {@link Response} to copy the headers from.
     * @param headers
     *            The {@link Series} to copy the headers to.
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static void addResponseHeaders(Response response,
            Series<Parameter> headers) throws IllegalArgumentException {

        // --------------------------
        // 1) Add the general headers
        // --------------------------
        addGeneralHeaders(response, headers);

        // --------------------------------
        // 2) Add response specific headers
        // --------------------------------

        // Add the accept-ranges header
        if (response.getServerInfo().isAcceptingRanges()) {
            addHeader(HeaderConstants.HEADER_ACCEPT_RANGES, "bytes", headers);
        }

        // Add the age
        if (response.getAge() > 0) {
            addHeader(HeaderConstants.HEADER_AGE, Integer.toString(response
                    .getAge()), headers);
        }

        // Indicate the allowed methods
        if (response.getStatus().equals(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED)
                || Method.OPTIONS.equals(response.getRequest().getMethod())) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;

            for (Method method : response.getAllowedMethods()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }

                sb.append(method.getName());
            }

            addHeader(HeaderConstants.HEADER_ALLOW, sb.toString(), headers);
        }

        // Set the location URI (for redirections or creations)
        if (response.getLocationRef() != null) {
            // The location header must contain an absolute URI.
            addHeader(HeaderConstants.HEADER_LOCATION, response
                    .getLocationRef().getTargetRef().toString(), headers);
        }

        // Add proxy authentication information
        if (response.getProxyChallengeRequests() != null) {
            for (ChallengeRequest challengeRequest : response
                    .getProxyChallengeRequests()) {
                addHeader(HeaderConstants.HEADER_PROXY_AUTHENTICATE,
                        org.restlet.engine.security.AuthenticatorUtils
                                .formatRequest(challengeRequest, response,
                                        headers), headers);
            }
        }

        // Add the retry after date
        if (response.getRetryAfter() != null) {
            addHeader(HeaderConstants.HEADER_RETRY_AFTER, DateUtils
                    .format(response.getRetryAfter()), headers);
        }

        // Set the server name again
        if ((response.getServerInfo() != null)
                && (response.getServerInfo().getAgent() != null)) {
            addHeader(HeaderConstants.HEADER_SERVER, response.getServerInfo()
                    .getAgent(), headers);
        } else {
            addHeader(HeaderConstants.HEADER_SERVER, Engine.VERSION_HEADER,
                    headers);
        }

        // Send the Vary header only to none-MSIE user agents as MSIE seems
        // to support partially and badly this header (cf issue 261).
        if (!((response.getRequest().getClientInfo().getAgent() != null) && response
                .getRequest().getClientInfo().getAgent().contains("MSIE"))) {
            // Add the Vary header if content negotiation was used
            Set<Dimension> dimensions = response.getDimensions();
            String vary = DimensionWriter.writer(dimensions);

            if (vary != null) {
                addHeader(HeaderConstants.HEADER_VARY, vary, headers);
            }
        }

        // Set the security data
        if (response.getChallengeRequests() != null) {
            for (ChallengeRequest challengeRequest : response
                    .getChallengeRequests()) {
                addHeader(HeaderConstants.HEADER_WWW_AUTHENTICATE,
                        org.restlet.engine.security.AuthenticatorUtils
                                .formatRequest(challengeRequest, response,
                                        headers), headers);
            }
        }

        // ----------------------------------
        // 3) Add supported extension headers
        // ----------------------------------

        // Add the Authentication-Info header
        if (response.getAuthenticationInfo() != null) {
            addHeader(HeaderConstants.HEADER_AUTHENTICATION_INFO,
                    org.restlet.engine.security.AuthenticatorUtils
                            .formatAuthenticationInfo(response
                                    .getAuthenticationInfo()), headers);
        }

        // Add the cookie settings
        List<CookieSetting> cookies = response.getCookieSettings();
        for (int i = 0; i < cookies.size(); i++) {
            addHeader(HeaderConstants.HEADER_SET_COOKIE, CookieWriter
                    .write(cookies.get(i)), headers);
        }

        // -------------------------------------
        // 4) Add user-defined extension headers
        // -------------------------------------

        Series<Parameter> additionalHeaders = (Series<Parameter>) response
                .getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        addExtensionHeaders(headers, additionalHeaders);
    }

    /**
     * Copies entity headers into a response and ensures that a non null
     * representation is returned when at least one entity header is present.
     * 
     * @param responseHeaders
     *            The headers to copy.
     * @param representation
     *            The Representation to update.
     * @return a representation with the entity headers of the response or null
     *         if no representation has been provided and the response has not
     *         sent any entity header.
     * @throws NumberFormatException
     * @see {@link HeaderUtils#copyResponseTransportHeaders(Series, Response)}
     */
    public static Representation copyResponseEntityHeaders(
            Iterable<Parameter> responseHeaders, Representation representation)
            throws NumberFormatException {
        Representation result = (representation == null) ? new EmptyRepresentation()
                : representation;
        boolean entityHeaderFound = false;

        for (Parameter header : responseHeaders) {
            if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_TYPE)) {
                ContentType contentType = new ContentType(header.getValue());
                result.setMediaType(contentType.getMediaType());

                if ((result.getCharacterSet() == null)
                        || (contentType.getCharacterSet() != null)) {
                    result.setCharacterSet(contentType.getCharacterSet());
                }

                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_LENGTH)) {
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_EXPIRES)) {
                result.setExpirationDate(HeaderReader.readDate(header
                        .getValue(), false));
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_ENCODING)) {
                EncodingReader hr = new EncodingReader(header.getValue());
                hr.addValues(result.getEncodings());
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_LANGUAGE)) {
                LanguageReader hr = new LanguageReader(header.getValue());
                hr.addValues(result.getLanguages());
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_LAST_MODIFIED)) {
                result.setModificationDate(HeaderReader.readDate(header
                        .getValue(), false));
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_ETAG)) {
                result.setTag(Tag.parse(header.getValue()));
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_LOCATION)) {
                result.setLocationRef(header.getValue());
                entityHeaderFound = true;
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_DISPOSITION)) {
                try {
                    DispositionReader r = new DispositionReader(header
                            .getValue());
                    result.setDisposition(r.readValue());
                    entityHeaderFound = true;
                } catch (IOException ioe) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during Content-Disposition header parsing. Header: "
                                    + header.getValue(), ioe);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_RANGE)) {
                // [ifndef gwt]
                org.restlet.engine.http.header.RangeReader.update(header
                        .getValue(), result);
                entityHeaderFound = true;
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CONTENT_MD5)) {
                // [ifndef gwt]
                result.setDigest(new org.restlet.data.Digest(
                        org.restlet.data.Digest.ALGORITHM_MD5,
                        org.restlet.engine.util.Base64
                                .decode(header.getValue())));
                entityHeaderFound = true;
                // [enddef]
            }

        }

        // If no representation was initially expected and no entity header
        // is found, then do not return any representation
        if ((representation == null) && !entityHeaderFound) {
            result = null;
        }

        return result;
    }

    /**
     * Copies headers into a response.
     * 
     * @param headers
     *            The headers to copy.
     * @param response
     *            The response to update.
     */
    public static void copyResponseTransportHeaders(Series<Parameter> headers,
            Response response) {
        // Read info from headers
        for (Parameter header : headers) {
            if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_LOCATION)) {
                response.setLocationRef(header.getValue());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_AGE)) {
                try {
                    response.setAge(Integer.parseInt(header.getValue()));
                } catch (NumberFormatException nfe) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during Age header parsing. Header: "
                                    + header.getValue(), nfe);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_DATE)) {
                Date date = DateUtils.parse(header.getValue());

                if (date == null) {
                    date = new Date();
                }

                response.setDate(date);
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_RETRY_AFTER)) {
                // [ifndef gwt]
                Date retryAfter = DateUtils.parse(header.getValue());

                if (retryAfter == null) {
                    // The date might be expressed as a number of seconds
                    try {
                        int retryAfterSecs = Integer
                                .parseInt(header.getValue());
                        java.util.Calendar calendar = java.util.Calendar
                                .getInstance();
                        calendar.add(java.util.Calendar.SECOND, retryAfterSecs);
                        retryAfter = calendar.getTime();
                    } catch (NumberFormatException nfe) {
                        Context.getCurrentLogger().log(
                                Level.WARNING,
                                "Error during Retry-After header parsing. Header: "
                                        + header.getValue(), nfe);
                    }
                }

                response.setRetryAfter(retryAfter);
                // [enddef]
            } else if ((header.getName()
                    .equalsIgnoreCase(HeaderConstants.HEADER_SET_COOKIE))
                    || (header.getName()
                            .equalsIgnoreCase(HeaderConstants.HEADER_SET_COOKIE2))) {
                try {
                    CookieSettingReader cr = new CookieSettingReader(header
                            .getValue());
                    response.getCookieSettings().add(cr.readValue());
                } catch (Exception e) {
                    Context.getCurrentLogger().log(
                            Level.WARNING,
                            "Error during cookie setting parsing. Header: "
                                    + header.getValue(), e);
                }
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_WWW_AUTHENTICATE)) {
                // [ifndef gwt]
                ChallengeRequest request = org.restlet.engine.security.AuthenticatorUtils
                        .parseRequest(response, header.getValue(), headers);
                response.getChallengeRequests().add(request);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_PROXY_AUTHENTICATE)) {
                // [ifndef gwt]
                ChallengeRequest request = org.restlet.engine.security.AuthenticatorUtils
                        .parseRequest(response, header.getValue(), headers);
                response.getProxyChallengeRequests().add(request);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_AUTHENTICATION_INFO)) {
                // [ifndef gwt]
                AuthenticationInfo authenticationInfo = org.restlet.engine.security.AuthenticatorUtils
                        .parseAuthenticationInfo(header.getValue());
                response.setAuthenticationInfo(authenticationInfo);
                // [enddef]
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_SERVER)) {
                response.getServerInfo().setAgent(header.getValue());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_ALLOW)) {
                MethodReader hr = new MethodReader(header.getValue());
                hr.addValues(response.getAllowedMethods());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_VARY)) {
                DimensionReader hr = new DimensionReader(header.getValue());
                hr.addValues(response.getDimensions());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_WARNING)) {
                WarningReader hr = new WarningReader(header.getValue());
                hr.addValues(response.getWarnings());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_CACHE_CONTROL)) {
                CacheDirectiveReader hr = new CacheDirectiveReader(header
                        .getValue());
                hr.addValues(response.getCacheDirectives());
            } else if (header.getName().equalsIgnoreCase(
                    HeaderConstants.HEADER_ACCEPT_RANGES)) {
                TokenReader tr = new TokenReader(header.getValue());
                response.getServerInfo().setAcceptingRanges(
                        tr.readValues().contains("bytes"));
            }
        }
    }

    /**
     * Returns the content length of the request entity if know,
     * {@link Representation#UNKNOWN_SIZE} otherwise.
     * 
     * @return The request content length.
     */
    public static long getContentLength(Series<Parameter> headers) {
        long contentLength = Representation.UNKNOWN_SIZE;

        if (headers != null) {
            // Extract the content length header
            for (Parameter header : headers) {
                if (header.getName().equalsIgnoreCase(
                        HeaderConstants.HEADER_CONTENT_LENGTH)) {
                    try {
                        contentLength = Long.parseLong(header.getValue());
                    } catch (NumberFormatException e) {
                        contentLength = Representation.UNKNOWN_SIZE;
                    }
                }
            }
        }

        return contentLength;
    }

    /**
     * Indicates if the given character is alphabetical (a-z or A-Z).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is alphabetical (a-z or A-Z).
     */
    public static boolean isAlpha(int character) {
        return isUpperCase(character) || isLowerCase(character);
    }

    /**
     * Indicates if the given character is in ASCII range.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is in ASCII range.
     */
    public static boolean isAsciiChar(int character) {
        return (character >= 0) && (character <= 127);
    }

    /**
     * Indicates if the given character is a carriage return.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a carriage return.
     */
    public static boolean isCarriageReturn(int character) {
        return (character == 13);
    }

    /**
     * Indicates if the entity is chunked.
     * 
     * @return True if the entity is chunked.
     */
    public static boolean isChunkedEncoding(Series<Parameter> headers) {
        boolean result = false;

        if (headers != null) {
            final String header = headers.getFirstValue(
                    HeaderConstants.HEADER_TRANSFER_ENCODING, true);
            result = "chunked".equalsIgnoreCase(header);
        }

        return result;
    }

    /**
     * Indicates if the given character is a comma, the character used as header
     * value separator.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a comma.
     */
    public static boolean isComma(int character) {
        return (character == ',');
    }

    /**
     * Indicates if the given character is a comment text. It means
     * {@link #isText(int)} returns true and the character is not '(' or ')'.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a quoted text.
     */
    public static boolean isCommentText(int character) {
        return isText(character) && (character != '(') && (character != ')');
    }

    /**
     * Indicates if the connection must be closed.
     * 
     * @param headers
     *            The headers to test.
     * @return True if the connection must be closed.
     */
    public static boolean isConnectionClose(Series<Parameter> headers) {
        boolean result = false;

        if (headers != null) {
            String header = headers.getFirstValue(
                    HeaderConstants.HEADER_CONNECTION, true);
            result = "close".equalsIgnoreCase(header);
        }

        return result;
    }

    /**
     * Indicates if the given character is a control character.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a control character.
     */
    public static boolean isControlChar(int character) {
        return ((character >= 0) && (character <= 31)) || (character == 127);
    }

    /**
     * Indicates if the given character is a digit (0-9).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a digit (0-9).
     */
    public static boolean isDigit(int character) {
        return (character >= '0') && (character <= '9');
    }

    /**
     * Indicates if the given character is a double quote.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a double quote.
     */
    public static boolean isDoubleQuote(int character) {
        return (character == 34);
    }

    /**
     * Indicates if the given character is an horizontal tab.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is an horizontal tab.
     */
    public static boolean isHorizontalTab(int character) {
        return (character == 9);
    }

    /**
     * Indicates if the given character is a value separator.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a value separator.
     */
    public static boolean isLinearWhiteSpace(int character) {
        return (isCarriageReturn(character) || isSpace(character)
                || isLineFeed(character) || HeaderUtils
                .isHorizontalTab(character));
    }

    /**
     * Indicates if the given character is a line feed.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a line feed.
     */
    public static boolean isLineFeed(int character) {
        return (character == 10);
    }

    /**
     * Indicates if the given character is lower case (a-z).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is lower case (a-z).
     */
    public static boolean isLowerCase(int character) {
        return (character >= 'a') && (character <= 'z');
    }

    /**
     * Indicates if the given character marks the start of a quoted pair.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character marks the start of a quoted pair.
     */
    public static boolean isQuoteCharacter(int character) {
        return (character == '\\');
    }

    /**
     * Indicates if the given character is a quoted text. It means
     * {@link #isText(int)} returns true and {@link #isDoubleQuote(int)} returns
     * false.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a quoted text.
     */
    public static boolean isQuotedText(int character) {
        return isText(character) && !isDoubleQuote(character);
    }

    /**
     * Indicates if the given character is a semicolon, the character used as
     * header parameter separator.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a semicolon.
     */
    public static boolean isSemiColon(int character) {
        return (character == ';');
    }

    /**
     * Indicates if the given character is a separator.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a separator.
     */
    public static boolean isSeparator(int character) {
        switch (character) {
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '/':
        case '[':
        case ']':
        case '?':
        case '=':
        case '{':
        case '}':
        case ' ':
        case '\t':
            return true;

        default:
            return false;
        }
    }

    /**
     * Indicates if the given character is a space.
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a space.
     */
    public static boolean isSpace(int character) {
        return (character == 32);
    }

    /**
     * Indicates if the given character is textual (ASCII and not a control
     * character).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is textual (ASCII and not a control
     *         character).
     */
    public static boolean isText(int character) {
        return isAsciiChar(character) && !isControlChar(character);
    }

    /**
     * Indicates if the token is valid.<br>
     * Only contains valid token characters.
     * 
     * @param token
     *            The token to check
     * @return True if the token is valid.
     */
    public static boolean isToken(CharSequence token) {
        for (int i = 0; i < token.length(); i++) {
            if (!isTokenChar(token.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Indicates if the given character is a token character (text and not a
     * separator).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is a token character (text and not a
     *         separator).
     */
    public static boolean isTokenChar(int character) {
        return isText(character) && !isSeparator(character);
    }

    /**
     * Indicates if the given character is upper case (A-Z).
     * 
     * @param character
     *            The character to test.
     * @return True if the given character is upper case (A-Z).
     */
    public static boolean isUpperCase(int character) {
        return (character >= 'A') && (character <= 'Z');
    }

    // [ifndef gwt] method
    /**
     * Writes a new line.
     * 
     * @param os
     *            The output stream.
     * @throws IOException
     */
    public static void writeCRLF(OutputStream os) throws IOException {
        os.write(13); // CR
        os.write(10); // LF
    }

    // [ifndef gwt] method
    /**
     * Writes a header line.
     * 
     * @param header
     *            The header to write.
     * @param os
     *            The output stream.
     * @throws IOException
     */
    public static void writeHeaderLine(Parameter header, OutputStream os)
            throws IOException {
        os.write(header.getName().getBytes());
        os.write(':');
        os.write(' ');

        if (header.getValue() != null) {
            os.write(header.getValue().getBytes());
        }

        os.write(13); // CR
        os.write(10); // LF
    }

    /**
     * Private constructor to ensure that the class acts as a true utility class
     * i.e. it isn't instantiable and extensible.
     */
    private HeaderUtils() {
    }
}
