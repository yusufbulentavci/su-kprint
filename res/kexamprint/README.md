# Exam Print Resources

## Directory Structure

```
kexamprint/
├── config.properties          - Main configuration
├── text/
│   ├── labels_uz.properties  - Uzbek language labels
│   └── labels_en.properties  - English language labels
├── fonts/                    - Custom fonts (future)
└── images/                   - Logos, icons (future)
```

## Configuration Files

### config.properties
Main application configuration including:
- Output directory paths
- Exam images directory
- Default exam names
- PDF settings

### text/labels_*.properties
Localized text resources for:
- Exam paper labels
- Signature form labels
- Day names
- Common text elements

## Usage

Resources are loaded via `ResourceLoader` and `TextResources` utility classes:

```java
// Load configuration
String outputDir = ResourceLoader.getOutputBaseDir();
String examName = ResourceLoader.getDefaultExamName();

// Load text resources
String evaluatorLabel = TextResources.getExamEvaluatorLabel();
String roomLabel = TextResources.getRoomLabel();

// Change language
TextResources.setLanguage("en");
```

## Adding New Resources

1. **Configuration**: Add to `config.properties`
2. **Text Labels**: Add to both `labels_uz.properties` and `labels_en.properties`
3. **Fonts**: Place in `fonts/` directory
4. **Images**: Place in `images/` directory

All resources are loaded from classpath at runtime.
