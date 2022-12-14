/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.oozie.BuildInfo;
import org.apache.oozie.cli.ValidationUtil;
import org.apache.oozie.client.rest.JsonTags;
import org.apache.oozie.client.rest.JsonToBean;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.client.retry.ConnectionRetriableClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;


/**
 * Client API to submit and manage Oozie workflow jobs against an Oozie instance.
 * <p>
 * This class is thread safe.
 * <p>
 * Syntax for filter for the {@link #getJobsInfo(String)} {@link #getJobsInfo(String, int, int)} methods:
 * <code>[NAME=VALUE][;NAME=VALUE]*</code>.
 * <p>
 * Valid filter names are:
 * <ul>
 * <li>name: the workflow application name from the workflow definition.</li>
 * <li>user: the user that submitted the job.</li>
 * <li>group: the group for the job.</li>
 * <li>status: the status of the job.</li>
 * </ul>
 * <p>
 * The query will do an AND among all the filter names. The query will do an OR among all the filter values for the same
 * name. Multiple values must be specified as different name value pairs.
 */
public class OozieClient {

    public static final long WS_PROTOCOL_VERSION_0 = 0;

    public static final long WS_PROTOCOL_VERSION_1 = 1;

    public static final long WS_PROTOCOL_VERSION = 2; // pointer to current version

    public static final String USER_NAME = "user.name";

    @Deprecated
    public static final String GROUP_NAME = "group.name";

    public static final String JOB_ACL = "oozie.job.acl";

    public static final String APP_PATH = "oozie.wf.application.path";

    public static final String COORDINATOR_APP_PATH = "oozie.coord.application.path";

    public static final String BUNDLE_APP_PATH = "oozie.bundle.application.path";

    public static final String BUNDLE_ID = "oozie.bundle.id";

    public static final String EXTERNAL_ID = "oozie.wf.external.id";

    public static final String WORKFLOW_NOTIFICATION_PROXY = "oozie.wf.workflow.notification.proxy";

    public static final String WORKFLOW_NOTIFICATION_URL = "oozie.wf.workflow.notification.url";

    public static final String ACTION_NOTIFICATION_URL = "oozie.wf.action.notification.url";

    public static final String COORD_ACTION_NOTIFICATION_URL = "oozie.coord.action.notification.url";

    public static final String COORD_ACTION_NOTIFICATION_PROXY = "oozie.coord.action.notification.proxy";

    public static final String RERUN_SKIP_NODES = "oozie.wf.rerun.skip.nodes";

    public static final String RERUN_FAIL_NODES = "oozie.wf.rerun.failnodes";

    public static final String LOG_TOKEN = "oozie.wf.log.token";

    public static final String ACTION_MAX_RETRIES = "oozie.wf.action.max.retries";

    public static final String ACTION_RETRY_INTERVAL = "oozie.wf.action.retry.interval";

    public static final String FILTER_USER = "user";

    public static final String FILTER_TEXT = "text";

    public static final String FILTER_GROUP = "group";

    public static final String FILTER_NAME = "name";

    public static final String FILTER_STATUS = "status";

    public static final String FILTER_NOMINAL_TIME = "nominaltime";

    public static final String FILTER_FREQUENCY = "frequency";

    public static final String FILTER_ID = "id";

    public static final String FILTER_UNIT = "unit";

    public static final String FILTER_JOBID = "jobid";

    public static final String FILTER_APPNAME = "appname";

    public static final String FILTER_CREATED_TIME_START = "startcreatedtime";

    public static final String FILTER_CREATED_TIME_END = "endcreatedtime";

    public static final String SLA_DISABLE_ALERT = "oozie.sla.disable.alerts";

    public static final String SLA_ENABLE_ALERT = "oozie.sla.enable.alerts";

    public static final String SLA_DISABLE_ALERT_OLDER_THAN = SLA_DISABLE_ALERT + ".older.than";

    public static final String SLA_DISABLE_ALERT_COORD = SLA_DISABLE_ALERT + ".coord";

    public static final String CHANGE_VALUE_ENDTIME = "endtime";

    public static final String CHANGE_VALUE_PAUSETIME = "pausetime";

    public static final String CHANGE_VALUE_CONCURRENCY = "concurrency";

    public static final String CHANGE_VALUE_STATUS = "status";

    public static final String LIBPATH = "oozie.libpath";

    public static final String CONFIG_PATH = "oozie.default.configuration.path";

    public static final String USE_SYSTEM_LIBPATH = "oozie.use.system.libpath";

    public static final String OOZIE_SUSPEND_ON_NODES = "oozie.suspend.on.nodes";

    public static final String FILTER_SORT_BY = "sortby";

    public static final String CONFIG_KEY_GENERATED_XML = "oozie.jobs.api.generated.xml";

    public enum SORT_BY {
        createdTime("createdTimestamp"), lastModifiedTime("lastModifiedTimestamp");
        private final String fullname;

        SORT_BY(String fullname) {
            this.fullname = fullname;
        }

        public String getFullname() {
            return fullname;
        }
    }

    public enum SYSTEM_MODE {
        NORMAL, NOWEBSERVICE, SAFEMODE
    }

    private static final Set<String> COMPLETED_WF_STATUSES = new HashSet<>();
    private static final Set<String> COMPLETED_COORD_AND_BUNDLE_STATUSES = new HashSet<>();
    private static final Set<String> COMPLETED_COORD_ACTION_STATUSES = new HashSet<>();
    static {
        COMPLETED_WF_STATUSES.add(WorkflowJob.Status.FAILED.toString());
        COMPLETED_WF_STATUSES.add(WorkflowJob.Status.KILLED.toString());
        COMPLETED_WF_STATUSES.add(WorkflowJob.Status.SUCCEEDED.toString());
        COMPLETED_COORD_AND_BUNDLE_STATUSES.add(Job.Status.FAILED.toString());
        COMPLETED_COORD_AND_BUNDLE_STATUSES.add(Job.Status.KILLED.toString());
        COMPLETED_COORD_AND_BUNDLE_STATUSES.add(Job.Status.SUCCEEDED.toString());
        COMPLETED_COORD_AND_BUNDLE_STATUSES.add(Job.Status.DONEWITHERROR.toString());
        COMPLETED_COORD_AND_BUNDLE_STATUSES.add(Job.Status.IGNORED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.FAILED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.IGNORED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.KILLED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.SKIPPED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.SUCCEEDED.toString());
        COMPLETED_COORD_ACTION_STATUSES.add(CoordinatorAction.Status.TIMEDOUT.toString());
    }

    /**
     * debugMode =0 means no debugging. 1 means debugging on.
     */
    public int debugMode = 0;

    private int retryCount = 4;

    private String baseUrl;
    private String protocolUrl;
    private boolean validatedVersion = false;
    private JSONArray supportedVersions;
    private final Map<String, String> headers = new HashMap<>();

    private static final ThreadLocal<String> USER_NAME_TL = new ThreadLocal<>();

    /**
     * Allows to impersonate other users in the Oozie server. The current user
     * must be configured as a proxyuser in Oozie.
     * <p>
     * IMPORTANT: impersonation happens only with Oozie client requests done within
     * doAs() calls.
     *
     * @param <T> the type of the callable
     * @param userName user to impersonate.
     * @param callable callable with {@link OozieClient} calls impersonating the specified user.
     * @return any response returned by the {@link Callable#call()} method.
     * @throws Exception thrown by the {@link Callable#call()} method.
     */
    public static <T> T doAs(String userName, Callable<T> callable) throws Exception {
        notEmpty(userName, "userName");
        notNull(callable, "callable");
        try {
            USER_NAME_TL.set(userName);
            return callable.call();
        }
        finally {
            USER_NAME_TL.remove();
        }
    }

    protected OozieClient() {
    }

    /**
     * Create a Workflow client instance.
     *
     * @param oozieUrl URL of the Oozie instance it will interact with.
     */
    public OozieClient(String oozieUrl) {
        this.baseUrl = notEmpty(oozieUrl, "oozieUrl");
        if (!this.baseUrl.endsWith("/")) {
            this.baseUrl += "/";
        }
    }

    /**
     * Return the Oozie URL of the workflow client instance.
     * <p>
     * This URL is the base URL fo the Oozie system, with not protocol versioning.
     *
     * @return the Oozie URL of the workflow client instance.
     */
    public String getOozieUrl() {
        return baseUrl;
    }

    /**
     * Return the Oozie URL used by the client and server for WS communications.
     * <p>
     * This URL is the original URL plus the versioning element path.
     *
     * @return the Oozie URL used by the client and server for communication.
     * @throws OozieClientException thrown in the client and the server are not protocol compatible.
     */
    public String getProtocolUrl() throws OozieClientException {
        validateWSVersion();
        return protocolUrl;
    }

    /**
     * @return current debug Mode
     */
    public int getDebugMode() {
        return debugMode;
    }

    /**
     * Set debug mode.
     *
     * @param debugMode : 0 means no debugging. 1 means debugging
     */
    public void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }

    public int getRetryCount() {
        return retryCount;
    }


    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    private String getBaseURLForVersion(long protocolVersion) throws OozieClientException {
        try {
            if (supportedVersions == null) {
                supportedVersions = getSupportedProtocolVersions();
            }
            if (supportedVersions == null) {
                throw new OozieClientException("HTTP error", "no response message");
            }
            if (supportedVersions.contains(protocolVersion)) {
                return baseUrl + "v" + protocolVersion + "/";
            }
            else {
                throw new OozieClientException(OozieClientException.UNSUPPORTED_VERSION, "Protocol version "
                        + protocolVersion + " is not supported");
            }
        }
        catch (IOException e) {
            throw new OozieClientException(OozieClientException.IO_ERROR, e);
        }
    }

    /**
     * Validate that the Oozie client and server instances are protocol compatible.
     *
     * @throws OozieClientException thrown in the client and the server are not protocol compatible.
     */
    public synchronized void validateWSVersion() throws OozieClientException {
        if (!validatedVersion) {
            try {
                supportedVersions = getSupportedProtocolVersions();
                if (supportedVersions == null) {
                    throw new OozieClientException("HTTP error", "no response message");
                }
                if (!supportedVersions.contains(WS_PROTOCOL_VERSION)
                        && !supportedVersions.contains(WS_PROTOCOL_VERSION_1)
                        && !supportedVersions.contains(WS_PROTOCOL_VERSION_0)) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Supported version [").append(WS_PROTOCOL_VERSION)
                            .append("] or less, Unsupported versions[");
                    String separator = "";
                    for (Object version : supportedVersions) {
                        msg.append(separator).append(version);
                    }
                    msg.append("]");
                    throw new OozieClientException(OozieClientException.UNSUPPORTED_VERSION, msg.toString());
                }
                if (supportedVersions.contains(WS_PROTOCOL_VERSION)) {
                    protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION + "/";
                }
                else if (supportedVersions.contains(WS_PROTOCOL_VERSION_1)) {
                    protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION_1 + "/";
                }
                else {
                    if (supportedVersions.contains(WS_PROTOCOL_VERSION_0)) {
                        protocolUrl = baseUrl + "v" + WS_PROTOCOL_VERSION_0 + "/";
                    }
                }
            }
            catch (IOException ex) {
                throw new OozieClientException(OozieClientException.IO_ERROR, ex);
            }
            validatedVersion = true;
        }
    }

    private JSONArray getSupportedProtocolVersions() throws IOException, OozieClientException {
        JSONArray versions = null;
        final URL url = new URL(baseUrl + RestConstants.VERSIONS);

        HttpURLConnection conn = createRetryableConnection(url, "GET");

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            versions = (JSONArray) JSONValue.parse(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }
        else {
            handleError(conn);
        }
        return versions;
    }

    /**
     * Create an empty configuration with just the {@link #USER_NAME} set to the JVM user name.
     *
     * @return an empty configuration.
     */
    public Properties createConfiguration() {
        Properties conf = new Properties();
        conf.setProperty(USER_NAME, getUserName());
        return conf;
    }

    /**
     * Set a HTTP header to be used in the WS requests by the workflow instance.
     *
     * @param name header name.
     * @param value header value.
     */
    public void setHeader(String name, String value) {
        headers.put(notEmpty(name, "name"), notNull(value, "value"));
    }

    /**
     * Get the value of a set HTTP header from the workflow instance.
     *
     * @param name header name.
     * @return header value, <code>null</code> if not set.
     */
    public String getHeader(String name) {
        return headers.get(notEmpty(name, "name"));
    }

    /**
     * Get the set HTTP header
     *
     * @return map of header key and value
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Remove a HTTP header from the workflow client instance.
     *
     * @param name header name.
     */
    public void removeHeader(String name) {
        headers.remove(notEmpty(name, "name"));
    }

    /**
     * Return an iterator with all the header names set in the workflow instance.
     *
     * @return header names.
     */
    public Iterator<String> getHeaderNames() {
        return Collections.unmodifiableMap(headers).keySet().iterator();
    }

    private URL createURL(Long protocolVersion, String collection, String resource, Map<String, String> parameters)
            throws IOException, OozieClientException {
        validateWSVersion();
        StringBuilder sb = new StringBuilder();
        if (protocolVersion == null) {
            sb.append(protocolUrl);
        }
        else {
            sb.append(getBaseURLForVersion(protocolVersion));
        }
        sb.append(collection);
        if (resource != null && resource.length() > 0) {
            sb.append("/").append(resource);
        }
        if (parameters.size() > 0) {
            String separator = "?";
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                if (param.getValue() != null) {
                    sb.append(separator).append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8.name())).append("=")
                            .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.name()));
                    separator = "&";
                }
            }
        }
        return new URL(sb.toString());
    }

    private boolean validateCommand(String url) {
        {
            if (protocolUrl.contains(baseUrl + "v0")) {
                if (url.contains("dryrun") || url.contains("jobtype=c") || url.contains("systemmode")) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Create retryable http connection to oozie server.
     *
     * @param url URL to create connection to
     * @param method method to use - GET, POST, PUT
     * @return connection
     * @throws IOException in case of an exception during connection setup
     */
    protected HttpURLConnection createRetryableConnection(final URL url, final String method) throws IOException {
        return (HttpURLConnection) new ConnectionRetriableClient(getRetryCount()) {
            @Override
            public Object doExecute(URL url, String method) throws IOException, OozieClientException {
                return createConnection(url, method);
            }
        }.execute(url, method);
    }

    /**
     * Create http connection to oozie server.
     *
     * @param url URL to create connection to
     * @param method method to use - GET, POST, PUT
     * @return connection
     * @throws IOException in case of an exception during connection setup
     * @throws OozieClientException if the connection can't be set up
     */
    protected HttpURLConnection createConnection(URL url, String method) throws IOException, OozieClientException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (method.equals("POST") || method.equals("PUT")) {
            conn.setDoOutput(true);
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }
        return conn;
    }

    protected abstract class ClientCallable<T> implements Callable<T> {
        private final String method;
        private final String collection;
        private final String resource;
        private final Map<String, String> params;
        private final Long protocolVersion;

        public ClientCallable(String method, String collection, String resource, Map<String, String> params) {
            this(method, null, collection, resource, params);
        }

        public ClientCallable(String method, Long protocolVersion, String collection, String resource, Map<String, String> params) {
            this.method = method;
            this.protocolVersion = protocolVersion;
            this.collection = collection;
            this.resource = resource;
            this.params = params;
        }

        public T call() throws OozieClientException {
            try {
                URL url = createURL(protocolVersion, collection, resource, params);
                if (validateCommand(url.toString())) {
                    if (getDebugMode() > 0) {
                        System.out.println(method + " " + url);
                    }
                    return call(createRetryableConnection(url, method));
                }
                else {
                    System.out.println("Option not supported in target server. Supported only on Oozie-2.0 or greater."
                            + " Use 'oozie help' for details");
                    throw new OozieClientException(OozieClientException.UNSUPPORTED_VERSION, new Exception());
                }
            }
            catch (IOException ex) {
                throw new OozieClientException(OozieClientException.IO_ERROR, ex);
            }
        }

        protected abstract T call(HttpURLConnection conn) throws IOException, OozieClientException;

    }

    protected abstract class MapClientCallable extends ClientCallable<Map<String, String>> {

        MapClientCallable(String method, String collection, String resource, Map<String, String> params) {
            super(method, collection, resource, params);
        }

        @Override
        protected Map<String, String> call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                Map<String, String> map = new HashMap<>();
                for (Object key : json.keySet()) {
                    map.put((String)key, (String)json.get(key));
                }
                return map;
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    static void handleError(HttpURLConnection conn) throws IOException, OozieClientException {
        int status = conn.getResponseCode();
        String error = conn.getHeaderField(RestConstants.OOZIE_ERROR_CODE);
        String message = conn.getHeaderField(RestConstants.OOZIE_ERROR_MESSAGE);

        if (error == null) {
            error = "HTTP error code: " + status;
        }

        if (message == null) {
            message = conn.getResponseMessage();
        }
        throw new OozieClientException(error, message);
    }

    static Map<String, String> prepareParams(String... params) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < params.length; i = i + 2) {
            map.put(params[i], params[i + 1]);
        }
        String doAsUserName = USER_NAME_TL.get();
        if (doAsUserName != null) {
            map.put(RestConstants.DO_AS_PARAM, doAsUserName);
        }
        return map;
    }

    public void writeToXml(Properties props, OutputStream out) throws IOException {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element conf = doc.createElement("configuration");
            doc.appendChild(conf);
            conf.appendChild(doc.createTextNode("\n"));
            for (String name : props.stringPropertyNames()) { // Properties whose key or value is not of type String are omitted.
                String value = props.getProperty(name);
                Element propNode = doc.createElement("property");
                conf.appendChild(propNode);

                Element nameNode = doc.createElement("name");
                nameNode.appendChild(doc.createTextNode(name.trim()));
                propNode.appendChild(nameNode);

                Element valueNode = doc.createElement("value");
                valueNode.appendChild(doc.createTextNode(value.trim()));
                propNode.appendChild(valueNode);

                conf.appendChild(doc.createTextNode("\n"));
            }

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(out);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            Transformer transformer = transFactory.newTransformer();
            transformer.transform(source, result);
            if (getDebugMode() > 0) {
                result = new StreamResult(System.out);
                transformer.transform(source, result);
                System.out.println();
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class JobSubmit extends ClientCallable<String> {
        private final Properties conf;
        private final String generatedXml;

        JobSubmit(final Properties conf, final boolean start, final String generatedXml) {
            super("POST", RestConstants.JOBS, "",
                    (start)
                            ? prepareParams(RestConstants.ACTION_PARAM,
                                    RestConstants.JOB_ACTION_START,
                                    RestConstants.USER_PARAM,
                                    getUserName())
                            : prepareParams(RestConstants.USER_PARAM,
                                    getUserName()));
            this.conf = notNull(conf, "conf");
            this.generatedXml = generatedXml;
        }

        JobSubmit(String jobId, Properties conf) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_ACTION_RERUN, RestConstants.USER_PARAM, getUserName()));
            this.conf = notNull(conf, "conf");
            this.generatedXml = null;
        }

        public JobSubmit(Properties conf, String jobActionDryrun) {
            super("POST", RestConstants.JOBS, "", prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_ACTION_DRYRUN, RestConstants.USER_PARAM, getUserName()));
            this.conf = notNull(conf, "conf");
            this.generatedXml = null;
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);

            if (!Strings.isNullOrEmpty(generatedXml)) {
                conf.setProperty(CONFIG_KEY_GENERATED_XML, generatedXml);
            }

            writeToXml(conf, conn.getOutputStream());

            if (conn.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                JSONObject json = (JSONObject) JSONValue.parse(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                return (String) json.get(JsonTags.JOB_ID);
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handleError(conn);
            }

            return null;
        }
    }

    /**
     * Submit a workflow job.
     *
     * @param conf job configuration.
     * @return the job Id.
     * @throws OozieClientException thrown if the job could not be submitted.
     */
    public String submit(Properties conf) throws OozieClientException {
        return (new JobSubmit(conf, false, null)).call();
    }

    public String submit(final Properties conf, final String generatedXml) throws OozieClientException {
        return (new JobSubmit(conf, false, generatedXml)).call();
    }

    private class JobAction extends ClientCallable<Void> {

        JobAction(String jobId, String action) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM, action));
        }

        JobAction(String jobId, String action, String params) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM, action,
                    RestConstants.JOB_CHANGE_VALUE, params));
        }

        @Override
        protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
            if (!(conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                handleError(conn);
            }
            return null;
        }
    }

    private class JobsAction extends ClientCallable<JSONObject> {

        JobsAction(String action, String filter, String jobType, int start, int len) {
            super("PUT", RestConstants.JOBS, "",
                    prepareParams(RestConstants.ACTION_PARAM, action,
                            RestConstants.JOB_FILTER_PARAM, filter, RestConstants.JOBTYPE_PARAM, jobType,
                            RestConstants.OFFSET_PARAM, Integer.toString(start),
                            RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected JSONObject call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                return (JSONObject) JSONValue.parse(reader);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }
    /**
     * Update coord definition.
     *
     * @param jobId the job id
     * @param conf the conf
     * @param dryrun the dryrun
     * @param showDiff the show diff
     * @return the string
     * @throws OozieClientException the oozie client exception
     */
    public String updateCoord(String jobId, Properties conf, String dryrun, String showDiff)
            throws OozieClientException {
        return (new UpdateCoord(jobId, conf, dryrun, showDiff)).call();
    }

    /**
     * Update coord definition without properties.
     *
     * @param jobId the job id
     * @param dryrun the dryrun
     * @param showDiff the show diff
     * @return the string
     * @throws OozieClientException the oozie client exception
     */
    public String updateCoord(String jobId, String dryrun, String showDiff) throws OozieClientException {
        return (new UpdateCoord(jobId, dryrun, showDiff)).call();
    }

    /**
     * The Class UpdateCoord.
     */
    private class UpdateCoord extends ClientCallable<String> {
        private final Properties conf;

        public UpdateCoord(String jobId, Properties conf, String jobActionDryrun, String showDiff) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_COORD_UPDATE, RestConstants.JOB_ACTION_DRYRUN, jobActionDryrun,
                    RestConstants.JOB_ACTION_SHOWDIFF, showDiff));
            this.conf = conf;
        }

        public UpdateCoord(String jobId, String jobActionDryrun, String showDiff) {
            this(jobId, new Properties(), jobActionDryrun, showDiff);
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            writeToXml(conf, conn.getOutputStream());

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject json = (JSONObject) JSONValue.parse(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                JSONObject update = (JSONObject) json.get(JsonTags.COORD_UPDATE);
                if (update != null) {
                    return (String) update.get(JsonTags.COORD_UPDATE_DIFF);
                }
                else {
                    return "";
                }
            }
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * dryrun for a given job
     *
     * @param conf Job configuration.
     * @return new job.
     * @throws org.apache.oozie.client.OozieClientException thrown if the job could not be submitted.
     */
    public String dryrun(Properties conf) throws OozieClientException {
        return new JobSubmit(conf, RestConstants.JOB_ACTION_DRYRUN).call();
    }

    /**
     * Start a workflow job.
     *
     * @param jobId job Id.
     * @throws OozieClientException thrown if the job could not be started.
     */
    public void start(String jobId) throws OozieClientException {
        new JobAction(jobId, RestConstants.JOB_ACTION_START).call();
    }

    /**
     * Submit and start a workflow job.
     *
     * @param conf job configuration.
     * @return the job Id.
     * @throws OozieClientException thrown if the job could not be submitted.
     */
    public String run(Properties conf) throws OozieClientException {
        return (new JobSubmit(conf, true, null)).call();
    }

    public String run(final Properties conf, final String generatedXml) throws OozieClientException {
        return (new JobSubmit(conf, true, generatedXml)).call();
    }

    /**
     * Rerun a workflow job.
     *
     * @param jobId job Id to rerun.
     * @param conf configuration information for the rerun.
     * @throws OozieClientException thrown if the job could not be started.
     */
    public void reRun(String jobId, Properties conf) throws OozieClientException {
        new JobSubmit(jobId, conf).call();
    }

    /**
     * Suspend a workflow job.
     *
     * @param jobId job Id.
     * @throws OozieClientException thrown if the job could not be suspended.
     */
    public void suspend(String jobId) throws OozieClientException {
        new JobAction(jobId, RestConstants.JOB_ACTION_SUSPEND).call();
    }

    /**
     * Resume a workflow job.
     *
     * @param jobId job Id.
     * @throws OozieClientException thrown if the job could not be resume.
     */
    public void resume(String jobId) throws OozieClientException {
        new JobAction(jobId, RestConstants.JOB_ACTION_RESUME).call();
    }

    /**
     * Kill a workflow/coord/bundle job.
     *
     * @param jobId job Id.
     * @throws OozieClientException thrown if the job could not be killed.
     */
    public void kill(String jobId) throws OozieClientException {
        new JobAction(jobId, RestConstants.JOB_ACTION_KILL).call();
    }

    /**
     * Kill coordinator actions
     * @param jobId coordinator Job Id
     * @param rangeType type 'date' if -date is used, 'action-num' if -action is used
     * @param scope kill scope for date or action nums
     * @return list of coordinator actions that underwent kill
     * @throws OozieClientException thrown if some actions could not be killed.
     */
    public List<CoordinatorAction> kill(String jobId, String rangeType, String scope) throws OozieClientException {
        return new CoordActionsKill(jobId, rangeType, scope).call();
    }

    public JSONObject bulkModifyJobs(String actionType, String filter, String jobType, int start, int len)
            throws OozieClientException {
        return new JobsAction(actionType, filter, jobType, start, len).call();
    }

    public JSONObject killJobs(String filter, String jobType, int start, int len)
            throws OozieClientException {
        return bulkModifyJobs("kill", filter, jobType, start, len);
    }

    public JSONObject suspendJobs(String filter, String jobType, int start, int len)
            throws OozieClientException {
        return bulkModifyJobs("suspend", filter, jobType, start, len);
    }

    public JSONObject resumeJobs(String filter, String jobType, int start, int len)
            throws OozieClientException {
        return bulkModifyJobs("resume", filter, jobType, start, len);
    }
    /**
     * Change a coordinator job.
     *
     * @param jobId job Id.
     * @param changeValue change value.
     * @throws OozieClientException thrown if the job could not be changed.
     */
    public void change(String jobId, String changeValue) throws OozieClientException {
        new JobAction(jobId, RestConstants.JOB_ACTION_CHANGE, changeValue).call();
    }

    /**
     * Ignore a coordinator job.
     *
     * @param jobId coord job Id.
     * @param scope list of coord actions to be ignored
     * @return the list ignored actions or null if there are none
     * @throws OozieClientException thrown if the job could not be changed.
     * @return list of ignored coordinator jobs
     */
    public List<CoordinatorAction> ignore(String jobId, String scope) throws OozieClientException {
        return new CoordIgnore(jobId, RestConstants.JOB_COORD_SCOPE_ACTION, scope).call();
    }

    private class JobInfo extends ClientCallable<WorkflowJob> {

        JobInfo(String jobId, int start, int len) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_INFO, RestConstants.OFFSET_PARAM, Integer.toString(start),
                    RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected WorkflowJob call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createWorkflowJob(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class JMSInfo extends ClientCallable<JMSConnectionInfo> {

        JMSInfo() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_JMS_INFO, prepareParams());
        }

        protected JMSConnectionInfo call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createJMSConnectionInfo(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class WorkflowActionInfo extends ClientCallable<WorkflowAction> {
        WorkflowActionInfo(String actionId) {
            super("GET", RestConstants.JOB, notEmpty(actionId, "id"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_INFO));
        }

        @Override
        protected WorkflowAction call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createWorkflowAction(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class WorkflowActionRetriesInfo extends ClientCallable<List<Map<String, String>>> {
        WorkflowActionRetriesInfo(String actionId) {
            super("GET", RestConstants.JOB, notEmpty(actionId, "id"),
                    prepareParams(RestConstants.JOB_SHOW_PARAM, RestConstants.JOB_SHOW_ACTION_RETRIES_PARAM));
        }

        @Override
        protected List<Map<String, String>> call(HttpURLConnection conn)
                throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return new ObjectMapper().readValue(json.get(JsonTags.WORKFLOW_ACTION_RETRIES).toString(),
                        new TypeReference<List<Map<String, String>>>() {
                        });
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Get the info of a workflow job.
     *
     * @param jobId job Id.
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public WorkflowJob getJobInfo(String jobId) throws OozieClientException {
        return getJobInfo(jobId, 0, 0);
    }

    /**
     * Get the JMS Connection info
     * @return JMSConnectionInfo object
     * @throws OozieClientException thrown if the JMS info could not be retrieved.
     */
    public JMSConnectionInfo getJMSConnectionInfo() throws OozieClientException {
        return new JMSInfo().call();
    }

    /**
     * Get the info of a workflow job and subset actions.
     *
     * @param jobId job Id.
     * @param start starting index in the list of actions belonging to the job
     * @param len number of actions to be returned
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public WorkflowJob getJobInfo(String jobId, int start, int len) throws OozieClientException {
        return new JobInfo(jobId, start, len).call();
    }

    /**
     * Get the info of a workflow action.
     *
     * @param actionId Id.
     * @return the workflow action info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public WorkflowAction getWorkflowActionInfo(String actionId) throws OozieClientException {
        return new WorkflowActionInfo(actionId).call();
    }


    /**
     * Get the info of a workflow action.
     *
     * @param actionId Id.
     * @return the workflow action retries info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public List<Map<String, String>> getWorkflowActionRetriesInfo(String actionId) throws OozieClientException {
        return new WorkflowActionRetriesInfo(actionId).call();
    }

    /**
     * Get the log of a workflow job.
     *
     * @param jobId job Id.
     * @return the job log.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public String getJobLog(String jobId) throws OozieClientException {
        return new JobLog(jobId).call();
    }

    /**
     * Get the audit log of a job.
     *
     * @param jobId the id of the job
     * @param ps the stream to write the result into
     * @throws OozieClientException thrown if the job audit log could not be retrieved.
     */
    public void getJobAuditLog(String jobId, PrintStream ps) throws OozieClientException {
        new JobAuditLog(jobId, ps).call();
    }

    /**
     * Get the log of a job.
     *
     * @param jobId job Id.
     * @param logRetrievalType Based on which filter criteria the log is retrieved
     * @param logRetrievalScope Value for the retrieval type
     * @param logFilter log filter
     * @param ps Printstream of command line interface
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public void getJobLog(String jobId, String logRetrievalType, String logRetrievalScope, String logFilter,
            PrintStream ps) throws OozieClientException {
        new JobLog(jobId, logRetrievalType, logRetrievalScope, logFilter, ps).call();
    }

    /**
     * Get the error log of a job.
     *
     * @param jobId the id of the job
     * @param ps the stream to write the result into
     * @throws OozieClientException thrown if the job log could not be retrieved.
     */
    public void getJobErrorLog(String jobId, PrintStream ps) throws OozieClientException {
        new JobErrorLog(jobId, ps).call();
    }

    /**
     * Get the log of a job.
     *
     * @param jobId job Id.
     * @param logRetrievalType Based on which filter criteria the log is retrieved
     * @param logRetrievalScope Value for the retrieval type
     * @param ps Printstream of command line interface
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public void getJobLog(String jobId, String logRetrievalType, String logRetrievalScope, PrintStream ps)
            throws OozieClientException {
        getJobLog(jobId, logRetrievalType, logRetrievalScope, null, ps);
    }

    private class JobLog extends JobMetadata {
        JobLog(String jobId) {
            super(jobId, RestConstants.JOB_SHOW_LOG);
        }
        JobLog(String jobId, String logRetrievalType, String logRetrievalScope, String logFilter, PrintStream ps) {
            super(jobId, logRetrievalType, logRetrievalScope, RestConstants.JOB_SHOW_LOG, logFilter, ps);
        }
    }

    private class JobErrorLog extends JobMetadata {
        JobErrorLog(String jobId, PrintStream ps) {
            super(jobId, RestConstants.JOB_SHOW_ERROR_LOG, ps);
        }
    }

    private class JobAuditLog extends JobMetadata {
        JobAuditLog(String jobId, PrintStream ps) {
            super(jobId, RestConstants.JOB_SHOW_AUDIT_LOG, ps);
        }
    }


    /**
     * Gets the JMS topic name for a particular job
     * @param jobId given jobId
     * @return the JMS topic name
     * @throws OozieClientException thrown if the JMS topic could not be retrieved.
     */
    public String getJMSTopicName(String jobId) throws OozieClientException {
        return new JMSTopic(jobId).call();
    }

    private class JMSTopic extends ClientCallable<String> {

        JMSTopic(String jobId) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_JMS_TOPIC));
        }

        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return (String) json.get(JsonTags.JMS_TOPIC_NAME);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Get the definition of a workflow job.
     *
     * @param jobId job Id.
     * @return the job log.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public String getJobDefinition(String jobId) throws OozieClientException {
        return new JobDefinition(jobId).call();
    }

    /**
     * Get coord action missing dependencies
     * @param jobId the id of the job
     * @param actionList the list of coordinator actions
     * @param dates a comma-separated list of date ranges. Each date range element is specified with two dates separated by '::'
     * @param ps the stream to write the result into
     * @throws OozieClientException thrown if the info could not be retrieved.
     */
    public void getCoordActionMissingDependencies(String jobId, String actionList, String dates, PrintStream ps)
            throws OozieClientException {
        new CoordActionMissingDependencies(jobId, actionList, dates, ps).call();
    }

    /**
     * Get coord action missing dependencies
     * @param jobId the id of the job
     * @param actionList the list of coordinator actions
     * @param dates a comma-separated list of date ranges. Each date range element is specified with two dates separated by '::'
     * @throws OozieClientException thrown if the info could not be retrieved.
     */
    public void getCoordActionMissingDependencies(String jobId, String actionList, String dates)
            throws OozieClientException {
        new CoordActionMissingDependencies(jobId, actionList, dates).call();
    }

    private class CoordActionMissingDependencies extends ClientCallable<String> {
        PrintStream printStream;
        public CoordActionMissingDependencies(String jobId, String actionList, String dates, PrintStream ps) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.COORD_ACTION_MISSING_DEPENDENCIES, RestConstants.JOB_COORD_SCOPE_ACTION_LIST, actionList,
                    RestConstants.JOB_COORD_SCOPE_DATE, dates));
            this.printStream = ps;
        }

        public CoordActionMissingDependencies(String jobId, String actionList, String dates) {
            this(jobId, actionList, dates, System.out);
        }


        @SuppressWarnings("unchecked")
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                if (json != null) {
                    JSONArray inputDependencies = (JSONArray) json.get(JsonTags.COORD_ACTION_MISSING_DEPENDENCIES);
                    for (Object dependencies : inputDependencies) {
                        printStream.println();
                        printStream.println("CoordAction : "
                                + ((JSONObject) dependencies).get(JsonTags.COORDINATOR_ACTION_ID));
                        if (((JSONObject) dependencies).get(JsonTags.COORD_ACTION_FIRST_MISSING_DEPENDENCIES) != null) {
                            printStream.println("Blocked on  : "
                                    + ((JSONObject) dependencies)
                                            .get(JsonTags.COORD_ACTION_FIRST_MISSING_DEPENDENCIES));
                        }

                        if (((JSONObject) dependencies).get(JsonTags.COORDINATOR_ACTION_DATASETS) != null) {

                            JSONArray missingDependencies = (JSONArray) ((JSONObject) dependencies)
                                    .get(JsonTags.COORDINATOR_ACTION_DATASETS);

                            for (Object missingDependenciesJson : missingDependencies) {

                                printStream.println("Dataset     : "
                                        + ((JSONObject) missingDependenciesJson)
                                                .get(JsonTags.COORDINATOR_ACTION_DATASET));

                                JSONArray inputDependenciesList = (JSONArray) ((JSONObject) missingDependenciesJson)
                                        .get(JsonTags.COORDINATOR_ACTION_MISSING_DEPS);
                                printStream.println("Pending Dependencies : ");

                                if (inputDependenciesList != null) {
                                    for (String anInputDependenciesList : (Iterable<String>) inputDependenciesList) {
                                        printStream.println("\t  " + anInputDependenciesList);
                                    }
                                }
                                printStream.println();
                            }
                        }
                    }
                }
                else {
                    printStream.println(" No missing input dependencies found");
                }

            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class JobDefinition extends JobMetadata {

        JobDefinition(String jobId) {
            super(jobId, RestConstants.JOB_SHOW_DEFINITION);
        }
    }

    private class JobMetadata extends ClientCallable<String> {
        PrintStream printStream;

        JobMetadata(String jobId, String metaType) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    metaType));
        }

        JobMetadata(String jobId, String metaType, PrintStream ps) {
            this(jobId, metaType);
            printStream = ps;

        }

        JobMetadata(String jobId, String logRetrievalType, String logRetrievalScope, String metaType, String logFilter,
                PrintStream ps) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    metaType, RestConstants.JOB_LOG_TYPE_PARAM, logRetrievalType, RestConstants.JOB_LOG_SCOPE_PARAM,
                    logRetrievalScope, RestConstants.LOG_FILTER_OPTION, logFilter));
            printStream = ps;
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            String returnVal = null;
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                InputStream is = conn.getInputStream();
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    if (printStream != null) {
                        sendToOutputStream(isr, -1, printStream);
                    } else {
                        returnVal = getReaderAsString(isr, -1);
                    }
                }
            }
            else {
                handleError(conn);
            }
            return returnVal;
        }

        /**
         * Output the log to command line interface
         *
         * @param reader reader to read into a string.
         * @param maxLen max content length allowed, if -1 there is no limit.
         * @throws IOException
         */
        private void sendToOutputStream(Reader reader, int maxLen, PrintStream printStream) throws IOException {
            notNull(reader, "reader");
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            int count = 0;
            int noOfCharstoFlush = 1024;
            while ((read = reader.read(buffer)) > -1) {
                count += read;
                if ((maxLen > -1) && (count > maxLen)) {
                    break;
                }
                sb.append(buffer, 0, read);
                if (sb.length() > noOfCharstoFlush) {
                    printStream.print(sb.toString());
                    sb = new StringBuilder("");
                }
            }
            printStream.print(sb.toString());
        }

        /**
         * Return a reader as string.
         * <p>
         *
         * @param reader reader to read into a string.
         * @param maxLen max content length allowed, if -1 there is no limit.
         * @return the reader content.
         * @throws IOException thrown if the resource could not be read.
         */
        private String getReaderAsString(Reader reader, int maxLen) throws IOException {
            notNull(reader, "reader");
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            int count = 0;
            while ((read = reader.read(buffer)) > -1) {
                count += read;

                // read up to maxLen chars;
                if ((maxLen > -1) && (count > maxLen)) {
                    break;
                }
                sb.append(buffer, 0, read);
            }
            reader.close();
            return sb.toString();
        }
    }

    private class CoordJobInfo extends ClientCallable<CoordinatorJob> {

        CoordJobInfo(String jobId, String filter, int start, int len, String order) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_INFO, RestConstants.JOB_FILTER_PARAM, filter, RestConstants.OFFSET_PARAM,
                    Integer.toString(start), RestConstants.LEN_PARAM, Integer.toString(len), RestConstants.ORDER_PARAM,
                    order));
        }

        @Override
        protected CoordinatorJob call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createCoordinatorJob(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class WfsForCoordAction extends ClientCallable<List<WorkflowJob>> {

        WfsForCoordAction(String coordActionId) {
            super("GET", RestConstants.JOB, notEmpty(coordActionId, "coordActionId"), prepareParams(
                    RestConstants.JOB_SHOW_PARAM, RestConstants.ALL_WORKFLOWS_FOR_COORD_ACTION));
        }

        @Override
        protected List<WorkflowJob> call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray workflows = (JSONArray) json.get(JsonTags.WORKFLOWS_JOBS);
                if (workflows == null) {
                    workflows = new JSONArray();
                }
                return JsonToBean.createWorkflowJobList(workflows);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }


    private class BundleJobInfo extends ClientCallable<BundleJob> {

        BundleJobInfo(String jobId) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_INFO));
        }

        @Override
        protected BundleJob call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createBundleJob(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class CoordActionInfo extends ClientCallable<CoordinatorAction> {
        CoordActionInfo(String actionId) {
            super("GET", RestConstants.JOB, notEmpty(actionId, "id"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_INFO));
        }

        @Override
        protected CoordinatorAction call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return JsonToBean.createCoordinatorAction(json);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Get the info of a bundle job.
     *
     * @param jobId job Id.
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public BundleJob getBundleJobInfo(String jobId) throws OozieClientException {
        return new BundleJobInfo(jobId).call();
    }

    /**
     * Get the info of a coordinator job.
     *
     * @param jobId job Id.
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public CoordinatorJob getCoordJobInfo(String jobId) throws OozieClientException {
        return new CoordJobInfo(jobId, null, -1, -1, "asc").call();
    }

    /**
     * Get the info of a coordinator job and subset actions.
     *
     * @param jobId job Id.
     * @param filter filter the status filter
     * @param start starting index in the list of actions belonging to the job
     * @param len number of actions to be returned
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public CoordinatorJob getCoordJobInfo(String jobId, String filter, int start, int len)
            throws OozieClientException {
        return new CoordJobInfo(jobId, filter, start, len, "asc").call();
    }

    /**
     * Get the info of a coordinator job and subset actions.
     *
     * @param jobId job Id.
     * @param filter filter the status filter
     * @param start starting index in the list of actions belonging to the job
     * @param len number of actions to be returned
     * @param order order to list coord actions (e.g, desc)
     * @return the job info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public CoordinatorJob getCoordJobInfo(String jobId, String filter, int start, int len, String order)
            throws OozieClientException {
        return new CoordJobInfo(jobId, filter, start, len, order).call();
    }

    public List<WorkflowJob> getWfsForCoordAction(String coordActionId) throws OozieClientException {
        return new WfsForCoordAction(coordActionId).call();
    }

    /**
     * Get the info of a coordinator action.
     *
     * @param actionId Id.
     * @return the coordinator action info.
     * @throws OozieClientException thrown if the job info could not be retrieved.
     */
    public CoordinatorAction getCoordActionInfo(String actionId) throws OozieClientException {
        return new CoordActionInfo(actionId).call();
    }

    private class JobsStatus extends ClientCallable<List<WorkflowJob>> {

        JobsStatus(String filter, int start, int len) {
            super("GET", RestConstants.JOBS, "", prepareParams(RestConstants.JOBS_FILTER_PARAM, filter,
                    RestConstants.JOBTYPE_PARAM, "wf", RestConstants.OFFSET_PARAM, Integer.toString(start),
                    RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected List<WorkflowJob> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray workflows = (JSONArray) json.get(JsonTags.WORKFLOWS_JOBS);
                if (workflows == null) {
                    workflows = new JSONArray();
                }
                return JsonToBean.createWorkflowJobList(workflows);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class CoordJobsStatus extends ClientCallable<List<CoordinatorJob>> {

        CoordJobsStatus(String filter, int start, int len) {
            super("GET", RestConstants.JOBS, "", prepareParams(RestConstants.JOBS_FILTER_PARAM, filter,
                    RestConstants.JOBTYPE_PARAM, "coord", RestConstants.OFFSET_PARAM, Integer.toString(start),
                    RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected List<CoordinatorJob> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray jobs = (JSONArray) json.get(JsonTags.COORDINATOR_JOBS);
                if (jobs == null) {
                    jobs = new JSONArray();
                }
                return JsonToBean.createCoordinatorJobList(jobs);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class BundleJobsStatus extends ClientCallable<List<BundleJob>> {

        BundleJobsStatus(String filter, int start, int len) {
            super("GET", RestConstants.JOBS, "", prepareParams(RestConstants.JOBS_FILTER_PARAM, filter,
                    RestConstants.JOBTYPE_PARAM, "bundle", RestConstants.OFFSET_PARAM, Integer.toString(start),
                    RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected List<BundleJob> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray jobs = (JSONArray) json.get(JsonTags.BUNDLE_JOBS);
                if (jobs == null) {
                    jobs = new JSONArray();
                }
                return JsonToBean.createBundleJobList(jobs);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class BulkResponseStatus extends ClientCallable<List<BulkResponse>> {

        BulkResponseStatus(String filter, int start, int len) {
            super("GET", RestConstants.JOBS, "", prepareParams(RestConstants.JOBS_BULK_PARAM, filter,
                    RestConstants.OFFSET_PARAM, Integer.toString(start), RestConstants.LEN_PARAM, Integer.toString(len)));
        }

        @Override
        protected List<BulkResponse> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray results = (JSONArray) json.get(JsonTags.BULK_RESPONSES);
                if (results == null) {
                    results = new JSONArray();
                }
                return JsonToBean.createBulkResponseList(results);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class CoordActionsKill extends ClientCallable<List<CoordinatorAction>> {

        CoordActionsKill(String jobId, String rangeType, String scope) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_ACTION_KILL, RestConstants.JOB_COORD_RANGE_TYPE_PARAM, rangeType,
                    RestConstants.JOB_COORD_SCOPE_PARAM, scope));
        }

        @Override
        protected List<CoordinatorAction> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray coordActions = (JSONArray) json.get(JsonTags.COORDINATOR_ACTIONS);
                return JsonToBean.createCoordinatorActionList(coordActions);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class CoordIgnore extends ClientCallable<List<CoordinatorAction>> {
        CoordIgnore(String jobId, String rerunType, String scope) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_ACTION_IGNORE, RestConstants.JOB_COORD_RANGE_TYPE_PARAM,
                    rerunType, RestConstants.JOB_COORD_SCOPE_PARAM, scope));
        }

        @Override
        protected List<CoordinatorAction> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                if(json != null) {
                    JSONArray coordActions = (JSONArray) json.get(JsonTags.COORDINATOR_ACTIONS);
                    return JsonToBean.createCoordinatorActionList(coordActions);
                }
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }
    private class CoordRerun extends ClientCallable<List<CoordinatorAction>> {
        private final Properties conf;

        CoordRerun(String jobId, String rerunType, String scope, boolean refresh, boolean noCleanup, boolean failed,
                   Properties conf) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_COORD_ACTION_RERUN, RestConstants.JOB_COORD_RANGE_TYPE_PARAM, rerunType,
                    RestConstants.JOB_COORD_SCOPE_PARAM, scope, RestConstants.JOB_COORD_RERUN_REFRESH_PARAM,
                    Boolean.toString(refresh), RestConstants.JOB_COORD_RERUN_NOCLEANUP_PARAM, Boolean
                            .toString(noCleanup), RestConstants.JOB_COORD_RERUN_FAILED_PARAM, Boolean.toString(failed)));
            this.conf = conf;
        }

        @Override
        protected List<CoordinatorAction> call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                JSONArray coordActions = (JSONArray) json.get(JsonTags.COORDINATOR_ACTIONS);
                return JsonToBean.createCoordinatorActionList(coordActions);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class BundleRerun extends ClientCallable<Void> {

        BundleRerun(String jobId, String coordScope, String dateScope, boolean refresh, boolean noCleanup) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_BUNDLE_ACTION_RERUN, RestConstants.JOB_BUNDLE_RERUN_COORD_SCOPE_PARAM,
                    coordScope, RestConstants.JOB_BUNDLE_RERUN_DATE_SCOPE_PARAM, dateScope,
                    RestConstants.JOB_COORD_RERUN_REFRESH_PARAM, Boolean.toString(refresh),
                    RestConstants.JOB_COORD_RERUN_NOCLEANUP_PARAM, Boolean.toString(noCleanup)));
        }

        @Override
        protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                return null;
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Rerun coordinator actions.
     *
     * @param jobId coordinator jobId
     * @param rerunType rerun type 'date' if -date is used, 'action-id' if -action is used
     * @param scope rerun scope for date or actionIds
     * @param refresh true if -refresh is given in command option
     * @param noCleanup true if -nocleanup is given in command option
     * @return the list of rerun coordinator actions
     * @throws OozieClientException thrown if the info could not be retrieved.
     */
    public List<CoordinatorAction> reRunCoord(String jobId, String rerunType, String scope, boolean refresh,
            boolean noCleanup) throws OozieClientException {
        return new CoordRerun(jobId, rerunType, scope, refresh, noCleanup, false, null).call();
    }

    /**
     * Rerun coordinator actions with failed option.
     *
     * @param jobId coordinator jobId
     * @param rerunType rerun type 'date' if -date is used, 'action-id' if -action is used
     * @param scope rerun scope for date or actionIds
     * @param refresh true if -refresh is given in command option
     * @param noCleanup true if -nocleanup is given in command option
     * @param failed true if -failed is given in command option
     * @param props properties to use during rerun
     * @return new coordinator job execution
     * @throws OozieClientException thrown if the info could not be retrieved.
     */
    public List<CoordinatorAction> reRunCoord(String jobId, String rerunType, String scope, boolean refresh,
                                              boolean noCleanup, boolean failed, Properties props) throws OozieClientException {
        return new CoordRerun(jobId, rerunType, scope, refresh, noCleanup, failed, props).call();
    }

    /**
     * Rerun bundle coordinators.
     *
     * @param jobId bundle jobId
     * @param coordScope rerun scope for coordinator jobs
     * @param dateScope rerun scope for date
     * @param refresh true if -refresh is given in command option
     * @param noCleanup true if -nocleanup is given in command option
     * @throws OozieClientException thrown if the info could not be retrieved.
     */
    public Void reRunBundle(String jobId, String coordScope, String dateScope, boolean refresh, boolean noCleanup)
            throws OozieClientException {
        return new BundleRerun(jobId, coordScope, dateScope, refresh, noCleanup).call();
    }

    /**
     * Return the info of the workflow jobs that match the filter.
     *
     * @param filter job filter. Refer to the {@link OozieClient} for the filter syntax.
     * @param start jobs offset, base 1.
     * @param len number of jobs to return.
     * @return a list with the workflow jobs info, without node details.
     * @throws OozieClientException thrown if the jobs info could not be retrieved.
     */
    public List<WorkflowJob> getJobsInfo(String filter, int start, int len) throws OozieClientException {
        return new JobsStatus(filter, start, len).call();
    }

    /**
     * Return the info of the workflow jobs that match the filter.
     * <p>
     * It returns the first 100 jobs that match the filter.
     *
     * @param filter job filter. Refer to the {@link OozieClient} for the filter syntax.
     * @return a list with the workflow jobs info, without node details.
     * @throws OozieClientException thrown if the jobs info could not be retrieved.
     */
    public List<WorkflowJob> getJobsInfo(String filter) throws OozieClientException {
        return getJobsInfo(filter, 1, 50);
    }

    /**
     * Sla enable alert.
     *
     * @param jobIds the job ids
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @throws OozieClientException the oozie client exception
     */
    public void slaEnableAlert(String jobIds, String actions, String dates) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_ENABLE_ALERT, jobIds, actions, dates, null).call();
    }

    /**
     * Sla enable alert for bundle with coord name/id.
     *
     * @param bundleId the bundle id
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coordinators
     * @throws OozieClientException the oozie client exception
     */
    public void slaEnableAlert(String bundleId, String actions, String dates, String coords)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_ENABLE_ALERT, bundleId, actions, dates, coords).call();
    }

    /**
     * Sla disable alert.
     *
     * @param jobIds the job ids
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @throws OozieClientException the oozie client exception
     */
    public void slaDisableAlert(String jobIds, String actions, String dates) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_DISABLE_ALERT, jobIds, actions, dates, null).call();
    }

    /**
     * Sla disable alert for bundle with coord name/id.
     *
     * @param bundleId the bundle id
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coordinators
     * @throws OozieClientException the oozie client exception
     */
    public void slaDisableAlert(String bundleId, String actions, String dates, String coords)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_DISABLE_ALERT, bundleId, actions, dates, coords).call();
    }

    /**
     * Sla change definations.
     * SLA change definition parameters can be [&lt;key&gt;=&lt;value&gt;,...&lt;key&gt;=&lt;value&gt;]
     * Supported parameter key names are should-start, should-end and max-duration
     * @param jobIds the job ids
     * @param actions comma separated list of action ids or action id ranges.
     * @param dates comma separated list of the nominal times
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String jobIds, String actions, String dates, String newSlaParams) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, jobIds, actions, dates, null, newSlaParams).call();
    }

    /**
     * Sla change defination for bundle with coord name/id.
     * SLA change definition parameters can be [&lt;key&gt;=&lt;value&gt;,...&lt;key&gt;=&lt;value&gt;]
     * Supported parameter key names are should-start, should-end and max-duration
     * @param bundleId the bundle id
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coords
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String bundleId, String actions, String dates, String coords, String newSlaParams)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, bundleId, actions, dates, coords, newSlaParams).call();
    }

    /**
     * Sla change with new sla param as hasmap.
     * Supported parameter key names are should-start, should-end and max-duration
     * @param bundleId the bundle id
     * @param actions comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coords
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String bundleId, String actions, String dates, String coords, Map<String, String> newSlaParams)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, bundleId, actions, dates, coords, mapToString(newSlaParams)).call();
    }

    /**
     * Convert Map to string.
     *
     * @param map the map
     * @return the string
     */
    protected String mapToString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> e : map.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append(";");
        }
        return sb.toString();
    }

    private class UpdateSLA extends ClientCallable<Void> {

        UpdateSLA(String action, String jobIds, String coordActions, String dates, String coords) {
            super("PUT", RestConstants.JOB, notEmpty(jobIds, "jobIds"), prepareParams(RestConstants.ACTION_PARAM,
                    action, RestConstants.JOB_COORD_SCOPE_ACTION_LIST, coordActions, RestConstants.JOB_COORD_SCOPE_DATE,
                    dates, RestConstants.COORDINATORS_PARAM, coords));
        }

        UpdateSLA(String action, String jobIds, String coordActions, String dates, String coords, String newSlaParams) {
            super("PUT", RestConstants.JOB, notEmpty(jobIds, "jobIds"), prepareParams(RestConstants.ACTION_PARAM,
                    action, RestConstants.JOB_COORD_SCOPE_ACTION_LIST, coordActions, RestConstants.JOB_COORD_SCOPE_DATE,
                    dates, RestConstants.COORDINATORS_PARAM, coords, RestConstants.JOB_CHANGE_VALUE, newSlaParams));
        }

        @Override
        protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                System.out.println("Done");
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
    * Print sla info about coordinator and workflow jobs and actions.
    *
    * @param start starting offset
    * @param len number of results
    * @param filter filters to use. Elements must be semicolon-separated name=value pairs
    * @throws OozieClientException thrown if the sla info could not be retrieved
    */
        public void getSlaInfo(int start, int len, String filter) throws OozieClientException {
            new SlaInfo(start, len, filter).call();
        }

        private class SlaInfo extends ClientCallable<Void> {

            SlaInfo(int start, int len, String filter) {
                super("GET", WS_PROTOCOL_VERSION_1, RestConstants.SLA, "", prepareParams(RestConstants.SLA_GT_SEQUENCE_ID,
                        Integer.toString(start), RestConstants.MAX_EVENTS, Integer.toString(len),
                        RestConstants.JOBS_FILTER_PARAM, filter));
            }

            @Override
            protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
                conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
                if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                else {
                    handleError(conn);
                }
                return null;
            }
        }

    private class JobIdAction extends ClientCallable<String> {

        JobIdAction(String externalId) {
            super("GET", RestConstants.JOBS, "", prepareParams(RestConstants.JOBTYPE_PARAM, "wf",
                    RestConstants.JOBS_EXTERNAL_ID_PARAM, externalId));
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return (String) json.get(JsonTags.JOB_ID);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Return the workflow job Id for an external Id.
     * <p>
     * The external Id must have provided at job creation time.
     *
     * @param externalId external Id given at job creation time.
     * @return the workflow job Id for an external Id, <code>null</code> if none.
     * @throws OozieClientException thrown if the operation could not be done.
     */
    public String getJobId(String externalId) throws OozieClientException {
        return new JobIdAction(externalId).call();
    }

    private class SetSystemMode extends ClientCallable<Void> {

        public SetSystemMode(SYSTEM_MODE status) {
            super("PUT", RestConstants.ADMIN, RestConstants.ADMIN_STATUS_RESOURCE, prepareParams(
                    RestConstants.ADMIN_SYSTEM_MODE_PARAM, status + ""));
        }

        @Override
        public Void call(HttpURLConnection conn) throws IOException, OozieClientException {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Enable or disable safe mode. Used by OozieCLI. In safe mode, Oozie would not accept any commands except status
     * command to change and view the safe mode status.
     *
     * @param status true to enable safe mode, false to disable safe mode.
     * @throws OozieClientException if it fails to set the safe mode status.
     */
    public void setSystemMode(SYSTEM_MODE status) throws OozieClientException {
        new SetSystemMode(status).call();
    }

    private class GetSystemMode extends ClientCallable<SYSTEM_MODE> {

        GetSystemMode() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_STATUS_RESOURCE, prepareParams());
        }

        @Override
        protected SYSTEM_MODE call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return SYSTEM_MODE.valueOf((String) json.get(JsonTags.OOZIE_SYSTEM_MODE));
            }
            else {
                handleError(conn);
            }
            return SYSTEM_MODE.NORMAL;
        }
    }

    /**
     * Returns if Oozie is in safe mode or not.
     *
     * @return true if safe mode is ON<br>
     *         false if safe mode is OFF
     * @throws OozieClientException throw if it could not obtain the safe mode status.
     */
    /*
     * public boolean isInSafeMode() throws OozieClientException { return new GetSafeMode().call(); }
     */
    public SYSTEM_MODE getSystemMode() throws OozieClientException {
        return new GetSystemMode().call();
    }

    public String updateShareLib() throws OozieClientException {
        return new UpdateSharelib().call();
    }

    public String listShareLib(String sharelibKey) throws OozieClientException {
        return new ListShareLib(sharelibKey).call();
    }

    private class GetBuildVersion extends ClientCallable<String> {

        GetBuildVersion() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_BUILD_VERSION_RESOURCE, prepareParams());
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return json.get(JsonTags.BUILD_INFO).toString();
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class ValidateXML extends ClientCallable<String> {

        String file = null;

        ValidateXML(String file, String user) {
            super("POST", WS_PROTOCOL_VERSION, RestConstants.VALIDATE, "",
                    prepareParams(RestConstants.FILE_PARAM, file, RestConstants.USER_PARAM, user));
            this.file = file;
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            if (file.startsWith("/")) {
                FileInputStream fi = new FileInputStream(new File(file));
                byte[] buffer = new byte[1024];
                int n;
                while (-1 != (n = fi.read(buffer))) {
                    conn.getOutputStream().write(buffer, 0, n);
                }
            }
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return (String) json.get(JsonTags.VALIDATE);
            }
            else if ((conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)) {
                return null;
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class SubmitXML extends ClientCallable<String> {
        SubmitXML(final String xml, final String user) {
            super("POST",
                    WS_PROTOCOL_VERSION,
                    RestConstants.ACTION_PARAM,
                    RestConstants.JOB_ACTION_SUBMIT,
                    prepareParams());
        }

        @Override
        protected String call(final HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            return null;
        }
    }

    private  class UpdateSharelib extends ClientCallable<String> {

        UpdateSharelib() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_UPDATE_SHARELIB, prepareParams(
                    RestConstants.ALL_SERVER_REQUEST, "true"));
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            StringBuilder bf = new StringBuilder();
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                Object sharelib = JSONValue.parse(reader);
                bf.append("[ShareLib update status]").append(System.getProperty("line.separator"));
                if (sharelib instanceof JSONArray) {
                    for (Object o : ((JSONArray) sharelib)) {
                        JSONObject obj = (JSONObject) ((JSONObject) o).get(JsonTags.SHARELIB_LIB_UPDATE);
                        for (Object key : obj.keySet()) {
                            bf.append("\t").append(key).append(" = ").append(obj.get(key))
                                    .append(System.getProperty("line.separator"));
                        }
                        bf.append(System.getProperty("line.separator"));
                    }
                }
                else{
                    JSONObject obj = (JSONObject) ((JSONObject) sharelib).get(JsonTags.SHARELIB_LIB_UPDATE);
                    for (Object key : obj.keySet()) {
                        bf.append("\t").append(key).append(" = ").append(obj.get(key))
                                .append(System.getProperty("line.separator"));
                    }
                    bf.append(System.getProperty("line.separator"));
                }
                return bf.toString();
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class ListShareLib extends ClientCallable<String> {

        ListShareLib(String sharelibKey) {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_LIST_SHARELIB, prepareParams(
                    RestConstants.SHARE_LIB_REQUEST_KEY, sharelibKey));
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {

            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                StringBuilder bf = new StringBuilder();
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                Object sharelib = json.get(JsonTags.SHARELIB_LIB);
                bf.append("[Available ShareLib]").append(System.getProperty("line.separator"));
                if (sharelib instanceof JSONArray) {
                    for (Object o : ((JSONArray) sharelib)) {
                        JSONObject obj = (JSONObject) o;
                        bf.append(obj.get(JsonTags.SHARELIB_LIB_NAME))
                                .append(System.getProperty("line.separator"));
                        if (obj.get(JsonTags.SHARELIB_LIB_FILES) != null) {
                            for (Object file : ((JSONArray) obj.get(JsonTags.SHARELIB_LIB_FILES))) {
                                bf.append("\t").append(file).append(System.getProperty("line.separator"));
                            }
                        }
                    }
                    return bf.toString();
                }
            }
            else {
                handleError(conn);
            }
            return null;
        }

    }

    public String purgeCommand(String purgeOptions) throws OozieClientException {
        String workflowAge = "";
        String coordAge = "";
        String bundleAge = "";
        String purgeLimit = "";
        String oldCoordAction = "";
        if (purgeOptions != null) {
            String options[] = purgeOptions.split(";");
            for (String option : options) {
                String pair[] = option.split("=");
                if (pair.length < 2) {
                    throw new OozieClientException(OozieClientException.INVALID_INPUT,
                            "Invalid purge option pair [" + option + "] specified.");
                }
                String key = pair[0].toLowerCase();
                String value = pair[1];
                switch (key) {
                    case "wf":
                        workflowAge = String.valueOf(ValidationUtil.parsePositiveInteger(value));
                        break;
                    case "coord":
                        coordAge = String.valueOf(ValidationUtil.parsePositiveInteger(value));
                        break;
                    case "bundle":
                        bundleAge = String.valueOf(ValidationUtil.parsePositiveInteger(value));
                        break;
                    case "limit":
                        purgeLimit = String.valueOf(ValidationUtil.parsePositiveInteger(value));
                        break;
                    case "oldcoordaction":
                        oldCoordAction = value;
                        break;
                    default:
                        throw new OozieClientException(OozieClientException.INVALID_INPUT,
                                "Invalid purge option [" + key + "] specified.");
                }
            }
        }
        PurgeCommand purgeCommand = new PurgeCommand(workflowAge, coordAge, bundleAge, purgeLimit, oldCoordAction);
        return purgeCommand.call();
    }

    private class PurgeCommand extends ClientCallable<String> {

        PurgeCommand(String workflowAge, String coordAge, String bundleAge, String purgeLimit, String oldCoordAction) {
            super("PUT", RestConstants.ADMIN, RestConstants.ADMIN_PURGE, prepareParams(
                    RestConstants.PURGE_WF_AGE, workflowAge,
                    RestConstants.PURGE_COORD_AGE, coordAge,
                    RestConstants.PURGE_BUNDLE_AGE, bundleAge,
                    RestConstants.PURGE_LIMIT, purgeLimit,
                    RestConstants.PURGE_OLD_COORD_ACTION, oldCoordAction
            ));
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(reader);
                Object msg = jsonObject.get(JsonTags.PURGE);
                return msg.toString();
            } else {
                handleError(conn);
            }
            return null;
        }
    }

    /**
     * Return the Oozie server build version.
     *
     * @return the Oozie server build version.
     * @throws OozieClientException throw if the server build version could not be retrieved.
     */
    public String getServerBuildVersion() throws OozieClientException {
        return new GetBuildVersion().call();
    }

    /**
     * Return the Oozie client build version.
     *
     * @return the Oozie client build version.
     */
    public String getClientBuildVersion() {
        return BuildInfo.getBuildInfo().getProperty(BuildInfo.BUILD_VERSION);
    }

    /**
     * Return the workflow application is valid.
     *
     * @param file local file or hdfs file.
     * @return the workflow application is valid.
     * @throws OozieClientException throw if it the workflow application's validation could not be retrieved.
     */
    public String validateXML(String file) throws OozieClientException {
        String fileName = file;
        if (file.startsWith("file://")) {
            fileName = file.substring(7, file.length());
        }
        if (!fileName.contains("://")) {
            File f = new File(fileName);
            if (!f.isFile()) {
                throw new OozieClientException("File error", "File does not exist : " + f.getAbsolutePath());
            }
            fileName = f.getAbsolutePath();
        }
        return new ValidateXML(fileName, getUserName()).call();
    }

    private String getUserName() {
        String userName = USER_NAME_TL.get();
        if (userName == null) {
            userName = System.getProperty("user.name");
        }
        return userName;
    }

    /**
     * Return the info of the coordinator jobs that match the filter.
     *
     * @param filter job filter. Refer to the {@link OozieClient} for the filter syntax.
     * @param start jobs offset, base 1.
     * @param len number of jobs to return.
     * @return a list with the coordinator jobs info
     * @throws OozieClientException thrown if the jobs info could not be retrieved.
     */
    public List<CoordinatorJob> getCoordJobsInfo(String filter, int start, int len) throws OozieClientException {
        return new CoordJobsStatus(filter, start, len).call();
    }

    /**
     * Return the info of the bundle jobs that match the filter.
     *
     * @param filter job filter. Refer to the {@link OozieClient} for the filter syntax.
     * @param start jobs offset, base 1.
     * @param len number of jobs to return.
     * @return a list with the bundle jobs info
     * @throws OozieClientException thrown if the jobs info could not be retrieved.
     */
    public List<BundleJob> getBundleJobsInfo(String filter, int start, int len) throws OozieClientException {
        return new BundleJobsStatus(filter, start, len).call();
    }

    public List<BulkResponse> getBulkInfo(String filter, int start, int len) throws OozieClientException {
        return new BulkResponseStatus(filter, start, len).call();
    }

    /**
     * Poll a job (Workflow Job ID, Coordinator Job ID, Coordinator Action ID, or Bundle Job ID) and return when it has reached a
     * terminal state.
     * (i.e. FAILED, KILLED, SUCCEEDED)
     *
     * @param id The Job ID
     * @param timeout timeout in minutes (negative values indicate no timeout)
     * @param interval polling interval in minutes (must be positive)
     * @param verbose if true, the current status will be printed out at each poll; if false, no output
     * @throws OozieClientException thrown if the job's status could not be retrieved
     */
    public void pollJob(String id, int timeout, int interval, boolean verbose) throws OozieClientException {
        notEmpty("id", id);
        if (interval < 1) {
            throw new IllegalArgumentException("interval must be a positive integer");
        }
        boolean noTimeout = (timeout < 1);
        long endTime = System.currentTimeMillis() + timeout * 60 * 1000;
        interval *= 60 * 1000;

        final Set<String> completedStatuses;
        if (id.endsWith("-W")) {
            completedStatuses = COMPLETED_WF_STATUSES;
        } else if (id.endsWith("-C")) {
            completedStatuses = COMPLETED_COORD_AND_BUNDLE_STATUSES;
        } else if (id.endsWith("-B")) {
            completedStatuses = COMPLETED_COORD_AND_BUNDLE_STATUSES;
        } else if (id.contains("-C@")) {
            completedStatuses = COMPLETED_COORD_ACTION_STATUSES;
        } else {
            throw new IllegalArgumentException("invalid job type");
        }

        String status = getStatus(id);
        if (verbose) {
            System.out.println(status);
        }
        while(!completedStatuses.contains(status) && (noTimeout || System.currentTimeMillis() <= endTime)) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {
                // ignore
            }
            status = getStatus(id);
            if (verbose) {
                System.out.println(status);
            }
        }
    }

    /**
     * Gets the status for a particular job (Workflow Job ID, Coordinator Job ID, Coordinator Action ID, or Bundle Job ID).
     *
     * @param jobId given jobId
     * @return the status
     * @throws OozieClientException thrown if the status could not be retrieved
     */
    public String getStatus(String jobId) throws OozieClientException {
        return new Status(jobId).call();
    }

    private class Status extends ClientCallable<String> {

        Status(String jobId) {
            super("GET", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.JOB_SHOW_PARAM,
                    RestConstants.JOB_SHOW_STATUS));
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return (String) json.get(JsonTags.STATUS);
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    private class GetQueueDump extends ClientCallable<List<String>> {
        GetQueueDump() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_QUEUE_DUMP_RESOURCE, prepareParams());
        }

        @Override
        protected List<String> call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() != HttpURLConnection.HTTP_OK)) {
                handleError(conn);
                return null;
            }

            Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
            JSONObject json = (JSONObject) JSONValue.parse(reader);
            List<String> queueDumpMessages = new ArrayList<>();

            addSeparator(queueDumpMessages);

            addQueueMessages(json,
                    queueDumpMessages,
                    JsonTags.QUEUE_DUMP,
                    JsonTags.CALLABLE_DUMP,
                    "[Server Queue Dump]:",
                    "The queue dump is empty, nothing to display.");

            addSeparator(queueDumpMessages);

            addQueueMessages(json,
                    queueDumpMessages,
                    JsonTags.UNIQUE_MAP_DUMP,
                    JsonTags.UNIQUE_ENTRY_DUMP,
                    "[Server Uniqueness Map Dump]:",
                    "The uniqueness map dump is empty, nothing to display.");

            addSeparator(queueDumpMessages);

            return queueDumpMessages;
        }

        private void addQueueMessages(JSONObject json,
                                      List<String> queueMessages,
                                      String queueName,
                                      String queueValue,
                                      String headerMessage,
                                      String emptyMessage) {

            JSONArray queueDumpArray = (JSONArray) json.get(queueName);
            queueMessages.add(headerMessage);

            for (Object o : queueDumpArray) {
                JSONObject entry = (JSONObject) o;
                if (entry.get(queueValue) != null) {
                    String value = (String) entry.get(queueValue);
                    queueMessages.add(value);
                }
            }

            if (queueDumpArray.isEmpty()) {
                queueMessages.add(emptyMessage);
            }
        }

        private void addSeparator(List<String> list) {
            list.add("******************************************");
        }
    }

    /**
     * Return the Oozie queue's commands' dump
     *
     * @return the list of strings of callable identification in queue
     * @throws OozieClientException throw if it the queue dump could not be retrieved.
     */
    public List<String> getQueueDump() throws OozieClientException {
        return new GetQueueDump().call();
    }

    private class GetAvailableOozieServers extends MapClientCallable {

        GetAvailableOozieServers() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_AVAILABLE_OOZIE_SERVERS_RESOURCE, prepareParams());
        }
    }

    /**
     * Return the list of available Oozie servers.
     *
     * @return the list of available Oozie servers.
     * @throws OozieClientException throw if it the list of available Oozie servers could not be retrieved.
     */
    public Map<String, String> getAvailableOozieServers() throws OozieClientException {
        return new GetAvailableOozieServers().call();
    }

    private class GetServerConfiguration extends MapClientCallable {

        GetServerConfiguration() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_CONFIG_RESOURCE, prepareParams());
        }
    }

    /**
     * Return the Oozie system configuration.
     *
     * @return the Oozie system configuration.
     * @throws OozieClientException throw if the system configuration could not be retrieved.
     */
    public Map<String, String> getServerConfiguration() throws OozieClientException {
        return new GetServerConfiguration().call();
    }

    private class GetJavaSystemProperties extends MapClientCallable {

        GetJavaSystemProperties() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_JAVA_SYS_PROPS_RESOURCE, prepareParams());
        }
    }

    /**
     * Return the Oozie Java system properties.
     *
     * @return the Oozie Java system properties.
     * @throws OozieClientException throw if the system properties could not be retrieved.
     */
    public Map<String, String> getJavaSystemProperties() throws OozieClientException {
        return new GetJavaSystemProperties().call();
    }

    private class GetOSEnv extends MapClientCallable {

        GetOSEnv() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_OS_ENV_RESOURCE, prepareParams());
        }
    }

    /**
     * Return the Oozie system OS environment.
     *
     * @return the Oozie system OS environment.
     * @throws OozieClientException throw if the system OS environment could not be retrieved.
     */
    public Map<String, String> getOSEnv() throws OozieClientException {
        return new GetOSEnv().call();
    }

    private class GetMetrics extends ClientCallable<Metrics> {

        GetMetrics() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_METRICS_RESOURCE, prepareParams());
        }

        @Override
        protected Metrics call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return new Metrics(json);
            }
            else if ((conn.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE)) {
                // Use Instrumentation endpoint
                return null;
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    public class Metrics {
        private Map<String, Long> counters;
        private Map<String, Object> gauges;
        private Map<String, Timer> timers;
        private Map<String, Histogram> histograms;

        @SuppressWarnings("unchecked")
        public Metrics(JSONObject json) {
            JSONObject jCounters = (JSONObject) json.get("counters");
            counters = new HashMap<>(jCounters.size());
            for (Object entO : jCounters.entrySet()) {
                Entry<String, JSONObject> ent = (Entry<String, JSONObject>) entO;
                counters.put(ent.getKey(), (Long)ent.getValue().get("count"));
            }

            JSONObject jGuages = (JSONObject) json.get("gauges");
            gauges = new HashMap<>(jGuages.size());
            for (Object entO : jGuages.entrySet()) {
                Entry<String, JSONObject> ent = (Entry<String, JSONObject>) entO;
                gauges.put(ent.getKey(), ent.getValue().get("value"));
            }

            JSONObject jTimers = (JSONObject) json.get("timers");
            timers = new HashMap<>(jTimers.size());
            for (Object entO : jTimers.entrySet()) {
                Entry<String, JSONObject> ent = (Entry<String, JSONObject>) entO;
                timers.put(ent.getKey(), new Timer(ent.getValue()));
            }

            JSONObject jHistograms = (JSONObject) json.get("histograms");
            histograms = new HashMap<>(jHistograms.size());
            for (Object entO : jHistograms.entrySet()) {
                Entry<String, JSONObject> ent = (Entry<String, JSONObject>) entO;
                histograms.put(ent.getKey(), new Histogram(ent.getValue()));
            }
        }

        public Map<String, Long> getCounters() {
            return counters;
        }

        public Map<String, Object> getGauges() {
            return gauges;
        }

        public Map<String, Timer> getTimers() {
            return timers;
        }

        public Map<String, Histogram> getHistograms() {
            return histograms;
        }

        public class Timer extends Histogram {
            private double m15Rate;
            private double m5Rate;
            private double m1Rate;
            private double meanRate;
            private String durationUnits;
            private String rateUnits;

            public Timer(JSONObject json) {
                super(json);
                m15Rate = Double.valueOf(json.get("m15_rate").toString());
                m5Rate = Double.valueOf(json.get("m5_rate").toString());
                m1Rate = Double.valueOf(json.get("m1_rate").toString());
                meanRate = Double.valueOf(json.get("mean_rate").toString());
                durationUnits = json.get("duration_units").toString();
                rateUnits = json.get("rate_units").toString();
            }

            public double get15MinuteRate() {
                return m15Rate;
            }

            public double get5MinuteRate() {
                return m5Rate;
            }

            public double get1MinuteRate() {
                return m1Rate;
            }

            public double getMeanRate() {
                return meanRate;
            }

            public String getDurationUnits() {
                return durationUnits;
            }

            public String getRateUnits() {
                return rateUnits;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(super.toString());
                sb.append("\n\t15 minute rate : ").append(m15Rate);
                sb.append("\n\t5 minute rate : ").append(m5Rate);
                sb.append("\n\t1 minute rate : ").append(m15Rate);
                sb.append("\n\tmean rate : ").append(meanRate);
                sb.append("\n\tduration units : ").append(durationUnits);
                sb.append("\n\trate units : ").append(rateUnits);
                return sb.toString();
            }
        }

        public class Histogram {
            private double p999;
            private double p99;
            private double p98;
            private double p95;
            private double p75;
            private double p50;
            private double mean;
            private double min;
            private double max;
            private double stdDev;
            private long count;

            public Histogram(JSONObject json) {
                p999 = Double.valueOf(json.get("p999").toString());
                p99 = Double.valueOf(json.get("p99").toString());
                p98 = Double.valueOf(json.get("p98").toString());
                p95 = Double.valueOf(json.get("p95").toString());
                p75 = Double.valueOf(json.get("p75").toString());
                p50 = Double.valueOf(json.get("p50").toString());
                mean = Double.valueOf(json.get("mean").toString());
                min = Double.valueOf(json.get("min").toString());
                max = Double.valueOf(json.get("max").toString());
                stdDev = Double.valueOf(json.get("stddev").toString());
                count = Long.valueOf(json.get("count").toString());
            }

            public double get999thPercentile() {
                return p999;
            }

            public double get99thPercentile() {
                return p99;
            }

            public double get98thPercentile() {
                return p98;
            }

            public double get95thPercentile() {
                return p95;
            }

            public double get75thPercentile() {
                return p75;
            }

            public double get50thPercentile() {
                return p50;
            }

            public double getMean() {
                return mean;
            }

            public double getMin() {
                return min;
            }

            public double getMax() {
                return max;
            }

            public double getStandardDeviation() {
                return stdDev;
            }

            public long getCount() {
                return count;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("\t999th percentile : ").append(p999);
                sb.append("\n\t99th percentile : ").append(p99);
                sb.append("\n\t98th percentile : ").append(p98);
                sb.append("\n\t95th percentile : ").append(p95);
                sb.append("\n\t75th percentile : ").append(p75);
                sb.append("\n\t50th percentile : ").append(p50);
                sb.append("\n\tmean : ").append(mean);
                sb.append("\n\tmax : ").append(max);
                sb.append("\n\tmin : ").append(min);
                sb.append("\n\tcount : ").append(count);
                sb.append("\n\tstandard deviation : ").append(stdDev);
                return sb.toString();
            }
        }
    }

    /**
     * Return the Oozie metrics.  If null is returned, then try {@link #getInstrumentation()}.
     *
     * @return the Oozie metrics or null.
     * @throws OozieClientException throw if the metrics could not be retrieved.
     */
    public Metrics getMetrics() throws OozieClientException {
        return new GetMetrics().call();
    }

    private class GetInstrumentation extends ClientCallable<Instrumentation> {

        GetInstrumentation() {
            super("GET", RestConstants.ADMIN, RestConstants.ADMIN_INSTRUMENTATION_RESOURCE, prepareParams());
        }

        @Override
        protected Instrumentation call(HttpURLConnection conn) throws IOException, OozieClientException {
            if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JSONObject json = (JSONObject) JSONValue.parse(reader);
                return new Instrumentation(json);
            }
            else if ((conn.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE)) {
                // Use Metrics endpoint
                return null;
            }
            else {
                handleError(conn);
            }
            return null;
        }
    }

    public class Instrumentation {
        private Map<String, Long> counters;
        private Map<String, Object> variables;
        private Map<String, Double> samplers;
        private Map<String, Timer> timers;

        public Instrumentation(JSONObject json) {
            JSONArray jCounters = (JSONArray) json.get("counters");
            counters = new HashMap<>(jCounters.size());
            for (Object groupO : jCounters) {
                JSONObject group = (JSONObject) groupO;
                String groupName = group.get("group").toString() + ".";
                JSONArray data = (JSONArray) group.get("data");
                for (Object datO : data) {
                    JSONObject dat = (JSONObject) datO;
                    counters.put(groupName + dat.get("name").toString(), Long.valueOf(dat.get("value").toString()));
                }
            }

            JSONArray jVariables = (JSONArray) json.get("variables");
            variables = new HashMap<>(jVariables.size());
            for (Object groupO : jVariables) {
                JSONObject group = (JSONObject) groupO;
                String groupName = group.get("group").toString() + ".";
                JSONArray data = (JSONArray) group.get("data");
                for (Object datO : data) {
                    JSONObject dat = (JSONObject) datO;
                    variables.put(groupName + dat.get("name").toString(), dat.get("value"));
                }
            }

            JSONArray jSamplers = (JSONArray) json.get("samplers");
            samplers = new HashMap<>(jSamplers.size());
            for (Object groupO : jSamplers) {
                JSONObject group = (JSONObject) groupO;
                String groupName = group.get("group").toString() + ".";
                JSONArray data = (JSONArray) group.get("data");
                for (Object datO : data) {
                    JSONObject dat = (JSONObject) datO;
                    samplers.put(groupName + dat.get("name").toString(), Double.valueOf(dat.get("value").toString()));
                }
            }

            JSONArray jTimers = (JSONArray) json.get("timers");
            timers = new HashMap<>(jTimers.size());
            for (Object groupO : jTimers) {
                JSONObject group = (JSONObject) groupO;
                String groupName = group.get("group").toString() + ".";
                JSONArray data = (JSONArray) group.get("data");
                for (Object datO : data) {
                    JSONObject dat = (JSONObject) datO;
                    timers.put(groupName + dat.get("name").toString(), new Timer(dat));
                }
            }
        }

        public class Timer {
            private double ownTimeStdDev;
            private long ownTimeAvg;
            private long ownMaxTime;
            private long ownMinTime;
            private double totalTimeStdDev;
            private long totalTimeAvg;
            private long totalMaxTime;
            private long totalMinTime;
            private long ticks;

            public Timer(JSONObject json) {
                ownTimeStdDev = Double.valueOf(json.get("ownTimeStdDev").toString());
                ownTimeAvg = Long.valueOf(json.get("ownTimeAvg").toString());
                ownMaxTime = Long.valueOf(json.get("ownMaxTime").toString());
                ownMinTime = Long.valueOf(json.get("ownMinTime").toString());
                totalTimeStdDev = Double.valueOf(json.get("totalTimeStdDev").toString());
                totalTimeAvg = Long.valueOf(json.get("totalTimeAvg").toString());
                totalMaxTime = Long.valueOf(json.get("totalMaxTime").toString());
                totalMinTime = Long.valueOf(json.get("totalMinTime").toString());
                ticks = Long.valueOf(json.get("ticks").toString());
            }

            public double getOwnTimeStandardDeviation() {
                return ownTimeStdDev;
            }

            public long getOwnTimeAverage() {
                return ownTimeAvg;
            }

            public long getOwnMaxTime() {
                return ownMaxTime;
            }

            public long getOwnMinTime() {
                return ownMinTime;
            }

            public double getTotalTimeStandardDeviation() {
                return totalTimeStdDev;
            }

            public long getTotalTimeAverage() {
                return totalTimeAvg;
            }

            public long getTotalMaxTime() {
                return totalMaxTime;
            }

            public long getTotalMinTime() {
                return totalMinTime;
            }

            public long getTicks() {
                return ticks;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("\town time standard deviation : ").append(ownTimeStdDev);
                sb.append("\n\town average time : ").append(ownTimeAvg);
                sb.append("\n\town max time : ").append(ownMaxTime);
                sb.append("\n\town min time : ").append(ownMinTime);
                sb.append("\n\ttotal time standard deviation : ").append(totalTimeStdDev);
                sb.append("\n\ttotal average time : ").append(totalTimeAvg);
                sb.append("\n\ttotal max time : ").append(totalMaxTime);
                sb.append("\n\ttotal min time : ").append(totalMinTime);
                sb.append("\n\tticks : ").append(ticks);
                return sb.toString();
            }
        }

        public Map<String, Long> getCounters() {
            return counters;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public Map<String, Double> getSamplers() {
            return samplers;
        }

        public Map<String, Timer> getTimers() {
            return timers;
        }
    }

    /**
     * Return the Oozie instrumentation.  If null is returned, then try {@link #getMetrics()}.
     *
     * @return the Oozie intstrumentation or null.
     * @throws OozieClientException throw if the intstrumentation could not be retrieved.
     */
    public Instrumentation getInstrumentation() throws OozieClientException {
        return new GetInstrumentation().call();
    }

    /**
     * Check if the string is not null or not empty.
     *
     * @param str the string to check
     * @param name the name to present in the error message
     * @return string
     */
    public static String notEmpty(String str, String name) {
        if (str == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return str;
    }

    /**
     * Check if the object is not null.
     *
     * @param <T> the type of the object
     * @param obj the object to check
     * @param name the name to present in the error message
     * @return string
     */
    public static <T> T notNull(T obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return obj;
    }
}
