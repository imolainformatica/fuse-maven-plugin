/*
 * Copyright 2016 Imola Informatica S.P.A..
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
 */
package it.imolinfo.maven.plugins.jboss.fuse.options;

import java.io.File;

/**
 *
 * @author giacomo
 */
public class Deployment {
    public static final String DEFAULT_EXPECTED_STATUS = ".*Active";
    private String expectedStatus = DEFAULT_EXPECTED_STATUS;
    private String expectedContextStatus;
    private String deploymentName;
    private File source;
    private Long waitTime;
    private Long timeout;

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getExpectedContextStatus() {
        return expectedContextStatus;
    }

    public void setExpectedContextStatus(String expectedContextStatus) {
        this.expectedContextStatus = expectedContextStatus;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    
    public Long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Long waitTime) {
        this.waitTime = waitTime;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
    
}
