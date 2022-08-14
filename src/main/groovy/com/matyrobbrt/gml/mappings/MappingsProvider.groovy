/*
 * MIT License
 *
 * Copyright (c) 2022 matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.matyrobbrt.gml.mappings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import cpw.mods.modlauncher.Launcher
import cpw.mods.modlauncher.api.IEnvironment
import groovy.transform.CompileStatic
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.loading.FMLLoader
import org.apache.commons.codec.binary.Hex
import org.slf4j.Logger

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.CompletableFuture
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class MappingsProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MappingsProvider INSTANCE = new MappingsProvider()

    private static final String PISTON_META = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
    private static final String MAPPINGS_THREAD = 'GML Mappings Thread'
    private static final String VERSION_JSON = 'version.json'
    private static final String OFFICIAL = 'official.txt'
    private static final String MCPCONFIG_ZIP = 'srg.zip'
    private static final String JOINED_PATH = 'config/joined.tsrg'
    private static final Gson GSON = new GsonBuilder().setLenient().excludeFieldsWithoutExposeAnnotation().create()

    final String version = FMLLoader.versionInfo().mcVersion()
    final Path gameDir = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.GAMEDIR.get()).get()
    final Path cacheDir = gameDir.resolve('mod_data/gml')
    final String mcpVersion = FMLLoader.versionInfo().mcpVersion()
    final String mcpConfigURL = "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/${version}-${mcpVersion}/mcp_config-${version}-${mcpVersion}.zip"

    final CompletableFuture<LoadedMappings> mappingsProvider = new CompletableFuture<LoadedMappings>()

    private boolean setup = false

    private MappingsProvider() {
    }

    private Thread setupMappingsThread(Runnable runnable) {
        Thread mappingsThread = new Thread(runnable, MAPPINGS_THREAD)
        mappingsThread.setUncaughtExceptionHandler { t, e ->
            LOGGER.error('Caught exception while setting up mappings...', e)
            mappingsProvider.complete(null)
        }
        return mappingsThread
    }

    synchronized void startMappingsSetup() {
        if (!setup) {
            setup = true
            final URL url = new URL(PISTON_META)
            try {
                final URLConnection connection = url.openConnection()
                connection.connect()
                LOGGER.info('Starting runtime mappings setup...')

                // Now, download and parse off-thread.
                Thread mappingsThread = setupMappingsThread {
                    try {
                        InputStream manifestInput = connection.getInputStream()
                        BufferedReader manifestReader = new BufferedReader(new InputStreamReader(manifestInput))
                        ManifestMetaFile manifestMeta = GSON.fromJson(manifestReader, ManifestMetaFile)
                        ManifestMetaFile.VersionMeta versionMeta = manifestMeta.versions.find { it.id == this.version }
                        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir)
                        LOGGER.info('Found version metadata from piston-meta.')

                        checkAndUpdateVersionFile(versionMeta)
                        LOGGER.info('version.json is up to date.')

                        checkAndUpdateOfficialFile()
                        LOGGER.info('Official mappings are up to date.')

                        checkAndUpdateMCPConfigFile()
                        LOGGER.info('MCPConfig is up to date.')

                        loadLayeredMappings()
                        LOGGER.info('Finished runtime mappings setup.')
                    } catch (IOException e) {
                        // Error state, I couldn't make mappings.
                        throw e
                    } catch (NoSuchElementException e) {
                        // Error state, not a known version? Huh?
                        throw e
                    }
                }
                mappingsThread.start()
            } catch (IOException e) {
                LOGGER.info("Couldn't connect to piston-meta. Looking for cached file instead.")
                Thread mappingsThread = setupMappingsThread {
                    loadLayeredMappings()
                    LOGGER.info('Finished runtime mappings setup.')
                }
                mappingsThread.start()
            }
        }
    }

    private void loadLayeredMappings() throws IOException {
        Path zipPath = cacheDir.resolve(MCPCONFIG_ZIP)
        Path officialPath = cacheDir.resolve(OFFICIAL)
        try (ZipFile zip = new ZipFile(zipPath.toFile())
             InputStream joined = zip.getInputStream(new ZipEntry(JOINED_PATH))
             SrgParser srgParser = new SrgParser(new BufferedReader(new InputStreamReader(joined)))
             InputStream officialStream = Files.newInputStream(officialPath)
             OfficialParser officialParser = new OfficialParser(new BufferedReader(new InputStreamReader(officialStream)))) {
            srgParser.parse()

            officialParser.parse()
            // official class name, official -> srg
            var methodsMap = new HashMap<String, Map<String, List<String>>>()
            var fieldsMap = new HashMap<String, Map<String, String>>()

            officialParser.classes.forEach { String official, String obf ->
                var methods = new HashMap<String, List<String>>()
                var fields = new HashMap<String, String>()

                var srgMethods = srgParser.methods.get(obf)
                var srgFields = srgParser.fields.get(obf)

                if (srgFields === null || srgMethods === null) return

                officialParser.fields.get(obf).forEach { fMoj, fObf ->
                    var srg = srgFields.get(fObf)
                    if (srg === null) return
                    fields.put(fMoj, srg)
                }

                officialParser.methods.get(obf).forEach { mMoj, mObf ->
                    var srg = mObf.collect { srgMethods.get(it) }
                    if (srg === null || srg.isEmpty()) return
                    methods.put(mMoj, srg)
                }

                methodsMap.put(official.replace('/', '.'), methods)
                fieldsMap.put(official.replace('/', '.'), fields)
            }

            mappingsProvider.complete(new LoadedMappings(methodsMap, fieldsMap))
        }
    }

    private void checkAndUpdateVersionFile(ManifestMetaFile.VersionMeta versionMeta) throws IOException {
        String sha1 = versionMeta.sha1

        Path versionJson = this.cacheDir.resolve(VERSION_JSON)
        if (Files.exists(versionJson)) {
            byte[] existingSha1 = calcSha1(versionJson)
            byte[] knownSha1 = Hex.decodeHex(sha1)
            if (Arrays.equals(knownSha1, existingSha1)) return
        }

        try (InputStream versionStream = new URL(versionMeta.url).openStream()) {
            Files.copy(versionStream, versionJson, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private void checkAndUpdateMCPConfigFile() throws IOException {
        Path srg = this.cacheDir.resolve(MCPCONFIG_ZIP)
        if (Files.exists(srg)) {
            return
        }

        try (InputStream officialStream = new URL(mcpConfigURL).openStream()) {
            Files.copy(officialStream, srg, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private void checkAndUpdateOfficialFile() throws IOException {
        Path versionJson = this.cacheDir.resolve(VERSION_JSON)
        VersionMetaFile versionMeta = GSON.fromJson(Files.newBufferedReader(versionJson), VersionMetaFile)
        String sha1 = switch (FMLLoader.dist) {
            case Dist.CLIENT -> versionMeta.downloads.client_mappings.sha1
            case Dist.DEDICATED_SERVER -> versionMeta.downloads.server_mappings.sha1
        }
        String url = switch (FMLLoader.dist) {
            case Dist.CLIENT -> versionMeta.downloads.client_mappings.url
            case Dist.DEDICATED_SERVER -> versionMeta.downloads.server_mappings.url
        }

        Path official = this.cacheDir.resolve(OFFICIAL)
        if (Files.exists(official)) {
            byte[] existingSha1 = calcSha1(official)
            byte[] knownSha1 = Hex.decodeHex(sha1)
            if (Arrays.equals(knownSha1, existingSha1)) return
        }

        try (InputStream officialStream = new URL(url).openStream()) {
            Files.copy(officialStream, official, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private static byte[] calcSha1(Path file) throws FileNotFoundException,
            IOException, NoSuchAlgorithmException {

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1")
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192]
            int len = input.read(buffer)

            while (len != -1) {
                sha1.update(buffer, 0, len)
                len = input.read(buffer)
            }

            return sha1.digest()
        }
    }
}
