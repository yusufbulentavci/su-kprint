# Resources Directory

This directory contains application resources that are loaded from the classpath.

## Structure

```
res/
└── kexamprint/
    ├── config.properties    - Main configuration file
    ├── fonts/              - Custom fonts (if needed)
    ├── images/             - Static images (logos, icons)
    └── templates/          - Document templates (if needed)
```

## Configuration

Edit `kexamprint/config.properties` to configure:
- Output directories
- Exam image directory (where question images are stored)
- Default exam names
- PDF settings

## Important

**Exam question images** should NOT be stored in this directory.
They should be stored in a separate directory configured via `exam.images.dir` property.

This directory is for application resources only (configs, fonts, logos, etc.)
