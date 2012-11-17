/*
 * Owl Platform Copyright (C) 2012 Robert Moore and the Owl Platform
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.owlplatform.solver.newsensors;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.solver.SolverAggregatorConnection;
import com.owlplatform.solver.rules.SubscriptionRequestRule;
import com.owlplatform.worldmodel.Attribute;
import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.client.StepResponse;
import com.owlplatform.worldmodel.client.WorldState;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage.AttributeSpecification;
import com.thoughtworks.xstream.XStream;

/**
 * A solver that detects the presence of devices that are not yet registered and
 * then adds them to the world model.
 * 
 * @author Robert Moore
 */
public class NewSensors extends Thread {

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

    final NewSensors solver = new NewSensors(config);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        solver.shutdown();
      }
    });

    solver.init();

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

  private final transient SolverWorldConnection solverWm;
  private final transient ClientWorldConnection clientWm;
  private final transient SolverAggregatorConnection solverAgg;

  private static final String PHY_ATTRIBUTE = "physical layer";
  private static final String DEVICE_ATTRIBUTE = "device id";
  private static final String UNKNOWN_IDENTIFIER = "unknown-device";

  private transient StepResponse wmUpdates = null;

  private final transient long updateFrequency;

  private transient boolean keepRunning = true;

  /**
   * Sensors already defined in the world model.
   */
  private final transient Set<String> existingSensors = new HashSet<String>();

  /**
   * String representation of sensors that have been "reported" as unregistered
   * to the world model.
   */
  private final transient Set<String> reportedSensors = new HashSet<String>();

  /**
   * Mapping from deviceID string to Identifier value inserted into the world
   * model.
   */
  private final transient Map<String, String> reportedIdentifier = new HashMap<String, String>();

  /**
   * String representation of sensors that need to be "reported" on the next
   * update.
   */
  private final transient Set<String> sensorsToReport = new HashSet<String>();

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

    this.updateFrequency = this.config.getUpdateFreqMillis();
  }

  /**
   * Initializes connections to the aggregator and world model.
   * 
   * @return {@code true} if the connections succeed, else {@code false} if at
   *         least one fails
   */
  public boolean init() {
    if (!this.solverAgg.connect(5000l)) {
      log.error("Unable to connect to the aggregator.");
      return false;
    }

    if (!this.solverWm.connect(5000l)) {
      log.error("Unable to connect to the world model as a solver.");
      return false;
    }

    if (!this.clientWm.connect(5000l)) {
      log.error("Unable to connect to the world model as a client.");
      return false;
    }
    return true;
  }

  /**
   * Terminates all connections and shuts down this solver.
   */
  public void shutdown() {
    this.solverAgg.disconnect();
    this.solverWm.disconnect();
    this.clientWm.disconnect();
  }

  /**
   * Configures all of the connections (aggregator, world model) with the
   * correct settings to prepare sending and receiving of information.
   * <ul>
   * <li>Send a subscription request to the aggregator</li>
   * <li>Send a stream request to the world model</li>
   * <li>Register attribute types with the world model</li>
   * </ul>
   */
  public void createSubscriptions() {
    // Aggregator subscription
    SubscriptionRequestRule allDevices = new SubscriptionRequestRule();
    allDevices.setPhysicalLayer(SampleMessage.PHYSICAL_LAYER_ALL);
    allDevices.setUpdateInterval(this.updateFrequency);
    this.solverAgg.addRule(allDevices);

    // Get all devices with sensor values
    this.wmUpdates = this.clientWm.getStreamRequest(".*",
        System.currentTimeMillis(), this.updateFrequency, ".*sensor.*");

    // Register the two basic attributes with the world model
    AttributeSpecification spec = new AttributeSpecification();
    spec.setAttributeName(DEVICE_ATTRIBUTE);
    spec.setIsOnDemand(false);
    this.solverWm.addAttribute(spec);
    spec = new AttributeSpecification();
    spec.setAttributeName(PHY_ATTRIBUTE);
    spec.setIsOnDemand(false);
    this.solverWm.addAttribute(spec);
  }

  @Override
  public void run() {
    long nextUpdate = System.currentTimeMillis() + this.updateFrequency;
    long now = System.currentTimeMillis();
    while (this.keepRunning) {
      if (now >= nextUpdate) {
        nextUpdate += this.updateFrequency;
        this.performUpdates();
      }

      final SampleMessage sample = this.solverAgg.getNextSample();
      String devId = fromSampleData(sample.getPhysicalLayer(),
          sample.getDeviceId());
      if (!this.existingSensors.contains(devId)
          && !this.reportedSensors.contains(devId)) {
        this.sensorsToReport.add(devId);
      }

      now = System.currentTimeMillis();
    }
  }

  private static String fromSensorAttribute(final byte[] attributeData) {
    if (attributeData == null) {
      log.warn("Null sensor attribute data.");
      return null;
    }
    if (attributeData.length < 2) {
      log.warn("Sensor attribute data is fewer than 2 bytes:",
          Arrays.toString(attributeData));
      return null;
    }

    StringBuilder sBuff = new StringBuilder((attributeData.length) * 2);

    for (int i = 0; i < attributeData.length; ++i) {
      sBuff.append(String.format("%2x",
          Integer.valueOf(attributeData[i] & 0xFF)));
    }
    return sBuff.toString();

  }

  private static String fromSampleData(final byte phy, final byte[] id) {
    StringBuilder sBuff = new StringBuilder((id.length + 1) * 2);
    sBuff.append(String.format("%2x", Integer.valueOf((phy & 0xFF))));

    for (int i = 0; i < id.length; ++i) {
      sBuff.append(String.format("%2x", Integer.valueOf(id[i] & 0xFF)));
    }
    return sBuff.toString();
  }

  private void performUpdates() {
    // Check sensors to remove
    while (this.wmUpdates.hasNext() && !this.wmUpdates.isError()) {
      try {
        WorldState state = this.wmUpdates.next();

        for (String identifier : state.getIdentifiers()) {
          Collection<Attribute> attributes = state.getState(identifier);
          for (Attribute attr : attributes) {
            // Only requested ".*sensor.*", so these should all be sensor values
            String devId = fromSensorAttribute(attr.getData());
            if (devId != null) {
              this.existingSensors.add(devId);
            }
          }
        }
      } catch (Exception e) {
        log.error(
            "An exception occurred while getting updates from the world model.",
            e);
        this.shutdown();
        break;
      }

      LinkedList<String> toRemove = new LinkedList<String>();

      // Ok, now determine which sensors to "remove"
      for (String reported : this.reportedSensors) {
        if (this.existingSensors.contains(reported)) {
          toRemove.add(reported);
        }
      }

      // Remove any sensors previously reported
      if (!toRemove.isEmpty()) {
        for(Iterator<String> iter = toRemove.iterator(); iter.hasNext();){
          String deviceId = iter.next();
          
          String identifier  = this.reportedIdentifier.get(deviceId);
          this.solverWm.delete(identifier);
          
          iter.remove();
           
        }
      }

    }
    if (this.wmUpdates.isError()) {
      log.error("An error has occurred: {}", this.wmUpdates.getError());
      this.shutdown();
    }
  }

}
