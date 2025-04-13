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

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveContext;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A directive that aggregates byte size and time duration values.
 */
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private String sizeColumn;
  private String timeColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private String sizeUnit;
  private String timeUnit;
  private static final String DEFAULT_SIZE_UNIT = "MB";
  private static final String DEFAULT_TIME_UNIT = "s";
  private static final String STORE_PREFIX = "aggregate-stats-";
  
  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("sizeColumn", TokenType.COLUMN_NAME);
    builder.define("timeColumn", TokenType.COLUMN_NAME);
    builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
    builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
    builder.define("sizeUnit", TokenType.STRING, true);
    builder.define("timeUnit", TokenType.STRING, true);
    return builder.build();
  }
  
  @Override
  public void initialize(DirectiveContext context) throws DirectiveParseException {
    this.sizeColumn = ((ColumnName) context.getArgument("sizeColumn")).value();
    this.timeColumn = ((ColumnName) context.getArgument("timeColumn")).value();
    this.totalSizeColumn = ((ColumnName) context.getArgument("totalSizeColumn")).value();
    this.totalTimeColumn = ((ColumnName) context.getArgument("totalTimeColumn")).value();
    
    if (context.hasArgument("sizeUnit")) {
      this.sizeUnit = ((Text) context.getArgument("sizeUnit")).value();
    } else {
      this.sizeUnit = DEFAULT_SIZE_UNIT;
    }
    
    if (context.hasArgument("timeUnit")) {
      this.timeUnit = ((Text) context.getArgument("timeUnit")).value();
    } else {
      this.timeUnit = DEFAULT_TIME_UNIT;
    }
  }
  
  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    // Initialize or get the aggregation store from the context
    Map<String, Object> store = initializeOrGetStore(context);
    
    // Aggregate values from all rows
    for (Row row : rows) {
      if (row.find(sizeColumn) != -1) {
        Object sizeObj = row.getValue(sizeColumn);
        long bytes = convertToBytes(sizeObj);
        Long totalBytes = (Long) store.getOrDefault("totalBytes", 0L);
        store.put("totalBytes", totalBytes + bytes);
      }
      
      if (row.find(timeColumn) != -1) {
        Object timeObj = row.getValue(timeColumn);
        long milliseconds = convertToMilliseconds(timeObj);
        Long totalMilliseconds = (Long) store.getOrDefault("totalMilliseconds", 0L);
        store.put("totalMilliseconds", totalMilliseconds + milliseconds);
      }
    }
    
    // Update the store in the context for future calls to this directive
    context.getTransientStore().put(getStoreKey(), store);
    
    // Check if this is the final batch
    if (context.isFinalBatch()) {
      return createAggregateResult(store);
    }
    
    // For non-final batches, return empty list to continue processing
    return rows;
  }
  
  private Map<String, Object> initializeOrGetStore(ExecutorContext context) {
    String key = getStoreKey();
    if (!context.getTransientStore().has(key)) {
      Map<String, Object> store = new HashMap<>();
      store.put("totalBytes", 0L);
      store.put("totalMilliseconds", 0L);
      context.getTransientStore().put(key, store);
      return store;
    }
    return (Map<String, Object>) context.getTransientStore().get(key);
  }
  
  private String getStoreKey() {
    return STORE_PREFIX + sizeColumn + "-" + timeColumn;
  }
  
  private long convertToBytes(Object obj) {
    if (obj instanceof ByteSize) {
      return ((ByteSize) obj).getBytes();
    } else if (obj instanceof String) {
      try {
        return new ByteSize((String) obj).getBytes();
      } catch (Exception e) {
        // If not a valid byte size format, try parsing as a number
        try {
          return Long.parseLong((String) obj);
        } catch (NumberFormatException ex) {
          throw new DirectiveExecutionException("Unable to convert value to bytes: " + obj);
        }
      }
    } else if (obj instanceof Number) {
      return ((Number) obj).longValue();
    }
    throw new DirectiveExecutionException("Unable to convert value to bytes: " + obj);
  }
  
  private long convertToMilliseconds(Object obj) {
    if (obj instanceof TimeDuration) {
      return ((TimeDuration) obj).getMilliseconds();
    } else if (obj instanceof String) {
      try {
        return new TimeDuration((String) obj).getMilliseconds();
      } catch (Exception e) {
        // If not a valid time duration format, try parsing as a number
        try {
          return Long.parseLong((String) obj);
        } catch (NumberFormatException ex) {
          throw new DirectiveExecutionException("Unable to convert value to milliseconds: " + obj);
        }
      }
    } else if (obj instanceof Number) {
      return ((Number) obj).longValue();
    }
    throw new DirectiveExecutionException("Unable to convert value to milliseconds: " + obj);
  }
  
  private List<Row> createAggregateResult(Map<String, Object> store) {
    Row result = new Row();
    
    // Convert total bytes to the specified unit
    long totalBytes = (Long) store.getOrDefault("totalBytes", 0L);
    double totalSizeInUnit = convertBytesToUnit(totalBytes, sizeUnit);
    result.add(totalSizeColumn, totalSizeInUnit);
    
    // Convert total milliseconds to the specified unit
    long totalMilliseconds = (Long) store.getOrDefault("totalMilliseconds", 0L);
    double totalTimeInUnit = convertMillisecondsToUnit(totalMilliseconds, timeUnit);
    result.add(totalTimeColumn, totalTimeInUnit);
    
    return List.of(result);
  }
  
  private double convertBytesToUnit(long bytes, String unit) {
    switch (unit.toUpperCase()) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024.0);
      case "GB":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      case "PB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
      default:
        throw new DirectiveExecutionException("Unrecognized byte unit: " + unit);
    }
  }
  
  private double convertMillisecondsToUnit(long milliseconds, String unit) {
    switch (unit.toLowerCase()) {
      case "ms":
        return milliseconds;
      case "s":
        return milliseconds / 1000.0;
      case "m":
        return milliseconds / (60.0 * 1000.0);
      case "h":
        return milliseconds / (60.0 * 60.0 * 1000.0);
      case "d":
        return milliseconds / (24.0 * 60.0 * 60.0 * 1000.0);
      default:
        throw new DirectiveExecutionException("Unrecognized time unit: " + unit);
    }
  }
}