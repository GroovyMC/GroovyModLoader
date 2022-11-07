ModsDotGroovy.make {
    modLoader = 'gml'
    loaderVersion = "[${this.version},)"
    license = 'MIT'

    issueTrackerUrl = 'https://github.com/Matyrobbrt/GroovyModLoader/issues'

    mod {
        modId = 'gml'
        authors = ['Matyrobbrt', 'Paint_Ninja']
        displayName = 'GroovyModLoader'
        version = this.version
        description = 'A mod loader for Groovy mods'
        logoFile = 'gml.png'

        dependencies {
            forge = "[${this.forgeVersion},)"
            minecraft = this.minecraftVersionRange
        }
    }
}