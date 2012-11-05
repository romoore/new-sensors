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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.solver.SolverAggregatorConnection;
import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.thoughtworks.xstream.XStream;

/**
 * A solver that detects the presence of devices that are not yet registered and
 * then adds them to the world model.
 * 
 * @author Robert Moore
 * 
 */
public class NewSensors {

  /**
   * Logging for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NewSensors.class);
  
  /**
   * @param args
   */
  public static void main(String[] args) {

    if (args.length < 1) {
      printUsageInfo();
      return;
    }
    XStream x = new XStream();
    Configuration config = (Configuration) x.fromXML(new File(args[0]));

    NewSensors solver = new NewSensors(config);

  }

  /**
   * Prints how the solver should be called from the commandline.
   */
  private static void printUsageInfo() {
    System.out.println("Usage: <Configuration File>");
  }

  /**
   * The configuration for the solver.
   */
  private final Configuration config;
  
  private SolverWorldConnection solverWm;
  private ClientWorldConnection clientWm;
  private SolverAggregatorConnection solverAgg;

  /**
   * Creates a new solver with the specified configuration.
   * 
   * @param config
   *          the configuration for the solver.
   */
  public NewSensors(final Configuration config) {
    super();
    this.config = config;
    
    this.solverAgg = new SolverAggregatorConnection();
    this.solverAgg.setBufferWarning(false);
    this.solverAgg.setHost(this.config.getAggregatorHost());
    this.solverAgg.setPort(this.config.getAggregatorPort());
    
    this.solverWm = new SolverWorldConnection();
    this.solverWm.setHost(this.config.getWorldModelHost());
    this.solverWm.setPort(this.config.getWmSolverPort());
    this.solverWm.setOriginString("new-device-solver");
    
    this.clientWm = new ClientWorldConnection();
    this.clientWm.setHost(this.config.getWorldModelHost());
    this.clientWm.setPort(this.config.getWmClientPort());
  }
  
  public boolean init(){
    if(!this.solverAgg.connect(5000l)){
      log.error("Unable to connect to the aggregator.");
      return false;
    }
    
    if(!this.solverWm.connect(5000l)){
      log.error("Unable to connect to the world model as a solver.");
      return false;
    }
    
    if(!this.clientWm.connect(5000l)){
      log.error("Unable to connect to the world model as a client.");
      return false;
    }
    return true;
  }

}
