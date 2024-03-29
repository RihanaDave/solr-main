/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.cluster.placement.impl;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.Phaser;
import org.apache.solr.cluster.placement.PlacementPlugin;
import org.apache.solr.cluster.placement.PlacementPluginConfig;
import org.apache.solr.cluster.placement.PlacementPluginFactory;

/** Helper class to support dynamic reloading of plugin implementations. */
public final class DelegatingPlacementPluginFactory
    implements PlacementPluginFactory<PlacementPluginConfig> {
  private volatile PlacementPluginFactory<? extends PlacementPluginConfig> delegate;
  // support for tests to make sure the update is completed
  private volatile Phaser phaser;
  private final PlacementPluginFactory<?> defaultPlacementPluginFactory;

  /**
   * Constructor.
   *
   * @param defaultPlacementPluginFactory A {@link PlacementPluginFactory} to use when no delegate
   *     is defined.
   */
  public DelegatingPlacementPluginFactory(PlacementPluginFactory<?> defaultPlacementPluginFactory) {
    this.defaultPlacementPluginFactory = defaultPlacementPluginFactory;
  }

  @Override
  public PlacementPlugin createPluginInstance() {
    if (delegate != null) {
      return delegate.createPluginInstance();
    } else {
      return defaultPlacementPluginFactory.createPluginInstance();
    }
  }

  @Override
  public PlacementPluginConfig getConfig() {
    if (delegate != null) {
      return delegate.getConfig();
    } else {
      return defaultPlacementPluginFactory.getConfig();
    }
  }

  /**
   * A phaser that will advance phases every time {@link #setDelegate(PlacementPluginFactory)} is
   * called. Useful for allowing tests to know when a new delegate is finished getting set.
   */
  @VisibleForTesting
  public void setDelegationPhaser(Phaser phaser) {
    phaser.register();
    this.phaser = phaser;
  }

  public void setDelegate(PlacementPluginFactory<? extends PlacementPluginConfig> delegate) {
    this.delegate = delegate;
    Phaser localPhaser = phaser; // volatile read
    if (localPhaser != null) {
      assert localPhaser.getRegisteredParties() == 1;
      // we should be the only ones registered, so this will advance phase each time
      localPhaser.arrive();
    }
  }

  @VisibleForTesting
  public PlacementPluginFactory<? extends PlacementPluginConfig> getDelegate() {
    return delegate;
  }
}
