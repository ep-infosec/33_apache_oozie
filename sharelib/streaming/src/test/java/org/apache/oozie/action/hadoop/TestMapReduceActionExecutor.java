/**
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

package org.apache.oozie.action.hadoop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.TypeConverter;
import org.apache.hadoop.streaming.StreamJob;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.oozie.WorkflowActionBean;
import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.client.WorkflowAction.Status;
import org.apache.oozie.command.wf.StartXCommand;
import org.apache.oozie.command.wf.SubmitXCommand;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.service.HadoopAccessorException;
import org.apache.oozie.service.HadoopAccessorService;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.WorkflowAppService;
import org.apache.oozie.util.ClassUtils;
import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.PropertiesUtils;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;
import org.jdom2.Element;

public class TestMapReduceActionExecutor extends ActionExecutorTestCase {
    private static final XLog LOG = XLog.getLog(TestMapReduceActionExecutor.class);

    private static final String PIPES = "pipes";
    private static final String MAP_REDUCE = "map-reduce";

    @Override
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes",
                String.join(",", MapReduceActionExecutor.class.getName(), JavaActionExecutor.class.getName()));
        setSystemProperty("oozie.credentials.credentialclasses", "cred=org.apache.oozie.action.hadoop.CredentialForTest");
    }

    public Element createUberJarActionXML(String uberJarPath, String additional) throws Exception{
        return XmlUtils.parseXml("<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>"
                + "<name-node>" + getNameNodeUri() + "</name-node>" + additional + "<configuration>"
                + "<property><name>oozie.mapreduce.uber.jar</name><value>" + uberJarPath + "</value></property>"
                + "</configuration>" + "</map-reduce>");
    }

    public void testConfigDefaultPropsToAction() throws Exception {
        String actionXml = "<map-reduce>"
                + "        <prepare>"
                + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
                + "        </prepare>"
                + "        <configuration>"
                + "          <property><name>bb</name><value>BB</value></property>"
                + "          <property><name>cc</name><value>from_action</value></property>"
                + "        </configuration>"
                + "      </map-reduce>";
        String wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
        + "<global>"
        + "<job-tracker>${jobTracker}</job-tracker>"
        + "<name-node>${nameNode}</name-node>"
        + "<configuration><property><name>aa</name><value>AA</value></property></configuration>"
        + "</global>"
        + "    <start to=\"mr-node\"/>"
        + "    <action name=\"mr-node\">"
        + actionXml
        + "    <ok to=\"end\"/>"
        + "    <error to=\"fail\"/>"
        + "</action>"
        + "<kill name=\"fail\">"
        + "    <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
        + "</kill>"
        + "<end name=\"end\"/>"
        + "</workflow-app>";

        Writer writer = new OutputStreamWriter(new FileOutputStream(getTestCaseDir() + "/workflow.xml"),
                StandardCharsets.UTF_8);
        IOUtils.copyCharStream(new StringReader(wfXml), writer);

        Configuration conf = new XConfiguration();
        conf.set("nameNode", getNameNodeUri());
        conf.set("jobTracker", getJobTrackerUri());
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set(OozieClient.APP_PATH, new File(getTestCaseDir(), "workflow.xml").toURI().toString());
        conf.set(OozieClient.LOG_TOKEN, "t");

        OutputStream os = new FileOutputStream(getTestCaseDir() + "/config-default.xml");
        XConfiguration defaultConf = new XConfiguration();
        defaultConf.set("outputDir", "default-output-dir");
        defaultConf.set("mapred.mapper.class", "MM");
        defaultConf.set("mapred.reducer.class", "RR");
        defaultConf.set("cc", "from_default");
        defaultConf.writeXml(os);
        os.close();

        String wfId = new SubmitXCommand(conf).call();
        new StartXCommand(wfId).call();
        waitForWorkflowAction(wfId + "@mr-node");

        WorkflowActionBean mrAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                wfId + "@mr-node");
        // check NN and JT settings
        Element eAction = XmlUtils.parseXml(mrAction.getConf());
        Element eConf = eAction.getChild("name-node", eAction.getNamespace());
        assertEquals(getNameNodeUri(), eConf.getText());
        eConf = eAction.getChild("job-tracker", eAction.getNamespace());
        assertEquals(getJobTrackerUri(), eConf.getText());

        // check other m-r settings
        eConf = eAction.getChild("configuration", eAction.getNamespace());
        Configuration actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(eConf).toString()));
        assertEquals("default-output-dir", actionConf.get("outputDir"));
        assertEquals("MM", actionConf.get("mapred.mapper.class"));
        assertEquals("RR", actionConf.get("mapred.reducer.class"));
        // check that default did not overwrite same property explicit in action conf
        assertEquals("from_action", actionConf.get("cc"));
        // check that original conf and from global was not deleted
        assertEquals("AA", actionConf.get("aa"));
        assertEquals("BB", actionConf.get("bb"));

        //test no infinite recursion by param referring to itself e.g. path = ${path}/sub-path
        actionXml = "<map-reduce>"
                + "        <prepare>"
                + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
                + "        </prepare>"
                + "        <configuration>"
                + "          <property><name>cc</name><value>${cc}/action_cc</value></property>"
                + "        </configuration>"
                + "      </map-reduce>";

        wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
                + "<global>"
                + "<job-tracker>${jobTracker}</job-tracker>"
                + "<name-node>${nameNode}</name-node>"
                + "<configuration><property><name>outputDir</name><value>global-output-dir</value></property></configuration>"
                + "</global>"
                + "    <start to=\"mr-node\"/>"
                + "    <action name=\"mr-node\">"
                + actionXml
                + "    <ok to=\"end\"/>"
                + "    <error to=\"fail\"/>"
                + "</action>"
                + "<kill name=\"fail\">"
                + "    <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
                + "</kill>"
                + "<end name=\"end\"/>"
                + "</workflow-app>";

         writer = new OutputStreamWriter(new FileOutputStream(getTestCaseDir() + "/workflow.xml"),
                StandardCharsets.UTF_8);
         IOUtils.copyCharStream(new StringReader(wfXml), writer);

        wfId = new SubmitXCommand(conf).call();
        new StartXCommand(wfId).call();
        waitForWorkflowAction(wfId + "@mr-node");

        mrAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                wfId + "@mr-node");

         // check param
         eAction = XmlUtils.parseXml(mrAction.getConf());
         eConf = eAction.getChild("configuration", eAction.getNamespace());
         actionConf = new XConfiguration(new StringReader(XmlUtils.prettyPrint(eConf).toString()));
         // action param referring to same param name given in defaults cc = ${cc}/action_cc
         assertEquals("from_default/action_cc", actionConf.get("cc"));
         // check global is retained and has precedence over config-default
         eConf = eAction.getChild("name-node", eAction.getNamespace());
         assertEquals(getNameNodeUri(), eConf.getText());
         assertEquals("global-output-dir", actionConf.get("outputDir"));
    }

    public void testGlobalOverrideJobXml() throws Exception {
        FileSystem fs = getFileSystem();

        String jobXml1 = createJobXml("aa", "from_jobXml1", "bb", "from_jobXml1", "jobXml1", fs);
        String jobXml2 = createJobXml("bb", "from_jobXml2", "cc", "from_jobXml2", "jobXml2", fs);
        String jobXml3 = createJobXml("cc", "from_jobXml3", "dd", "from_jobXml3", "jobXml3", fs);
        String jobXml4 = createJobXml("dd", "from_jobXml4", "ee", "from_jobXml4", "jobXml4", fs);

        String actionXml = "<map-reduce>"
                + "        <prepare>"
                + "          <delete path=\"${nameNode}/user/${wf:user()}/mr/${outputDir}\"/>"
                + "        </prepare>"
                + "        <job-xml>" + jobXml3 + "</job-xml>"
                + "        <job-xml>" + jobXml4 + "</job-xml>"
                + "        <configuration>"
                + "        <property><name>ee</name><value>from_action_config</value></property>"
                + "        </configuration>"
                + "      </map-reduce>";
        String wfXml = "<workflow-app xmlns=\"uri:oozie:workflow:0.5\" name=\"map-reduce-wf\">"
                + "<global>"
                + "<job-tracker>${jobTracker}</job-tracker>"
                + "<name-node>${nameNode}</name-node>"
                + "<job-xml>" + jobXml1 + "</job-xml>"
                + "<job-xml>" + jobXml2 + "</job-xml>"
                + "<configuration>"
                + "<property><name>aa</name><value>from_global_config</value></property>"
                + "<property><name>ee</name><value>from_global_config</value></property>"
                + "</configuration>"
                + "</global>"
                + "    <start to=\"mr-node\"/>"
                + "    <action name=\"mr-node\">"
                + actionXml
                + "    <ok to=\"end\"/>"
                + "    <error to=\"fail\"/>"
                + "</action>"
                + "<kill name=\"fail\">"
                + "    <message>Map/Reduce failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>"
                + "</kill>"
                + "<end name=\"end\"/>"
                + "</workflow-app>";

        Writer writer = new FileWriter(getTestCaseDir() + "/workflow.xml");
        IOUtils.copyCharStream(new StringReader(wfXml), writer);

        Configuration conf = new XConfiguration();
        conf.set("nameNode", getNameNodeUri());
        conf.set("jobTracker", getJobTrackerUri());
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set(OozieClient.APP_PATH, "file://" + getTestCaseDir() + File.separator + "workflow.xml");

        OutputStream os = new FileOutputStream(getTestCaseDir() + "/config-default.xml");
        XConfiguration defaultConf = new XConfiguration();
        defaultConf.set("aa", "from_config_default");
        defaultConf.set("ff", "from_config_default");
        defaultConf.writeXml(os);
        os.close();
        defaultConf.set(WorkflowAppService.HADOOP_USER, getTestUser());

        String wfId = new SubmitXCommand(conf).call();
        new StartXCommand(wfId).call();
        waitForWorkflowAction(wfId + "@mr-node");
        WorkflowActionBean mrAction = WorkflowActionQueryExecutor.getInstance().get(WorkflowActionQuery.GET_ACTION,
                wfId + "@mr-node");

        JavaActionExecutor ae = new JavaActionExecutor();
        WorkflowJobBean wf = createBaseWorkflow(defaultConf, "mr-action", wfXml);
        Context context = new Context(wf, mrAction);
        Element mrActionXml = XmlUtils.parseXml(mrAction.getConf());
        Configuration actionConfig = ae.setupActionConf(conf, context, mrActionXml, getAppPath());

        // Check attribute values
        assertEquals("from_global_config", actionConfig.get("aa"));
        assertEquals("from_jobXml2", actionConfig.get("bb"));
        assertEquals("from_jobXml3", actionConfig.get("cc"));
        assertEquals("from_jobXml4", actionConfig.get("dd"));
        assertEquals("from_action_config", actionConfig.get("ee"));
        assertEquals("from_config_default", actionConfig.get("ff"));
    }

    protected String createJobXml(String key1, String value1, String key2, String value2, String filename, FileSystem fs)
            throws Exception {
        String content = "<configuration>"
                + "<property><name>" + key1 + "</name><value>" + value1 + "</value></property>"
                + "<property><name>" + key2 + "</name><value>" + value2 + "</value></property>"
                + "</configuration>";

        Path path = new Path(getAppPath(), filename);
        Writer writer = new OutputStreamWriter(fs.create(path, true));
        writer.write(content);
        writer.close();

        return path.toString();
    }

    public void testSetupMethods() throws Exception {
        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        List<Class<?>> classes = Arrays.<Class<?>>asList(StreamingMain.class);
        assertEquals(classes, ae.getLauncherClasses());

        Element actionXml = XmlUtils.parseXml("<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>"
                + "<name-node>" + getNameNodeUri() + "</name-node>" + "<configuration>"
                + "<property><name>mapred.input.dir</name><value>IN</value></property>"
                + "<property><name>mapred.output.dir</name><value>OUT</value></property>" + "</configuration>"
                + "</map-reduce>");

        XConfiguration protoConf = new XConfiguration();
        protoConf.set(WorkflowAppService.HADOOP_USER, getTestUser());

        WorkflowJobBean wf = createBaseWorkflow(protoConf, "mr-action");
        WorkflowActionBean action = (WorkflowActionBean) wf.getActions().get(0);
        action.setType(ae.getType());

        Context context = new Context(wf, action);

        Configuration conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals("IN", conf.get("mapred.input.dir"));
        Configuration launcherJobConf = ae.createLauncherConf(getFileSystem(), context, action, actionXml, conf);
        assertEquals(false, launcherJobConf.getBoolean("mapreduce.job.complete.cancel.delegation.tokens", true));
        assertEquals(true, conf.getBoolean("mapreduce.job.complete.cancel.delegation.tokens", false));

        // Enable uber jars to test that MapReduceActionExecutor picks up the oozie.mapreduce.uber.jar property correctly
        Services serv = Services.get();
        boolean originalUberJarDisabled = serv.getConf().getBoolean("oozie.action.mapreduce.uber.jar.enable", false);
        serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", true);

        actionXml = createUberJarActionXML(getNameNodeUri() + "/app/job.jar", "");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        // absolute path with namenode
        assertEquals(getNameNodeUri() + "/app/job.jar", conf.get(MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR));

        actionXml = createUberJarActionXML("/app/job.jar", "");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        // absolute path without namenode
        assertEquals(getNameNodeUri() + "/app/job.jar", conf.get(MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR));

        actionXml = createUberJarActionXML("job.jar", "");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals(getFsTestCaseDir() + "/job.jar", conf.get(MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR)); // relative path

        actionXml = createUberJarActionXML("job.jar", "<streaming></streaming>");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        // ignored for streaming
        assertEquals("", conf.get(MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR));

        actionXml = createUberJarActionXML("job.jar", "<pipes></pipes>");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals("", conf.get("oozie.mapreduce.uber.jar"));                                 // ignored for pipes

        actionXml = XmlUtils.parseXml("<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>"
                + "<name-node>" + getNameNodeUri() + "</name-node>" + "</map-reduce>");
        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertNull(conf.get("oozie.mapreduce.uber.jar"));                                       // doesn't resolve if not set

        // Disable uber jars to test that MapReduceActionExecutor won't allow the oozie.mapreduce.uber.jar property
        serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", false);
        try {
            actionXml = createUberJarActionXML(getNameNodeUri() + "/app/job.jar", "");
            conf = ae.createBaseHadoopConf(context, actionXml);
            ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
            fail("ActionExecutorException expected because uber jars are disabled");
        } catch (ActionExecutorException aee) {
            assertEquals("MR003", aee.getErrorCode());
            assertEquals(ActionExecutorException.ErrorType.ERROR, aee.getErrorType());
            assertTrue(aee.getMessage().contains("oozie.action.mapreduce.uber.jar.enable"));
            assertTrue(aee.getMessage().contains("oozie.mapreduce.uber.jar"));
        }
        serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", originalUberJarDisabled);

        actionXml = XmlUtils.parseXml("<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>"
                + "<name-node>" + getNameNodeUri() + "</name-node>" + "<streaming>" + "<mapper>M</mapper>"
                + "<reducer>R</reducer>" + "<record-reader>RR</record-reader>"
                + "<record-reader-mapping>RRM1=1</record-reader-mapping>"
                + "<record-reader-mapping>RRM2=2</record-reader-mapping>" + "<env>e=E</env>" + "<env>ee=EE</env>"
                + "</streaming>" + "<configuration>"
                + "<property><name>mapred.input.dir</name><value>IN</value></property>"
                + "<property><name>mapred.output.dir</name><value>OUT</value></property>" + "</configuration>"
                + "</map-reduce>");

        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals("M", conf.get("oozie.streaming.mapper"));
        assertEquals("R", conf.get("oozie.streaming.reducer"));
        assertEquals("RR", conf.get("oozie.streaming.record-reader"));
        assertEquals("2", conf.get("oozie.streaming.record-reader-mapping.size"));
        assertEquals("2", conf.get("oozie.streaming.env.size"));

        actionXml = XmlUtils.parseXml("<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>"
                + "<name-node>" + getNameNodeUri() + "</name-node>" + "<pipes>" + "<map>M</map>" + "<reduce>R</reduce>"
                + "<inputformat>IF</inputformat>" + "<partitioner>P</partitioner>" + "<writer>W</writer>"
                + "<program>PP</program>" + "</pipes>" + "<configuration>"
                + "<property><name>mapred.input.dir</name><value>IN</value></property>"
                + "<property><name>mapred.output.dir</name><value>OUT</value></property>" + "</configuration>"
                + "</map-reduce>");

        conf = ae.createBaseHadoopConf(context, actionXml);
        ae.setupActionConf(conf, context, actionXml, getFsTestCaseDir());
        assertEquals("M", conf.get("oozie.pipes.map"));
        assertEquals("R", conf.get("oozie.pipes.reduce"));
        assertEquals("IF", conf.get("oozie.pipes.inputformat"));
        assertEquals("P", conf.get("oozie.pipes.partitioner"));
        assertEquals("W", conf.get("oozie.pipes.writer"));
        assertEquals(getFsTestCaseDir()+"/PP", conf.get("oozie.pipes.program"));
    }

    protected Context createContext(String name, String actionXml) throws Exception {
        JavaActionExecutor ae = new JavaActionExecutor();

        Path appJarPath = new Path("lib/test.jar");
        File jarFile = IOUtils.createJar(new File(getTestCaseDir()), "test.jar", MapperReducerForTest.class);
        InputStream is = new FileInputStream(jarFile);
        OutputStream os = getFileSystem().create(new Path(getAppPath(), "lib/test.jar"));
        IOUtils.copyStream(is, os);

        XConfiguration protoConf = new XConfiguration();
        protoConf.set(WorkflowAppService.HADOOP_USER, getTestUser());

        protoConf.setStrings(WorkflowAppService.APP_LIB_PATH_LIST, appJarPath.toString());

        WorkflowJobBean wf = createBaseWorkflow(protoConf, "mr-action");
        WorkflowActionBean action = (WorkflowActionBean) wf.getActions().get(0);
        action.setName(name);
        action.setType(ae.getType());
        action.setConf(actionXml);

        return new Context(wf, action);
    }

    protected Context createContextWithCredentials(String name, String actionXml) throws Exception {
        JavaActionExecutor ae = new JavaActionExecutor();

        Path appJarPath = new Path("lib/test.jar");
        File jarFile = IOUtils.createJar(new File(getTestCaseDir()), "test.jar", MapperReducerForTest.class);
        InputStream is = new FileInputStream(jarFile);
        OutputStream os = getFileSystem().create(new Path(getAppPath(), "lib/test.jar"));
        IOUtils.copyStream(is, os);

        XConfiguration protoConf = new XConfiguration();
        protoConf.set(WorkflowAppService.HADOOP_USER, getTestUser());
        protoConf.setStrings(WorkflowAppService.APP_LIB_PATH_LIST, appJarPath.toString());


        WorkflowJobBean wf = createBaseWorkflowWithCredentials(protoConf, "mr-action");
        WorkflowActionBean action = (WorkflowActionBean) wf.getActions().get(0);
        action.setName(name);
        action.setType(ae.getType());
        action.setConf(actionXml);
        action.setCred("testcred");

        return new Context(wf, action);
    }

    protected String submitAction(Context context) throws Exception {
        MapReduceActionExecutor ae = new MapReduceActionExecutor();

        WorkflowAction action = context.getAction();

        ae.prepareActionDir(getFileSystem(), context);
        ae.submitLauncher(getFileSystem(), context, action);

        return context.getAction().getExternalId();
    }

    private String _testSubmit(String name, String actionXml) throws Exception {

        Context context = createContext(name, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                context.getProtoActionConf());
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        String externalChildIDs = context.getAction().getExternalChildIDs();
        waitUntilYarnAppDoneAndAssertSuccess(externalChildIDs);
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        //hadoop.counters will always be set in case of MR action.
        assertNotNull(context.getVar("hadoop.counters"));
        String counters = context.getVar("hadoop.counters");
        assertTrue(counters.contains("Counter"));
        assertTrue(counters.contains("\"MAP_OUTPUT_RECORDS\":2"));

        //External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());

        return externalChildIDs;
    }

    private void _testSubmitError(String actionXml, String errorMessage) throws Exception {
        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.FAILED_KILLED, context.getAction().getExternalStatus());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.ERROR, context.getAction().getStatus());
        assertTrue(context.getAction().getErrorMessage().contains("already exists"));
    }

    private void _testSubmitWithCredentials(String name, String actionXml) throws Exception {

        Context context = createContextWithCredentials(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                context.getProtoActionConf());
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        String externalChildIDs = context.getAction().getExternalChildIDs();
        waitUntilYarnAppDoneAndAssertSuccess(externalChildIDs);
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());
        Configuration conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));
        String user = conf.get("user.name");
        JobClient jobClient = getHadoopAccessorService().createJobClient(user, conf);
        org.apache.hadoop.mapreduce.JobID jobID = TypeConverter.fromYarn(
                ConverterUtils.toApplicationId(externalChildIDs));
        final RunningJob mrJob = jobClient.getJob(JobID.downgrade(jobID));
        assertTrue(MapperReducerCredentialsForTest.hasCredentials(mrJob));
    }

    protected XConfiguration getSleepMapReduceConfig(String inputDir, String outputDir) {
        XConfiguration conf = getMapReduceConfig(inputDir, outputDir);
        conf.set("mapred.mapper.class", BlockingMapper.class.getName());
        return conf;
    }

    protected XConfiguration getMapReduceConfig(String inputDir, String outputDir) {
        XConfiguration conf = new XConfiguration();
        conf.set("mapred.mapper.class", MapperReducerForTest.class.getName());
        conf.set("mapred.reducer.class", MapperReducerForTest.class.getName());
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        return conf;
    }

    protected XConfiguration getMapReduceCredentialsConfig(String inputDir, String outputDir) {
        XConfiguration conf = new XConfiguration();
        conf.set("mapred.mapper.class", MapperReducerCredentialsForTest.class.getName());
        conf.set("mapred.reducer.class", MapperReducerForTest.class.getName());
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        return conf;
    }

    protected XConfiguration getMapReduceUberJarConfig(String inputDir, String outputDir) throws Exception{
        XConfiguration conf = new XConfiguration();
        conf.set("mapred.mapper.class", MapperReducerUberJarForTest.class.getName());
        conf.set("mapred.reducer.class", MapperReducerUberJarForTest.class.getName());
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        conf.set("oozie.mapreduce.uber.jar", createAndUploadUberJar().toUri().toString());
        return conf;
    }

    public void testMapReduce() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "</map-reduce>";
        _testSubmit(MAP_REDUCE, actionXml);
    }

    public void testMapReduceActionError() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output1");

        Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")), StandardCharsets.UTF_8);
        w.write("dummy\n");
        w.write("dummy\n");
        writeDummyInput(fs, outputDir);

        String actionXml = "<map-reduce>" +
                "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "<name-node>" + getNameNodeUri() + "</name-node>" +
                "<configuration>" +
                "<property><name>mapred.mapper.class</name><value>" + MapperReducerForTest.class.getName() +
                "</value></property>" +
                "<property><name>mapred.reducer.class</name><value>" + MapperReducerForTest.class.getName() +
                "</value></property>" +
                "<property><name>mapred.input.dir</name><value>" + inputDir + "</value></property>" +
                "<property><name>mapred.output.dir</name><value>" + outputDir + "</value></property>" +
                "</configuration>" +
                "</map-reduce>";

        _testSubmitError(actionXml, "already exists");
    }

    public void testMapReduceWithConfigClass() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        Path jobXml = new Path(getFsTestCaseDir(), "action.xml");
        XConfiguration conf = getMapReduceConfig(inputDir.toString(), outputDir.toString());
        conf.set(MapperReducerForTest.JOB_XML_OUTPUT_LOCATION, jobXml.toUri().toString());
        conf.set("B", "b");
        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + conf.toXmlString(false)
                + "<config-class>" + OozieActionConfiguratorForTest.class.getName() + "</config-class>" + "</map-reduce>";

        _testSubmit(MAP_REDUCE, actionXml);
        Configuration conf2 = new Configuration(false);
        conf2.addResource(fs.open(jobXml));

        assertEquals("a", conf2.get("A"));
        assertEquals("c", conf2.get("B"));
    }

    public void testMapReduceWithConfigClassNotFound() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceConfig(inputDir.toString(), outputDir.toString()).toXmlString(false)
                + "<config-class>org.apache.oozie.does.not.exist</config-class>" + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        final Map<String, String> actionData = LauncherHelper.getActionData(fs, context.getActionDir(),
                context.getProtoActionConf());
        Properties errorProps = PropertiesUtils.stringToProperties(actionData.get(LauncherAMUtils.ACTION_DATA_ERROR_PROPS));
        assertEquals("An Exception occurred while instantiating the action config class",
                errorProps.getProperty("exception.message"));
        assertTrue(errorProps.getProperty("exception.stacktrace").startsWith(OozieActionConfiguratorException.class.getName()));
    }

    public void testMapReduceWithConfigClassThrowException() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        XConfiguration conf = getMapReduceConfig(inputDir.toString(), outputDir.toString());
        conf.setBoolean("oozie.test.throw.exception", true);        // causes OozieActionConfiguratorForTest to throw an exception
        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + conf.toXmlString(false)
                + "<config-class>" + OozieActionConfiguratorForTest.class.getName() + "</config-class>" + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        final Map<String, String> actionData = LauncherHelper.getActionData(fs, context.getActionDir(),
                context.getProtoActionConf());
        Properties errorProps = PropertiesUtils.stringToProperties(actionData.get(LauncherAMUtils.ACTION_DATA_ERROR_PROPS));
        assertEquals("doh", errorProps.getProperty("exception.message"));
        assertTrue(errorProps.getProperty("exception.stacktrace").startsWith(OozieActionConfiguratorException.class.getName()));
    }

    public void testMapReduceActionKill() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getSleepMapReduceConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        // wait until LauncherAM terminates - the MR job keeps running the background
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        MapReduceActionExecutor mae = new MapReduceActionExecutor();
        mae.check(context, context.getAction());  // must be called so that externalChildIDs are read from HDFS

        mae.kill(context, context.getAction());

        waitUntilYarnAppKilledAndAssertSuccess(context.getAction().getExternalChildIDs());
    }

    public void testMapReduceWithCredentials() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceCredentialsConfig(inputDir.toString(), outputDir.toString()).toXmlString(false)
                + "</map-reduce>";
        _testSubmitWithCredentials(MAP_REDUCE, actionXml);
    }

    protected Path createAndUploadUberJar() throws Exception {
        Path localJobJarPath = makeUberJarWithLib(getTestCaseDir());
        Path remoteJobJarPath = new Path(getAppPath(), localJobJarPath.getName());
        getFileSystem().moveFromLocalFile(localJobJarPath, remoteJobJarPath);
        File localJobJarFile = new File(localJobJarPath.toUri().toString());
        if (localJobJarFile.exists()) {     // just to make sure
            localJobJarFile.delete();
        }
        return remoteJobJarPath;
    }

    private Path makeUberJarWithLib(String testDir) throws Exception {
        Path jobJarPath = new Path(testDir, "uber.jar");
        FileOutputStream fos = new FileOutputStream(new File(jobJarPath.toUri().getPath()));
        JarOutputStream jos = new JarOutputStream(fos);
        // Have to put in real jar files or it will complain
        createAndAddJarToJar(jos, new File(new Path(testDir, "lib1.jar").toUri().getPath()));
        createAndAddJarToJar(jos, new File(new Path(testDir, "lib2.jar").toUri().getPath()));
        jos.close();
        return jobJarPath;
    }

    private void createAndAddJarToJar(JarOutputStream jos, File jarFile) throws Exception {
        FileOutputStream fos2 = new FileOutputStream(jarFile);
        JarOutputStream jos2 = new JarOutputStream(fos2);
        // Have to have at least one entry or it will complain
        ZipEntry ze = new ZipEntry(jarFile.getName() + ".inside");
        jos2.putNextEntry(ze);
        jos2.closeEntry();
        jos2.close();
        ze = new ZipEntry("lib/" + jarFile.getName());
        jos.putNextEntry(ze);
        FileInputStream in = new FileInputStream(jarFile);
        byte buf[] = new byte[1024];
        int numRead;
        do {
            numRead = in.read(buf);
            if (numRead >= 0) {
                jos.write(buf, 0, numRead);
            }
        } while (numRead != -1);
        in.close();
        jos.closeEntry();
        jarFile.delete();
    }

    public void _testMapReduceWithUberJar() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceUberJarConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "</map-reduce>";
        String appID = _testSubmit(MAP_REDUCE, actionXml);

        boolean containsLib1Jar = false;
        String lib1JarStr = "jobcache/" + appID + "/jars/lib/lib1.jar";
        Pattern lib1JarPatYarn = Pattern.compile(
                ".*appcache/" + appID + "/filecache/.*/uber.jar/lib/lib1.jar.*");
        boolean containsLib2Jar = false;
        String lib2JarStr = "jobcache/" + appID + "/jars/lib/lib1.jar";
        Pattern lib2JarPatYarn = Pattern.compile(
                ".*appcache/" + appID + "/filecache/.*/uber.jar/lib/lib2.jar.*");

        FileStatus[] fstats = getFileSystem().listStatus(outputDir);
        for (FileStatus fstat : fstats) {
            Path p = fstat.getPath();
            if (getFileSystem().isFile(p) && p.getName().startsWith("part-")) {
                InputStream is = getFileSystem().open(p);
                Scanner sc = new Scanner(new InputStreamReader(is,StandardCharsets.UTF_8));
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    containsLib1Jar = (containsLib1Jar || line.contains(lib1JarStr) || lib1JarPatYarn.matcher(line).matches());
                    containsLib2Jar = (containsLib2Jar || line.contains(lib2JarStr) || lib2JarPatYarn.matcher(line).matches());
                }
                sc.close();
                is.close();
            }
        }

        assertTrue("lib/lib1.jar should have been unzipped from the uber jar and added to the classpath but was not",
                containsLib1Jar);
        assertTrue("lib/lib2.jar should have been unzipped from the uber jar and added to the classpath but was not",
                containsLib2Jar);
    }

    // With the oozie.action.mapreduce.uber.jar.enable property set to false, a workflow with an uber jar should fail
    public void testMapReduceWithUberJarDisabled() throws Exception {
        Services serv = Services.get();
        boolean originalUberJarDisabled = serv.getConf().getBoolean("oozie.action.mapreduce.uber.jar.enable", false);
        try {
            serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", false);
            _testMapReduceWithUberJar();
        } catch (ActionExecutorException aee) {
            assertEquals("MR003", aee.getErrorCode());
            assertEquals(ActionExecutorException.ErrorType.ERROR, aee.getErrorType());
            assertTrue(aee.getMessage().contains("oozie.action.mapreduce.uber.jar.enable"));
            assertTrue(aee.getMessage().contains("oozie.mapreduce.uber.jar"));
        } catch (Exception e) {
            throw e;
        } finally {
            serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", originalUberJarDisabled);
        }
    }

    public void testJobNameSetForMapReduceChild() throws Exception {
        Services serv = Services.get();
        serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", true);

        final FileSystem fs = getFileSystem();
        final Path inputDir = new Path(getFsTestCaseDir(), "input");
        final Path outputDir = new Path(getFsTestCaseDir(), "output");
        writeDummyInput(fs, inputDir);

        final String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>"
                + getMapReduceUberJarConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "</map-reduce>";

        final String extId = _testSubmit(MAP_REDUCE, actionXml);
        final ApplicationId appId = ConverterUtils.toApplicationId(extId);
        final Configuration conf = getHadoopAccessorService().createConfiguration(getJobTrackerUri());
        final String name = getHadoopAccessorService().createYarnClient(getTestUser(), conf).getApplicationReport(appId).getName();
        assertTrue(name.contains("oozie:action"));
    }

    private void writeDummyInput(FileSystem fs, Path inputDir) throws IOException {
        Writer w = new OutputStreamWriter(fs.create(new Path(inputDir, "data.txt")), StandardCharsets.UTF_8);
        w.write("dummy\n");
        w.write("dummy\n");
        w.close();
    }

    public void testMapReduceWithUberJarEnabled() throws Exception {
        Services serv = Services.get();
        boolean originalUberJarDisabled = serv.getConf().getBoolean("oozie.action.mapreduce.uber.jar.enable", false);
        try {
            serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", true);
            _testMapReduceWithUberJar();
        } catch (Exception e) {
            throw e;
        } finally {
            serv.getConf().setBoolean("oozie.action.mapreduce.uber.jar.enable", originalUberJarDisabled);
        }
    }

    protected XConfiguration getStreamingConfig(String inputDir, String outputDir) {
        XConfiguration conf = new XConfiguration();
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        return conf;
    }

    private void runStreamingWordCountJob(Path inputDir, Path outputDir, XConfiguration streamingConf) throws Exception {
        FileSystem fs = getFileSystem();
        Path streamingJar = new Path(getFsTestCaseDir(), "jar/hadoop-streaming.jar");

        InputStream is = new FileInputStream(ClassUtils.findContainingJar(StreamJob.class));
        OutputStream os = fs.create(new Path(getAppPath(), streamingJar));
        IOUtils.copyStream(is, os);

        writeDummyInput(fs, inputDir);

        String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                + getNameNodeUri() + "</name-node>" + "      <streaming>" + "        <mapper>cat</mapper>"
                + "        <reducer>wc</reducer>" + "      </streaming>"
                + streamingConf.toXmlString(false) + "<file>"
                + streamingJar + "</file>" + "</map-reduce>";
        _testSubmit("streaming", actionXml);
    }

    public void testStreaming() throws Exception {
        FileSystem fs = getFileSystem();
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");
        final XConfiguration streamingConf = getStreamingConfig(inputDir.toString(), outputDir.toString());

        runStreamingWordCountJob(inputDir, outputDir, streamingConf);

        final FSDataInputStream dis = fs.open(getOutputFile(outputDir, fs));
        final List<String> lines = org.apache.commons.io.IOUtils.readLines(dis);
        dis.close();
        assertEquals(1, lines.size());
        // Not sure why it is 14 instead of 12. \n twice ??
        assertEquals("2       2      14", lines.get(0).trim());
    }

    public void testStreamingConfOverride() throws Exception {
        FileSystem fs = getFileSystem();
        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");
        final XConfiguration streamingConf = getStreamingConfig(inputDir.toString(), outputDir.toString());
        streamingConf.set("mapred.output.format.class", "org.apache.hadoop.mapred.SequenceFileOutputFormat");

        runStreamingWordCountJob(inputDir, outputDir, streamingConf);

        SequenceFile.Reader seqFile = new SequenceFile.Reader(fs, getOutputFile(outputDir, fs), getFileSystem().getConf());
        Text key = new Text(), value = new Text();
        if (seqFile.next(key, value)) {
            assertEquals("2       2      14", key.toString().trim());
            assertEquals("", value.toString());
        }
        assertFalse(seqFile.next(key, value));
        seqFile.close();
    }

    private Path getOutputFile(Path outputDir, FileSystem fs) throws FileNotFoundException, IOException {
        final FileStatus[] files = fs.listStatus(outputDir, new PathFilter() {

            @Override
            public boolean accept(Path path) {
                return path.getName().startsWith("part");
            }
        });
        return files[0].getPath(); //part-[m/r]-00000
    }

    protected XConfiguration getPipesConfig(String inputDir, String outputDir) {
        XConfiguration conf = new XConfiguration();
        conf.setBoolean("hadoop.pipes.java.recordreader", true);
        conf.setBoolean("hadoop.pipes.java.recordwriter", true);
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        return conf;
    }

    private XConfiguration getOozieActionExternalStatsWriteProperty(String inputDir, String outputDir,
            String oozieProperty) {
        XConfiguration conf = new XConfiguration();
        conf.set("mapred.input.dir", inputDir);
        conf.set("mapred.output.dir", outputDir);
        conf.set("oozie.action.external.stats.write", oozieProperty);
        return conf;
    }

    public void testPipes() throws Exception {
        Path programPath = new Path(getFsTestCaseDir(), "wordcount-simple");

        FileSystem fs = getFileSystem();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("wordcount-simple");
        if (is != null) {
            OutputStream os = fs.create(programPath);
            IOUtils.copyStream(is, os);

            Path inputDir = new Path(getFsTestCaseDir(), "input");
            Path outputDir = new Path(getFsTestCaseDir(), "output");

            writeDummyInput(fs, inputDir);

            String actionXml = "<map-reduce>" + "<job-tracker>" + getJobTrackerUri() + "</job-tracker>" + "<name-node>"
                    + getNameNodeUri() + "</name-node>" + "      <pipes>" + "        <program>" + programPath
                    + "#wordcount-simple" + "</program>" + "      </pipes>"
                    + getPipesConfig(inputDir.toString(), outputDir.toString()).toXmlString(false) + "<file>"
                    + programPath + "</file>" + "</map-reduce>";
            _testSubmit(PIPES, actionXml);
        }
        else {
            System.out.println(
                "SKIPPING TEST: TestMapReduceActionExecutor.testPipes(), " +
                "binary 'wordcount-simple' not available in the classpath");
        }
    }

    // Test to assert that executionStats is set when user has specified stats
    // write property as true.
    public void testSetExecutionStats_when_user_has_specified_stats_write_TRUE() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        // set user stats write property as true explicitly in the
        // configuration.
        String actionXml = "<map-reduce>"
                + "<job-tracker>"
                + getJobTrackerUri()
                + "</job-tracker>"
                + "<name-node>"
                + getNameNodeUri()
                + "</name-node>"
                + getOozieActionExternalStatsWriteProperty(inputDir.toString(), outputDir.toString(), "true")
                        .toXmlString(false) + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        Configuration conf = ae.createBaseHadoopConf(context, XmlUtils.parseXml(actionXml));

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                conf);
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        waitUntilYarnAppDoneAndAssertSuccess(context.getAction().getExternalChildIDs());
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        // Assert for stats info stored in the context.
        assertNotNull(context.getExecutionStats());
        assertTrue(context.getExecutionStats().contains("ACTION_TYPE"));
        assertTrue(context.getExecutionStats().contains("Counter"));

        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());

        // hadoop.counters will always be set in case of MR action.
        assertNotNull(context.getVar("hadoop.counters"));
        String counters = context.getVar("hadoop.counters");
        assertTrue(counters.contains("Counter"));
    }

    // Test to assert that executionStats is not set when user has specified
    // stats write property as false.
    public void testSetExecutionStats_when_user_has_specified_stats_write_FALSE() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        // set user stats write property as false explicitly in the
        // configuration.
        String actionXml = "<map-reduce>"
                + "<job-tracker>"
                + getJobTrackerUri()
                + "</job-tracker>"
                + "<name-node>"
                + getNameNodeUri()
                + "</name-node>"
                + getOozieActionExternalStatsWriteProperty(inputDir.toString(), outputDir.toString(), "false")
                        .toXmlString(false) + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                context.getProtoActionConf());
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        waitUntilYarnAppDoneAndAssertSuccess(context.getAction().getExternalChildIDs());
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        // Assert for stats info stored in the context.
        assertNull(context.getExecutionStats());

        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());

        // hadoop.counters will always be set in case of MR action.
        assertNotNull(context.getVar("hadoop.counters"));
        String counters = context.getVar("hadoop.counters");
        assertTrue(counters.contains("Counter"));
    }

    public void testEndWithoutConfiguration() throws Exception {
        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        // set user stats write property as false explicitly in the
        // configuration.
        String actionXml = "<map-reduce>"
                + "<job-tracker>"
                + getJobTrackerUri()
                + "</job-tracker>"
                + "<name-node>"
                + getNameNodeUri()
                + "</name-node>"
                + getOozieActionExternalStatsWriteProperty(inputDir.toString(), outputDir.toString(), "false")
                .toXmlString(false) + "</map-reduce>";

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                context.getProtoActionConf());
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        waitUntilYarnAppDoneAndAssertSuccess(context.getAction().getExternalChildIDs());
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        actionXml = "<map-reduce>"
                + "<job-tracker>"
                + getJobTrackerUri()
                + "</job-tracker>"
                + "<name-node>"
                + getNameNodeUri()
                + "</name-node></map-reduce>";

        WorkflowActionBean action = (WorkflowActionBean) context.getAction();
        action.setConf(actionXml);

        try {
            ae.end(context, context.getAction());
        }
        catch(Exception e)
        {
            fail("unexpected exception throwing " + e);
        }
    }

    /**
     * Test "oozie.launcher.mapred.job.name" and "mapred.job.name" can be set in
     * the action configuration and not overridden by the action executor
     *
     * @throws Exception
     */
    public void testSetMapredJobName() throws Exception {
        final String launcherJobName = "MapReduceLauncherTest";
        final String mapredJobName = "MapReduceTest";

        FileSystem fs = getFileSystem();

        Path inputDir = new Path(getFsTestCaseDir(), "input");
        Path outputDir = new Path(getFsTestCaseDir(), "output");

        writeDummyInput(fs, inputDir);

        XConfiguration mrConfig = getMapReduceConfig(inputDir.toString(),
                outputDir.toString());
        mrConfig.set("oozie.launcher.mapred.job.name", launcherJobName);
        mrConfig.set("mapred.job.name", mapredJobName);

        StringBuilder sb = new StringBuilder("<map-reduce>")
                .append("<job-tracker>").append(getJobTrackerUri())
                .append("</job-tracker>").append("<name-node>")
                .append(getNameNodeUri()).append("</name-node>")
                .append(mrConfig.toXmlString(false)).append("</map-reduce>");
        String actionXml = sb.toString();

        Context context = createContext(MAP_REDUCE, actionXml);
        final String launcherId = submitAction(context);
        waitUntilYarnAppDoneAndAssertSuccess(launcherId);

        Map<String, String> actionData = LauncherHelper.getActionData(getFileSystem(), context.getActionDir(),
                context.getProtoActionConf());
        assertTrue(LauncherHelper.hasIdSwap(actionData));

        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        ae.check(context, context.getAction());
        assertTrue(launcherId.equals(context.getAction().getExternalId()));

        String externalChildIDs = context.getAction().getExternalChildIDs();
        waitUntilYarnAppDoneAndAssertSuccess(externalChildIDs);
        ae.check(context, context.getAction());

        assertEquals(JavaActionExecutor.SUCCEEDED, context.getAction().getExternalStatus());
        assertNull(context.getAction().getData());

        ae.end(context, context.getAction());
        assertEquals(WorkflowAction.Status.OK, context.getAction().getStatus());

        Configuration conf = getHadoopAccessorService().createConfiguration(getJobTrackerUri());
        final YarnClient yarnClient = getHadoopAccessorService().createYarnClient(getTestUser(), conf);
        ApplicationReport report = yarnClient.getApplicationReport(ConverterUtils.toApplicationId(externalChildIDs));
        // Assert Mapred job name has been set
        assertEquals(mapredJobName, report.getName());

        // Assert for stats info stored in the context.
        assertNull(context.getExecutionStats());

        // External Child IDs used to be null, but after 4.0, become Non-Null in case of MR action.
        assertNotNull(context.getExternalChildIDs());

        // hadoop.counters will always be set in case of MR action.
        assertNotNull(context.getVar("hadoop.counters"));
        String counters = context.getVar("hadoop.counters");
        assertTrue(counters.contains("Counter"));
    }

    public void testDefaultShareLibName() {
        MapReduceActionExecutor ae = new MapReduceActionExecutor();
        Element e = new Element("mapreduce");
        assertNull(ae.getDefaultShareLibName(e));
        e.addContent(new Element("streaming"));
        assertEquals("mapreduce-streaming", ae.getDefaultShareLibName(e));
    }

    /**
     * https://issues.apache.org/jira/browse/OOZIE-87
     * This test covers map-reduce action
     * @throws Exception
     */
    public void testCommaSeparatedFilesAndArchives() throws Exception {
        Path root = new Path(getFsTestCaseDir(), "root");

        Path jar = new Path("jar.jar");
        getFileSystem().create(new Path(getAppPath(), jar)).close();
        Path rootJar = new Path(root, "rootJar.jar");
        getFileSystem().create(rootJar).close();

        Path file = new Path("file");
        getFileSystem().create(new Path(getAppPath(), file)).close();
        Path rootFile = new Path(root, "rootFile");
        getFileSystem().create(rootFile).close();

        Path so = new Path("soFile.so");
        getFileSystem().create(new Path(getAppPath(), so)).close();
        Path rootSo = new Path(root, "rootSoFile.so");
        getFileSystem().create(rootSo).close();

        Path so1 = new Path("soFile.so.1");
        getFileSystem().create(new Path(getAppPath(), so1)).close();
        Path rootSo1 = new Path(root, "rootSoFile.so.1");
        getFileSystem().create(rootSo1).close();

        Path archive = new Path("archive.tar");
        getFileSystem().create(new Path(getAppPath(), archive)).close();
        Path rootArchive = new Path(root, "rootArchive.tar");
        getFileSystem().create(rootArchive).close();

        String actionXml = "<map-reduce>" +
                "      <job-tracker>" + getJobTrackerUri() + "</job-tracker>" +
                "      <name-node>" + getNameNodeUri() + "</name-node>" +
                "      <main-class>CLASS</main-class>" +
                "      <file>" + jar.toString() +
                            "," + rootJar.toString() +
                            "," + file.toString() +
                            ", " + rootFile.toString() + // with leading and trailing spaces
                            "  ," + so.toString() +
                            "," + rootSo.toString() +
                            "," + so1.toString() +
                            "," + rootSo1.toString() + "</file>\n" +
                "      <archive>" + archive.toString() + ", "
                            + rootArchive.toString() + " </archive>\n" + // with leading and trailing spaces
                "</map-reduce>";

        Element eActionXml = XmlUtils.parseXml(actionXml);

        Context context = createContext(MAP_REDUCE, actionXml);

        Path appPath = getAppPath();

        MapReduceActionExecutor ae = new MapReduceActionExecutor();

        Configuration jobConf = ae.createBaseHadoopConf(context, eActionXml);
        ae.setupActionConf(jobConf, context, eActionXml, appPath);
        ae.setLibFilesArchives(context, eActionXml, appPath, jobConf);


        assertTrue(DistributedCache.getSymlink(jobConf));

        Path[] filesInClasspath = DistributedCache.getFileClassPaths(jobConf);
        for (Path p : new Path[]{new Path(getAppPath(), jar), rootJar}) {
            boolean found = false;
            for (Path c : filesInClasspath) {
                if (!found && p.toUri().getPath().equals(c.toUri().getPath())) {
                    found = true;
                }
            }
            assertTrue("file " + p.toUri().getPath() + " not found in classpath", found);
        }
        for (Path p : new Path[]{new Path(getAppPath(), file), rootFile, new Path(getAppPath(), so), rootSo,
                                new Path(getAppPath(), so1), rootSo1}) {
            boolean found = false;
            for (Path c : filesInClasspath) {
                if (!found && p.toUri().getPath().equals(c.toUri().getPath())) {
                    found = true;
                }
            }
            assertFalse("file " + p.toUri().getPath() + " found in classpath", found);
        }

        URI[] filesInCache = DistributedCache.getCacheFiles(jobConf);
        for (Path p : new Path[]{new Path(getAppPath(), jar), rootJar, new Path(getAppPath(), file), rootFile,
                                new Path(getAppPath(), so), rootSo, new Path(getAppPath(), so1), rootSo1}) {
            boolean found = false;
            for (URI c : filesInCache) {
                if (!found && p.toUri().getPath().equals(c.getPath())) {
                    found = true;
                }
            }
            assertTrue("file " + p.toUri().getPath() + " not found in cache", found);
        }

        URI[] archivesInCache = DistributedCache.getCacheArchives(jobConf);
        for (Path p : new Path[]{new Path(getAppPath(), archive), rootArchive}) {
            boolean found = false;
            for (URI c : archivesInCache) {
                if (!found && p.toUri().getPath().equals(c.getPath())) {
                    found = true;
                }
            }
            assertTrue("archive " + p.toUri().getPath() + " not found in cache", found);
        }
    }

    private void waitForWorkflowAction(final String actionId) {
        waitFor(3 * 60 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                WorkflowActionBean mrAction = WorkflowActionQueryExecutor.getInstance()
                        .get(WorkflowActionQuery.GET_ACTION, actionId);
                return mrAction.inTerminalState() || mrAction.getStatus() == Status.RUNNING;
            }
        });
    }

    public void testFailingMapReduceJobCausesOozieLauncherAMToFail() throws Exception {
        final String workflowUri = createWorkflowWithMapReduceAction();

        startWorkflowAndFailChildMRJob(workflowUri);
    }

    private String createWorkflowWithMapReduceAction() throws IOException {
        final String workflowUri = getTestCaseFileUri("workflow.xml");
        final String appXml = "<workflow-app xmlns=\"uri:oozie:workflow:1.0\" name=\"workflow\">" +
                "   <start to=\"map-reduce\"/>" +
                "   <action name=\"map-reduce\">" +
                "       <map-reduce>" +
                "           <resource-manager>" + getJobTrackerUri() + "</resource-manager>" +
                "           <name-node>" + getNameNodeUri() + "</name-node>" +
                "           <configuration>\n" +
                "               <property>\n" +
                "                   <name>mapred.job.queue.name</name>\n" +
                "                   <value>default</value>\n" +
                "               </property>\n" +
                "               <property>\n" +
                "                   <name>mapred.mapper.class</name>\n" +
                "                   <value>org.apache.oozie.action.hadoop.SleepMapperReducerForTest</value>\n" +
                "               </property>\n" +
                "               <property>\n" +
                "                   <name>mapred.reducer.class</name>\n" +
                "                   <value>org.apache.oozie.action.hadoop.SleepMapperReducerForTest</value>\n" +
                "               </property>\n" +
                "                <property>\n" +
                "                    <name>mapred.input.dir</name>\n" +
                "                    <value>" + getFsTestCaseDir() + "/input</value>\n" +
                "                </property>\n" +
                "                <property>\n" +
                "                    <name>mapred.output.dir</name>\n" +
                "                    <value>" + getFsTestCaseDir() + "/output</value>\n" +
                "                </property>\n" +
                "           </configuration>\n" +
                "       </map-reduce>" +
                "       <ok to=\"end\"/>" +
                "       <error to=\"fail\"/>" +
                "   </action>" +
                "   <kill name=\"fail\">" +
                "       <message>Sub workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>" +
                "   </kill>" +
                "   <end name=\"end\"/>" +
                "</workflow-app>";

        writeToFile(appXml, workflowUri);

        return workflowUri;
    }

    private void startWorkflowAndFailChildMRJob(final String workflowUri) throws Exception {
        try {
            LocalOozie.start();
            final OozieClient wfClient = LocalOozie.getClient();
            final String workflowId = submitWorkflow(workflowUri, wfClient);
            final Configuration conf = Services.get().get(HadoopAccessorService.class).createConfiguration(getJobTrackerUri());

            final Path inputFolder = createInputFolder(conf);

            waitForWorkflowToStart(wfClient, workflowId);
            waitForChildYarnApplication(getHadoopAccessorService().createYarnClient(getTestUser(), conf), workflowId);
            assertAndWriteNextMRJobId(workflowId, conf, inputFolder);

            final ApplicationId externalChildJobId = getChildMRJobApplicationId(conf);

            killYarnApplication(conf, externalChildJobId);
            waitUntilYarnAppKilledAndAssertSuccess(externalChildJobId.toString());
            waitForWorkflowToKill(wfClient, workflowId);
        } finally {
            LocalOozie.stop();
        }
    }

    /**
     * Get all YARN application IDs, select the one of type {@code MAPREDUCE} that is relevant to {@code workflowId},
     * and write to {@code inputFolder/jobID.txt}.
     * <p>
     * Simulating functional parts of {@link LauncherMain#writeExternalChildIDs(String, Pattern[], String)} in order
     * {@link MapReduceActionExecutor#check(ActionExecutor.Context, WorkflowAction)} can find it later on the call chain.
     * <p>
     * We need to write out an own sequence file to {@link LauncherMainTester#JOB_ID_FILE_NAME} in order
     * {@link ActionExecutorTestCase#getChildMRJobApplicationId(Configuration)} can find it. We unfortunately cannot rely on the
     * original sequence file written by {@link LauncherMain#writeExternalChildIDs(String, Pattern[], String)} because we don't own
     * a reference to the original {@link ActionExecutor.Context} as in {@link MapReduceActionExecutor}.
     * @param workflowId the workflow ID
     * @param conf the {@link Configuration} used for Hadoop Common / YARN API calls
     * @param inputFolder where to write the output text file
     * @throws IOException when the output text file cannot be written
     * @throws YarnException when the list of YARN applications cannot be queried
     * @throws HadoopAccessorException when {@link YarnClient} cannot be created
     */
    private void assertAndWriteNextMRJobId(final String workflowId, final Configuration conf, final Path inputFolder)
            throws IOException, YarnException, HadoopAccessorException {
        final Path wfIDFile = new Path(inputFolder, LauncherMainTester.JOB_ID_FILE_NAME);
        try (final FileSystem fs = FileSystem.get(conf);
             final Writer w = new OutputStreamWriter(fs.create(wfIDFile), StandardCharsets.UTF_8)) {
            final List<ApplicationReport> allApplications =
                    getHadoopAccessorService().createYarnClient(getTestUser(), conf).getApplications();

            assertTrue("YARN applications number mismatch", allApplications.size() >= 2);

            ApplicationReport mapReduce = null;
            for (final ApplicationReport candidate : allApplications) {
                if (candidate.getApplicationType().equals(MapReduceActionExecutor.YARN_APPLICATION_TYPE_MAPREDUCE)
                        && candidate.getName().contains(workflowId)) {
                    mapReduce = candidate;
                }
            }
            assertNotNull("MAPREDUCE YARN application not found", mapReduce);

            final String applicationId = mapReduce.getApplicationId().toString();
            final String nextMRJobId = applicationId.replace("application", "job");

            LOG.debug("Writing next MapReduce job ID: {0}", nextMRJobId);

            w.write(nextMRJobId);
        }
    }

    private Path createInputFolder(final Configuration conf) throws IOException {
        final Path inputDir = new Path(getFsTestCaseDir(), "input");
        try (final FileSystem fs = FileSystem.get(conf)) {
             fs.mkdirs(inputDir);
        }
        return inputDir;
    }

    private void waitForChildYarnApplication(final YarnClient yarnClient, final String workflowId) {
        waitFor(JOB_TIMEOUT, new ChildYarnApplicationPresentPredicate(yarnClient, workflowId));
    }

    private class ChildYarnApplicationPresentPredicate implements Predicate {
        private final YarnClient yarnClient;
        private final String workflowId;

        ChildYarnApplicationPresentPredicate(final YarnClient yarnClient, final String workflowId) {
            this.yarnClient = yarnClient;
            this.workflowId = workflowId;
        }

        @Override
        public boolean evaluate() throws Exception {
            if (yarnClient.getApplications().isEmpty()) {
                return false;
            }

            for (final ApplicationReport applicationReport : yarnClient.getApplications()) {
                final String name = applicationReport.getName();
                final String type = applicationReport.getApplicationType();
                if (type.equals(MapReduceActionExecutor.YARN_APPLICATION_TYPE_MAPREDUCE) && name.contains(workflowId)) {
                    return true;
                }
            }

            return false;
        }
    }
}
