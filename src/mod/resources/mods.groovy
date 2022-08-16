ModsDotGroovy.make {
    modLoader = 'gml'
    loaderVersion = "[${this.version},)"
    license = 'MIT'

    issueTrackerUrl = 'https://github.com/Matyrobbrt/GroovyModLoader/issues'

    mod {
        modId = 'gml'
        author = 'Matyrobbrt'
        displayName = 'GroovyModLoader'
        version = this.version
        description = 'A mod loader for Groovy mods'

        dependencies {
            forge = "[${this.forgeVersion},)"
            minecraft = this.minecraftVersionRange
        }
    }
}