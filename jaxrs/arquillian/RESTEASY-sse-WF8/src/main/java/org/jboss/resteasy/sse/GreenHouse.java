/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.resteasy.sse;

public class GreenHouse  {
    
    private long timestamp;
    private int temperature;
    private int humidity;
    
    public GreenHouse() {
    }

    public GreenHouse(long timestamp, int temperature, int humidity) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTempe() {
        return temperature;
    }

    public void setTempe(int temperature) {
        this.temperature = temperature;
    }
    
    public int getHumid() {
        return humidity;
    }

    public void setHumid(int humidity) {
        this.humidity = humidity;
    }
}
