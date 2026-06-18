package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun convertLineEndings() {
    val file = File("/app/src/main/java/com/example/ui/screens/ProductsScreen.kt")
    if (file.exists()) {
        val content = file.readText()
        val normalized = content.replace("\r\n", "\n")
        file.writeText(normalized)
        println("SUCCESSFULLY NORMALIZED LINE ENDINGS")
    } else {
        println("FILE NOT FOUND")
    }
  }

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }
}
