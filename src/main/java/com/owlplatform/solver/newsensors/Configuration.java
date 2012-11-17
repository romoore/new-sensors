/*
 * Owl Platform
 * Copyright (C) 2012 Robert Moore and the Owl Platform
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.owlplatform.solver.newsensors;

/**
 * @author Robert Moore
 *
 */
public class Configuration {

  private String aggregatorHost = "localhost";
  private String worldModelHost = "localhost";
  private int aggregatorPort = 7008;
  private int wmSolverPort = 7009;
  private int wmClientPort = 7010;
  private long updateFreqMillis = 30000;
  public String getAggregatorHost() {
    return aggregatorHost;
  }
  public void setAggregatorHost(String aggregatorHost) {
    this.aggregatorHost = aggregatorHost;
  }
  public String getWorldModelHost() {
    return worldModelHost;
  }
  public void setWorldModelHost(String worldModelHost) {
    this.worldModelHost = worldModelHost;
  }
  public int getAggregatorPort() {
    return aggregatorPort;
  }
  public void setAggregatorPort(int aggregatorPort) {
    this.aggregatorPort = aggregatorPort;
  }
  public int getWmSolverPort() {
    return wmSolverPort;
  }
  public void setWmSolverPort(int wmSolverPort) {
    this.wmSolverPort = wmSolverPort;
  }
  public int getWmClientPort() {
    return wmClientPort;
  }
  public void setWmClientPort(int wmClientPort) {
    this.wmClientPort = wmClientPort;
  }
  public long getUpdateFreqMillis() {
    return updateFreqMillis;
  }
  public void setUpdateFreqMillis(long updateFreqMillis) {
    this.updateFreqMillis = updateFreqMillis;
  }
}
