
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

package io.cdap.wrangler.api.parser;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Represents a time duration token with unit (ms, s, m, h, d)
 */
public class TimeDuration implements Token {
  private static final Pattern PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(ms|s|m|h|d)");
  private final double value;
  private final String unit;

  public TimeDuration(String text) {
    Matcher matcher = PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time duration format: " + text);
    }
    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(3);
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public long getMilliseconds() {
    BigDecimal multiplier;
    switch (unit) {
      case "ms": multiplier = BigDecimal.valueOf(1); break;
      case "s":  multiplier = BigDecimal.valueOf(1000); break;
      case "m":  multiplier = BigDecimal.valueOf(60 * 1000); break;
      case "h":  multiplier = BigDecimal.valueOf(60 * 60 * 1000); break;
      case "d":  multiplier = BigDecimal.valueOf(24 * 60 * 60 * 1000); break;
      default:
        throw new IllegalStateException("Unsupported unit: " + unit);
    }
    BigDecimal milliseconds = BigDecimal.valueOf(value).multiply(multiplier);
    return milliseconds.longValue();
  }

  public long getNanoseconds() {
    return BigDecimal.valueOf(getMilliseconds())
                      .multiply(BigDecimal.valueOf(1_000_000))
                      .longValue();
  }

  public static double millisecondsToUnit(long milliseconds, String targetUnit) {
    switch (targetUnit) {
      case "ms": return milliseconds;
      case "s":  return milliseconds / 1000.0;
      case "m":  return milliseconds / (60.0 * 1000);
      case "h":  return milliseconds / (60.0 * 60 * 1000);
      case "d":  return milliseconds / (24.0 * 60 * 60 * 1000);
      default:
        throw new IllegalArgumentException("Unsupported target unit: " + targetUnit);
    }
  }
  @Override
  public Object value() {
    return value;
  }
  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }
  @Override
public JsonElement toJson() {
  return new JsonPrimitive(value);  // or however you want to convert the value
}

}
