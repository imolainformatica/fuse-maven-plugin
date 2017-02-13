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
package it.imolinfo.maven.plugins.jboss.fuse.model;

/**
 *
 * @author giacomo
 */
public class Bundle {
    
    public enum State {
        INSTALLED,
        ACTIVE,
        RESOLVED,
        STARTING,
        STOPPING
    }
    
    private Long id;
    private String name;
    private String version;
    private State state;
    private String blueprintState;
    private String springState;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getBlueprintState() {
        return blueprintState;
    }

    public void setBlueprintState(String blueprintState) {
        this.blueprintState = blueprintState;
    }

    public String getSpringState() {
        return springState;
    }

    public void setSpringState(String springState) {
        this.springState = springState;
    }

}
