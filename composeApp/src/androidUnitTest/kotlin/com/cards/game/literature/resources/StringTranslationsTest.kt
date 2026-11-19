package com.cards.game.literature.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every language the app ships must say everything English says, with the same placeholders.
 * Compose resources fall back to English silently for a key a translation lacks, so a string
 * added to values/ without its Tamil and Hindi lines would ship half-translated unnoticed.
 */
class StringTranslationsTest {

    private val placeholder = Regex("""%\d+\$[sdf]|%[sd]""")

    private fun strings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val n = nodes.item(i)
            n.attributes.getNamedItem("name").nodeValue to (n.textContent ?: "")
        }
    }

    private fun check(base: File, translated: File) {
        assertTrue(translated.isFile, "${translated.path} is missing")
        val en = strings(base)
        val tr = strings(translated)
        assertEquals(emptySet(), en.keys - tr.keys, "${translated.name} (${translated.parentFile.name}) lacks these English keys")
        assertEquals(emptySet(), tr.keys - en.keys, "${translated.parentFile.name} has keys English does not")
        en.forEach { (key, text) ->
            assertEquals(
                placeholder.findAll(text).map { it.value }.sorted().toList(),
                placeholder.findAll(tr.getValue(key)).map { it.value }.sorted().toList(),
                "${translated.parentFile.name}/$key: placeholders differ from English",
            )
        }
        assertTrue(!translated.readText().contains("\\'"), "${translated.parentFile.name}: compose-resources renders \\' literally; use a plain apostrophe")
    }

    // Gradle runs unit tests with the module directory as the working directory.
    private val module = File(System.getProperty("user.dir"))

    @Test
    fun everyShippedLanguageCoversTheAppStrings() {
        val res = module.resolve("src/commonMain/composeResources")
        listOf("ta", "hi").forEach { check(res.resolve("values/strings.xml"), res.resolve("values-$it/strings.xml")) }
    }

    @Test
    fun everyShippedLanguageCoversTheAndroidStrings() {
        val res = module.resolve("src/androidMain/res")
        listOf("ta", "hi").forEach { check(res.resolve("values/strings.xml"), res.resolve("values-$it/strings.xml")) }
    }
}
