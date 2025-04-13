/*
 * Copyright © 2024 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

public class TimeDurationTest {

  @Test
  public void testValidTimeDurations() {
    // Test basic units
    Assert.assertEquals(1, new TimeDuration("1ms").getMilliseconds());
    Assert.assertEquals(1000, new TimeDuration("1s").getMilliseconds());
    Assert.assertEquals(60 * 1000, new TimeDuration("1m").getMilliseconds());
    Assert.assertEquals(60 * 60 * 1000, new TimeDuration("1h").getMilliseconds());
    Assert.assertEquals(24 * 60 * 60 * 1000, new TimeDuration("1d").getMilliseconds());
    
    // Test case insensitivity
    Assert.assertEquals(1, new TimeDuration("1MS").getMilliseconds());
    Assert.assertEquals(1000, new TimeDuration("1S").getMilliseconds());
    
    // Test decimal values
    Assert.assertEquals(1500, new TimeDuration("1.5s").getMilliseconds());
    Assert.assertEquals(90 * 1000, new TimeDuration("1.5m").getMilliseconds());
    
    // Test with spaces
    Assert.assertEquals(2000, new TimeDuration("2 s").getMilliseconds());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("invalid");
  }
  
  @Test
  public void testConversion() {
    TimeDuration duration = new TimeDuration("120s");
    
    Assert.assertEquals(120 * 1000, duration.getMilliseconds());
    Assert.assertEquals(120000, duration.convertTo("ms"), 0.001);
    Assert.assertEquals(120, duration.convertTo("s"), 0.001);
    Assert.assertEquals(2, duration.convertTo("m"), 0.001);
    Assert.assertEquals(0.033333, duration.convertTo("h"), 0.001);
  }
  
  @Test
  public void testGetters() {
    TimeDuration duration = new TimeDuration("10.5s");
    
    Assert.assertEquals(10.5, duration.getValue(), 0.001);
    Assert.assertEquals("s", duration.getUnit());
  }
}
