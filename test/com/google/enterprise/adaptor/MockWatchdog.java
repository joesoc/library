// Copyright 2013 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.adaptor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Empty watchdog implementation that never interrupts other threads. */
class MockWatchdog extends Watchdog {
  /**
   * Shared executor that nothing is executed with. It is not shutdown properly
   * to make using the mock easier.
   */
  private static final ScheduledExecutorService executor
      = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setDaemon(true).build());

  public MockWatchdog() {
    super(executor);
  }

  @Override
  public void processingStarting(long timeout) {
  }

  @Override
  public void processingStarting(Thread thread, long timout) {
  }

  @Override
  public void processingCompleted() {
  }

  @Override
  public void processingCompleted(Thread thread) {
  }
}
