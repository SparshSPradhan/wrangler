# Wrangler
A data preparation tool for data cleaning, transformation and preprocessing.
## Byte Size and Time Duration Unit Parsers
This implementation enhances the Wrangler core library by adding native support for parsing and utilizing byte size and time duration units within recipes.
### Byte Size Parser
The ByteSize parser accepts values in the following format:
- `<number><unit>` (e.g., "10KB", "1.5MB")
- Supported units: B, KB, MB, GB, TB, PB (case-insensitive)
Usage in directives:
```
set-column :new_size 10MB
set-column :scaled_size bytes(size_column, "MB")
```
### Time Duration Parser
The TimeDuration parser accepts values in the following format:
- `<number><unit>` (e.g., "100ms", "1.5s")
- Supported units: ms, s, m, h, d (case-insensitive)
Usage in directives:
```
set-column :new_duration 10s
set-column :scaled_duration duration(time_column, "s")
```
### Aggregate Stats Directive
The `aggregate-stats` directive uses both parsers to aggregate byte size and time duration values:
```
aggregate-stats :size_column :time_column total_size total_time [output_size_unit] [output_time_unit]
```
Parameters:
- `size_column`: Source column containing byte sizes (e.g., "10KB", "1.5MB")
- `time_column`: Source column containing time durations (e.g., "100ms", "1.5s")
- `total_size`: Target column name for the total size
- `total_time`: Target column name for the total time
- `output_size_unit`: (Optional) Unit for the total size output (default: "MB")
- `output_time_unit`: (Optional) Unit for the total time output (default: "s")

## Installation

### Requirements
- Java 8 or higher
- Wrangler 2.0.0 or higher

### Building from source
Clone the repository and build using Maven:

```bash
git clone https://github.com/yourusername/wrangler-unit-parsers.git
cd wrangler-unit-parsers
mvn clean package
```

The built JAR file will be available in the `target` directory.

### Installing the plugin
Copy the JAR file to your Wrangler plugins directory:

```bash
cp target/wrangler-unit-parsers-1.0.0.jar /path/to/wrangler/plugins/
```

Restart the Wrangler service to load the plugin.

## Usage Examples

### Basic Byte Size Conversion
Convert file sizes in a column to a standardized unit:

```
set-column :standardized_size bytes(file_size, "MB")
```

### Time Duration Calculations
Calculate execution time in minutes:

```
set-column :minutes_taken duration(execution_time, "m")
```

### Aggregate Statistics
Summarize total file sizes and processing times:

```
aggregate-stats :file_sizes :processing_times total_size total_time GB h
```

This will create two new columns: `total_size` with the sum of all file sizes in gigabytes, and `total_time` with the sum of all processing times in hours.

### Filtering Based on Size or Duration
Use the parsers in filter directives:

```
filter-rows-on condition: bytes(file_size) > bytes("500MB")
```

```
filter-rows-on condition: duration(processing_time) < duration("1h")
```

## API Reference

### ByteSizeParser Methods
- `parseBytes(String value)`: Converts a byte size string to bytes
- `convertBytes(long bytes, String targetUnit)`: Converts bytes to a specific unit
- `formatBytes(long bytes, String targetUnit)`: Formats bytes into a human-readable string

### TimeDurationParser Methods
- `parseMilliseconds(String value)`: Converts a time duration string to milliseconds
- `convertMilliseconds(long milliseconds, String targetUnit)`: Converts milliseconds to a specific unit
- `formatDuration(long milliseconds, String targetUnit)`: Formats milliseconds into a human-readable string

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
This project is licensed under the Apache License 2.0 - see the LICENSE file for details.