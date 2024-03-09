NeoForgeModsDotGroovy.make {
    modLoader = 'gml'
    loaderVersion = "[${buildProperties.gml_core_version},)"
    license = 'MIT'

    issueTrackerUrl = 'https://github.com/GroovyMC/GroovyModLoader/issues'

    mod {
        modId = 'gml'
        authors = ['Luke Bemish', 'Matyrobbrt', 'Paint_Ninja']
        displayName = 'GroovyModLoader'
        displayUrl = 'https://www.curseforge.com/minecraft/mc-mods/gml?projectId=661517'
        version = environmentInfo.version
        description = 'A mod loader for Groovy mods'
        logoFile = 'gml.png'

        dependencies {
            neoforge = "[${environmentInfo.platformVersion},)"
            minecraft = environmentInfo.minecraftVersionRange
            commongroovylibrary = "[${buildProperties.cgl_version},)"
        }
    }
}
