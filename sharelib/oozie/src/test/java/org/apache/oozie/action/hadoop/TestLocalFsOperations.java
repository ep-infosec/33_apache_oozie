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

import org.junit.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestLocalFsOperations {
    @Test
    public void testPrintContentsOfDirWithRetry() throws Exception {
        LocalFsOperations spy = spy(new LocalFsOperations());
        // Throw an exception on 1st call
        when(spy.listContents(new File(".").toPath()))
                .thenThrow(NoSuchFileException.class)
                .thenReturn("success");
        spy.printContentsOfDir(new File("."));
    }
}
