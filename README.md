# Toon4j - Token-Oriented Object Notation 4 Java

[![Toon4j Tests](https://github.com/rickenbazolo/toon4j/actions/workflows/ci.yml/badge.svg)](https://github.com/rickenbazolo/toon4j/actions/workflows/ci.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-red.svg)](https://maven.apache.org/)

[![serialization](https://img.shields.io/badge/tag-serialization-green)](https://github.com/topics/serialization)
[![tokenization](https://img.shields.io/badge/tag-tokenization-green)](https://github.com/topics/tokenization)
[![data-format](https://img.shields.io/badge/tag-data--format-green)](https://github.com/topics/data-format)
[![llm](https://img.shields.io/badge/tag-llm-green)](https://github.com/topics/llm)

A Java implementation of **TOON** (Token-Oriented Object Notation), a compact serialization format optimized to reduce token usage when interacting with Large Language Models (LLMs).

> **Origin**: This Java implementation is based on the TOON format specification defined at [toon-format-spec](https://github.com/toon-format/spec). This version fully respects the TOON specifications while focusing on providing a robust Java API for easy integration into existing projects.

## 🎯 What is TOON?

TOON is a data format designed to transmit structured information to LLMs with **30-60% fewer tokens** than JSON. It combines the indentation-based structure of YAML with the tabular format of CSV, while remaining human-readable.

### Comparison Example

**JSON (257 tokens):**
```json
{
  "users": [
    { "id": 1, "name": "Alice", "role": "admin" },
    { "id": 2, "name": "Bob", "role": "user" }
  ]
}
```

**TOON (166 tokens - 35% reduction):**
```toon
users[2]{id,name,role}:
1,Alice,admin
2,Bob,user
```

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.rickenbazolo</groupId>
    <artifactId>toon4j</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.rickenbazolo:toon4j:$version'
```

## 🚀 Quick Start

### Basic Encoding

```java
import com.rickenbazolo.toon.Toon;

import java.util.*;

// Simple object
Map<String, Object> user = Map.of(
        "id", 123,
        "name", "Ada",
        "active", true
);

        String toon = Toon.encode(user);
// Result: id: 123\nname: Ada\nactive: true
```

### Basic Decoding

```java
// Decode to JsonNode
JsonNode node = Toon.decode("name: Alice\nage: 30");

// Decode to specific type
User user = Toon.decode("name: Alice\nage: 30", User.class);
```

### JSON Conversion

```java
// Convert JSON to TOON
String toonString = Toon.fromJson(jsonString);

// Convert TOON to JSON
String jsonString = Toon.toJson(toonString);
```

## 📊 Array Formats

TOON automatically optimizes array representation based on content:

### 1. Tabular Arrays (Objects with identical keys)
```toon
products[3]{id,name,price}:
1,Laptop,999.99
2,Mouse,29.99
3,Keyboard,79.99
```

### 2. Primitive Arrays
```toon
numbers[5]: 1,2,3,4,5
tags[3]: java,python,javascript
```

### 3. List Arrays (Complex nested data)
```toon
items[2]:
- name: Item 1
  details:
    color: red
- name: Item 2
  details:
    color: blue
```

## ⚙️ Configuration Options

Customize encoding/decoding behavior with `ToonOptions`:

```java
ToonOptions options = ToonOptions.builder()
    .indent(4)                          // 4 spaces per level
    .delimiter(ToonOptions.Delimiter.TAB) // Use tabs as delimiter
    .lengthMarker(true)                 // Include length markers
    .strict(false)                      // Relaxed parsing
    .build();

String toon = Toon.encode(data, options);
JsonNode result = Toon.decode(toonString, options);
```

### Available Delimiters

- **COMMA** (`,`): Default, good balance of readability and efficiency
- **TAB** (`\t`): Excellent for tabular data, great token efficiency  
- **PIPE** (`|`): High readability, useful when data contains commas

## 📈 Token Savings Analysis

Estimate token savings compared to JSON:

```java
Map<String, Object> data = Map.of(
    "users", List.of(
        Map.of("id", 1, "name", "Alice", "role", "admin"),
        Map.of("id", 2, "name", "Bob", "role", "user")
    )
);

Toon.TokenSavings savings = Toon.estimateSavings(data);
System.out.println(savings);
// Output: JSON: 157 chars | TOON: 102 chars | Savings: 55 chars (35.0%)
```

## 🏗️ Architecture

### Core Classes

- **`Toon`**: Main facade for encoding/decoding operations
- **`ToonEncoder`**: Handles object-to-TOON serialization with format optimization
- **`ToonDecoder`**: Handles TOON-to-object deserialization with context-aware parsing
- **`ToonOptions`**: Configuration record for customizing behavior
- **`StringUtils`**: Utility methods for string handling and validation

### Key Features

✅ **Smart Format Selection**: Automatically chooses the most efficient array representation  
✅ **Type Safety**: Full support for Java generics and type conversion  
✅ **Null Safety**: Proper handling of null values and edge cases  
✅ **Validation**: Configurable strict mode for input validation  
✅ **Performance**: Optimized for both speed and memory usage  
✅ **Extensible**: Easy to extend with custom options and handlers  

## 🔧 Advanced Usage

### Custom Object Mapping

```java
public class Person {
    public String name;
    public int age;
    public List<String> hobbies;
}

// Encode custom object
Person person = new Person();
person.name = "Alice";
person.age = 30;
person.hobbies = List.of("reading", "coding", "hiking");

String toon = Toon.encode(person);
// Result: name: Alice\nage: 30\nhobbies[3]: reading,coding,hiking

// Decode back to object
Person decoded = Toon.decode(toon, Person.class);
```

### Error Handling

```java
try {
    JsonNode result = Toon.decode(invalidToonString);
} catch (ToonDecoder.ToonParseException e) {
    System.err.println("Parse error: " + e.getMessage());
} catch (RuntimeException e) {
    System.err.println("Unexpected error: " + e.getMessage());
}
```

### Working with Streams

```java
// Process multiple TOON strings
List<String> toonStrings = Arrays.asList(
    "name: Alice\nage: 30",
    "name: Bob\nage: 25"
);

List<JsonNode> results = toonStrings.stream()
    .map(Toon::decode)
    .collect(Collectors.toList());
```

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

The project includes comprehensive tests covering:
- Encoding/decoding of various data types
- Array format optimization
- Configuration options
- Edge cases and error conditions
- Performance benchmarks

## 📋 Requirements

- Java 17 or later
- Maven 3.8+ (for building)
- Jackson Databind 2.17+ (automatically included)

## 🤝 Contributing

We welcome contributions! Here's how to get started:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** with proper tests
4. **Run tests**: `mvn test`
5. **Commit your changes**: `git commit -m 'Add amazing feature'`
6. **Push to branch**: `git push origin feature/amazing-feature`
7. **Open a Pull Request**

### Development Guidelines

- Follow Java coding conventions
- Add JavaDoc for all public APIs
- Include unit tests for new features
- Update documentation as needed
- Ensure backward compatibility

## 🌐 Related Projects

This is one of many community implementations of the TOON format. For a complete list of implementations in other programming languages, see the [Community Implementations](https://github.com/toon-format/toon#community-implementations) section in the official TOON specification repository.

### Other Java Implementations
- [JToon](https://github.com/felipestanzani/JToon) by Felipe Stanzani

### Cross-Language Ecosystem
The TOON format is available in 15+ programming languages including .NET, C++, Go, Python, Rust, and more. Visit [toon-format/toon](https://github.com/toon-format/toon) for the complete ecosystem.

## 📄 License

This project is licensed under the MIT License.

---

**TOON Java SDK** - Making LLM interactions more efficient, one token at a time. 🚀
