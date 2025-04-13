




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
 * Represents a byte size token with unit (KB, MB, GB, etc.)
 */
public class ByteSize implements Token {
  private static final Pattern PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*([kKmMgGtTpP]?[bB])");
  private final double value;
  private final String unit;

  public ByteSize(String text) {
    Matcher matcher = PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid byte size format: " + text);
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

  public long getBytes() {
    BigDecimal multiplier;
    switch (unit.toLowerCase()) {
      case "b":  multiplier = BigDecimal.valueOf(1); break;
      case "kb": multiplier = BigDecimal.valueOf(1024); break;
      case "mb": multiplier = BigDecimal.valueOf(1024).multiply(BigDecimal.valueOf(1024)); break;
      case "gb": multiplier = BigDecimal.valueOf(1024).pow(3); break;
      case "tb": multiplier = BigDecimal.valueOf(1024).pow(4); break;
      case "pb": multiplier = BigDecimal.valueOf(1024).pow(5); break;
      default:
        throw new IllegalStateException("Unsupported unit: " + unit);
    }
    BigDecimal bytes = BigDecimal.valueOf(value).multiply(multiplier);
    return bytes.longValue();
  }

  public static double bytesToUnit(long bytes, String targetUnit) {
    switch (targetUnit.toLowerCase()) {
      case "b":  return bytes;
      case "kb": return bytes / 1024.0;
      case "mb": return bytes / (1024.0 * 1024);
      case "gb": return bytes / (1024.0 * 1024 * 1024);
      case "tb": return bytes / (1024.0 * 1024 * 1024 * 1024);
      case "pb": return bytes / (1024.0 * 1024 * 1024 * 1024 * 1024);
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
    return TokenType.BYTE_SIZE;
  }
  @Override
public JsonElement toJson() {
  return new JsonPrimitive(value);  // or however you want to convert the value
}

}
