${header}
[![Latest GML version](https://img.shields.io/badge/dynamic/xml?style=for-the-badge&color=red&label=Latest%20GML%20Version&prefix=v&query=metadata//latest&url=https://maven.moddinginquisition.org/releases/org/groovymc/gml/gml/maven-metadata.xml)](https://maven.moddinginquisition.org/#/releases/org/groovymc/gml/gml)
# GroovyModLoader
GroovyModLoader (GML) is a Forge language loader for Groovy mods.
## How to use
To use this language provider, simply add it as a dependency in your build script. The
language provider is available on ModdingInquisition's maven, so you can simply add the following
to your build script. Make sure to define `gmlVersion` in the `gradle.properties` file, as the version of GML you want to use.
```groovy
repositories {
    maven {
        name = 'Modding Inquisition Releases'
        url = 'https://maven.moddinginquisition.org/releases'
    }
}
dependencies {
    compileOnly "org.groovymc.gml:gml:${gmlVersion}"
    runtimeOnly fg.deobf("org.groovymc.gml:gml:${gmlVersion}:all") {
        exclude module: 'transform'
    }
}
```

Also make sure to add the `groovy` plugin to your build script to ensure that the required
tasks for Groovy compilation are added:
```groovy
plugins {
    id 'groovy'
}
```
In your `mods.toml` / [mods.groovy](https://github.com/GroovyMC/ModsDotGroovy), specify `gml` as the loader and the version of the software
as `loaderVersion`.

The main mod class should have a no-arg constructor and be annotated with `@GMod` with the mod ID specified. Access to the mod and Forge
buses is provided respectively through the `modBus` or `forgeBus` properties.
If you want IDE support alongside `@CompileStatic` for those properties, consider implementing `BaseGMod` in your mod main class or install the [EnhancedGroovy plugin for IntelliJ](https://plugins.jetbrains.com/plugin/19844-enhancedgroovy).

```groovy
@GMod('examplemod')
@Slf4j(category = 'ExampleMod')
class ExampleMod implements BaseGMod {
    ExampleMod() {
        log.info('Hello from Groovy-land!')
        
        modBus.register(ModBusEventHandler)
        forgeBus.register(ForgeBusEventHandler)
        
        assert forgeBus === MinecraftForge.EVENT_BUS
        assert modBus === GMLModLoadingContext.get().getModEventBus()
    }
}
```

## Groovy Version
The Groovy version provided by the latest version of GML is currently: **${groovyVersion}**.  
To find out what Groovy version a built GML jar provides, you can read the `BundledGroovyVersion` property in the `META-INF/MANIFEST.MF`.

Only releases will be supported by GML, not release candidates.
Once a Forge version has a Recommended Build published, the Groovy major and minor versions will be locked for that Minecraft version. However, patch versions will still be updated.

### Groovy modules
The Groovy modules provided by GML are: ${groovyLibs}.  
You may however include a Groovy module that is **not** provided by GML using [JiJ](https://forge.gemwire.uk/wiki/Jar-in-Jar).  
Please **do not** JiJ different versions of Groovy modules already bundled by GML as that may cause issues.

## MC Versions
The major GML version is bumped every MC version. Below you can find a list of MC version -> GML version:
- `1.19.2` -> `1.x.x`
- `1.19.3` -> `2.x.x`
- `1.19.4` -> `3.x.x`
- `1.20.0` -> `4.x.x`

**NOTE**: 1.19.2 - 1.19.4 versions use the `com.matyrobbrt.gml` artifact group.

## Shading GML
While we would appreciate if you instead had a dependency on GML on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/gml) in order provide us revenue, we understand that in some cases, you may not
want to depend on another mod. Fortunately, we _are_ JiJ-able.  
If you want to JiJ GML, just follow the guide [here](https://forge.gemwire.uk/wiki/Jar-in-Jar) but replace GSON with the `all` version of GML (the `runtimeOnly` one).

## Contributing
We welcome contributions to GML. If you want to contribute, please open a PR with your changes.

### Troubleshooting

#### Unable to build
Make sure to include the Git tags when forking, as they are used to determine the version of GML and the build will fail if they are not present:
```shell
git fetch --tags upstream
git push --tags
```

#### GML not showing up in-game when running in IDE
Developing GML requires you to use the Gradle run tasks to test your changes rather than the IDE run configurations, for example, use `./gradlew runClient` instead of the IntelliJ `runClient` run configuration.

#### Updating the README
Remember to run the `makeReadme` task when updating the README template or Groovy version, otherwise the README will be outdated.
