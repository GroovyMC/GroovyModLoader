ModsDotGroovy.make {
    modLoader = 'gml'
    loaderVersion = "[${this.version},)"
    license = 'MIT'

    issueTrackerUrl = 'https://github.com/GroovyMC/GroovyModLoader/issues'

    mod {
        modId = 'gml'
        authors = ['Matyrobbrt', 'Paint_Ninja']
        displayName = 'GroovyModLoader'
        displayUrl = 'https://www.curseforge.com/minecraft/mc-mods/gml?projectId=661517'
        version = this.version
        description = 'A mod loader for Groovy mods'
        logoFile = 'gml.png'
        updateJsonUrl = 'https://maven.moddinginquisition.org/releases/org/groovymc/gml/gml/forge-promotions.json'

        dependencies {
            forge = "[${this.forgeVersion},)"
            minecraft = this.minecraftVersionRange
        }
    }
}
