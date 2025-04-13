/*
 * Copyright © 2023 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
//  */
package io.cdap.wrangler.core.directive.aggregate;

import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.test.TestingRig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AggregateStatsTest {

  @Test
  public void testBasicAggregation() {
    // Create test data
    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Row row = new Row();
      row.add("data_transfer_size", (i + 1) + "KB");
      row.add("response_time", (i + 1) * 100 + "ms");
      rows.add(row);
    }
    
    // Define recipe
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
    };
    
    // Execute the recipe
    List<Row> results = TestingRig.execute(recipe, rows);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    
    // Expected: (1+2+3+4+5)KB = 15KB = 0.01465 MB
    Assert.assertEquals(15 * 1024 / (1024.0 * 1024.0), 
                       results.get(0).getValue("total_size_mb"), 
                       0.001);
    
    // Expected: (100+200+300+400+500)ms = 1500ms = 1.5s
    Assert.assertEquals(1.5, 
                       results.get(0).getValue("total_time_sec"), 
                       0.001);
  }
  
  @Test
  public void testMixedUnits() {
    // Create test data with mixed units
    List<Row> rows = new ArrayList<>();
    
    Row row1 = new Row();
    row1.add("size", "1KB");
    row1.add("time", "500ms");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("size", "1MB");
    row2.add("time", "1s");
    rows.add(row2);
    
    Row row3 = new Row();
    row3.add("size", "5KB");
    row3.add("time", "1.5s");
    rows.add(row3);
    
    // Define recipe with custom output units
    String[] recipe = new String[] {
      "aggregate-stats :size :time total_size_kb total_time_ms KB ms"
    };
    
    // Execute the recipe
    List<Row> results = TestingRig.execute(recipe, rows);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    
    // Expected: 1KB + 1MB + 5KB = 1030KB
    Assert.assertEquals(1030.0, 
                       results.get(0).getValue("total_size_kb"), 
                       0.001);
    
    // Expected: 500ms + 1000ms + 1500ms = 3000ms
    Assert.assertEquals(3000.0, 
                       results.get(0).getValue("total_time_ms"), 
                       0.001);
  }
  
  @Test
  public void testNullValues() {
    // Create test data with some null values
    List<Row> rows = new ArrayList<>();
    
    Row row1 = new Row();
    row1.add("size", "1KB");
    row1.add("time", "500ms");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("size", null);
    row2.add("time", "1s");
    rows.add(row2);
    
    Row row3 = new Row();
    row3.add("size", "5KB");
    row3.add("time", null);
    rows.add(row3);
    
    // Define recipe
    String[] recipe = new String[] {
      "aggregate-stats :size :time total_size_mb total_time_sec"
    };
    
    // Execute the recipe
    List<Row> results = TestingRig.execute(recipe, rows);
    
    // Verify results (should ignore null values)
    Assert.assertEquals(1, results.size());
    
    // Expected: 1KB + 5KB = 6KB = 0.00586 MB
    Assert.assertEquals(6 * 1024 / (1024.0 * 1024.0), 
                       results.get(0).getValue("total_size_mb"), 
                       0.001);
    
    // Expected: 500ms + 1000ms = 1500ms = 1.5s
    Assert.assertEquals(1.5, 
                       results.get(0).getValue("total_time_sec"), 
                       0.001);
  }
}