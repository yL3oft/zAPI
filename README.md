## About
zAPI is a Java API used to manage common plugin tasks for Paper-based Minecraft servers. It collects utilities, managers and integrations so plugin authors don't need to reimplement everyday boilerplate.

Documentation: https://docs.yleoft.me/zAPI  
Releases & artifacts: https://github.com/yL3oft/zAPI/releases

## Features
- Safe file management — robust creation, loading and automatic recovery for corrupted config/data files
- Language & localization — load and manage language YAMLs for simple i18n and message handling
- Custom menus — build interactive inventories with flexible item options and actions
- Inventory safety & NBT integration — listeners and protections for custom inventory behaviors with NBT support
- Cross-platform scheduling — unified task scheduling that works on both Folia and classic Bukkit
- Location serialization — utilities to serialize and deserialize Bukkit Location objects reliably
- Time & string utilities — advanced parsing and formatting helpers, including millisecond-aware time parsing
- Placeholder handling — safe placeholder application with support for offline players
- Player utilities — common player-focused helpers designed with Folia compatibility in mind
- Lightweight NBT helpers — small integration points to work with item NBT when required
- And more...

## Requirements
- Java 17+
- Maven or Gradle build system
- Internet access for dependency resolution
- (Optional) Git for cloning the repository
- (Recommended) IDE such as IntelliJ IDEA or Eclipse for development

## Installation (Repository)
Add the CodeMC repository to your build system:
```xml
<repositories>
    <repository>
        <id>yl3oft-repo</id>
        <url>https://repo.codemc.io/repository/yl3oft/</url>
    </repository>
</repositories>
```

## Dependency (use latest release)
- Latest release tag: 2.0.0

Maven
```xml
<dependencies>
    <dependency>
        <groupId>me.yleoft</groupId>
        <artifactId>zAPI</artifactId>
        <version>2.0.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://repo.codemc.io/repository/yl3oft/")
}

dependencies {
    implementation("me.yleoft:zAPI:2.0.0")
}
```

## Shading (recommended to avoid dependency collisions)
Maven (maven-shade-plugin snippet)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.3</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>me.yleoft.zAPI</pattern>
                        <shadedPattern>your.package.shaded.zAPI</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Gradle (shadow plugin snippet)
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

shadowJar {
    relocate("me.yleoft.zAPI", "your.package.shaded.zAPI")
}
```

## Build
Simply run:
```bash
mvn clean install
```
This is the only required build command for producing the library artifacts locally and ensuring the project compiles with the configured CI settings.

## Documentation & Support
- Full API docs: https://docs.yleoft.me/zAPI
- Releases & changelog: https://github.com/yL3oft/zAPI/releases
- Issues & feature requests: open an issue on the GitHub repo

## License
MIT — see LICENSE