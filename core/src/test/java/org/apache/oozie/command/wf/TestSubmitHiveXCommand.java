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

package org.apache.oozie.command.wf;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.action.hadoop.ActionUtils;
import org.apache.oozie.client.XOozieClient;
import org.apache.oozie.test.XFsTestCase;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;
import org.apache.oozie.service.XLogService;
import org.jdom2.Element;
public class TestSubmitHiveXCommand extends XFsTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setSystemProperty(XLogService.LOG4J_FILE, "oozie-log4j.properties");
        LocalOozie.start();
    }

    @Override
    protected void tearDown() throws Exception {
        LocalOozie.stop();
        super.tearDown();
    }

    public void testWFXmlGeneration() throws Exception {
        Configuration conf = new Configuration();

        conf.set(XOozieClient.RM, "jobtracker");
        conf.set(XOozieClient.NN, "namenode");
        conf.set(OozieClient.LIBPATH, "libpath");

        conf.set(XOozieClient.FILES, "/user/oozie/input1.txt,/user/oozie/input2.txt#my.txt");
        conf.set(XOozieClient.ARCHIVES, "/user/oozie/udf1.jar,/user/oozie/udf2.jar#my.jar");

        String hiveArgsStr = "-a aaa -b bbb -c ccc -M -Da=aaa -Db=bbb -param input=abc";
        String[] args = hiveArgsStr.split(" ");
        ActionUtils.setStrings(conf, XOozieClient.HIVE_OPTIONS, args);
        String[] params = new String[]{"INPUT=/some/path", "OUTPUT=/some/other/path", "abc=xyz"};
        ActionUtils.setStrings(conf, XOozieClient.HIVE_SCRIPT_PARAMS, params);

        SubmitHiveXCommand submitHiveCmd = new SubmitHiveXCommand(conf);
        String xml = submitHiveCmd.getWorkflowXml(conf);

        XLog.getLog(getClass()).info("xml = " + xml);

        StringBuilder sb = new StringBuilder();
        sb.append("<workflow-app xmlns=\"uri:oozie:workflow:0.2\" name=\"oozie-hive\">");
        sb.append("<start to=\"hive1\" />");
        sb.append("<action name=\"hive1\">");
        sb.append("<hive xmlns=\"uri:oozie:hive-action:0.5\">");
        sb.append("<job-tracker>jobtracker</job-tracker>");
        sb.append("<name-node>namenode</name-node>");
        sb.append("<configuration>");
        sb.append("<property>");
        sb.append("<name>a</name>");
        sb.append("<value>aaa</value>");
        sb.append("</property>");
        sb.append("<property>");
        sb.append("<name>b</name>");
        sb.append("<value>bbb</value>");
        sb.append("</property>");
        sb.append("</configuration>");
        sb.append("<script>dummy.hive</script>");
        sb.append("<param>INPUT=/some/path</param>");
        sb.append("<param>OUTPUT=/some/other/path</param>");
        sb.append("<param>abc=xyz</param>");
        sb.append("<argument>-a</argument>");
        sb.append("<argument>aaa</argument>");
        sb.append("<argument>-b</argument>");
        sb.append("<argument>bbb</argument>");
        sb.append("<argument>-c</argument>");
        sb.append("<argument>ccc</argument>");
        sb.append("<argument>-M</argument>");
        sb.append("<argument>-param</argument>");
        sb.append("<argument>input=abc</argument>");
        sb.append("<file>/user/oozie/input1.txt#input1.txt</file>");
        sb.append("<file>/user/oozie/input2.txt#my.txt</file>");
        sb.append("<archive>/user/oozie/udf1.jar#udf1.jar</archive>");
        sb.append("<archive>/user/oozie/udf2.jar#my.jar</archive>");
        sb.append("</hive>");
        sb.append("<ok to=\"end\" />");
        sb.append("<error to=\"fail\" />");
        sb.append("</action>");
        sb.append("<kill name=\"fail\">");
        sb.append("<message>hive failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>");
        sb.append("</kill>");
        sb.append("<end name=\"end\" />");
        sb.append("</workflow-app>");

        Element root = XmlUtils.parseXml(sb.toString());
        String reference = XmlUtils.prettyPrint(root).toString();

        XLog.getLog(getClass()).info("reference xml = " + reference);
        assertTrue(xml.equals(reference));
    }
}
