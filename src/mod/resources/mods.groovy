ModsDotGroovy.make {
    modLoader = 'gml'
    loaderVersion = "[${this.version},)"
    license = 'MIT'

    issueTrackerUrl = 'https://github.com/Matyrobbrt/GroovyModLoader/issues'

    mod {
        modId = 'gml'
        authors = ['Matyrobbrt', 'Paint_Ninja']
        displayName = 'GroovyModLoader'
        displayUrl = 'https://www.curseforge.com/minecraft/mc-mods/gml?projectId=661517'
        version = this.version
        description = 'A mod loader for Groovy mods'
        logoFile = 'gml.png'

        dependencies {
            forge = "[${this.forgeVersion},)"
            minecraft = '[1.19.3]'
        }
    }
}
