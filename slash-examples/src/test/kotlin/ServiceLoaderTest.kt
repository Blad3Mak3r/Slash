import io.github.blad3mak3r.slash.registry.CommandRegistrar
import io.github.blad3mak3r.slash.registry.DefaultPreconditionProvider
import io.github.blad3mak3r.slash.registry.HandlerRegistry
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceLoaderTest {

    @Test
    fun `ServiceLoader discovers all generated registrars`() {
        val loader = ServiceLoader.load(CommandRegistrar::class.java)
        val registrars = loader.toList()

        // One registrar per @ApplicationCommand class in slash-examples
        assertEquals(8, registrars.size, "Expected 8 registrars (one per @ApplicationCommand class)")
    }

    @Test
    fun `All registrars populate the HandlerRegistry without errors`() {
        val registry = HandlerRegistry()
        val provider = DefaultPreconditionProvider()
        val loader = ServiceLoader.load(CommandRegistrar::class.java)

        for (registrar in loader) {
            registrar.register(registry, provider)
        }

        assertTrue(registry.slashCount() > 0, "slash handlers must be registered")
        assertTrue(registry.buttonCount() > 0, "button handlers must be registered")
        assertTrue(registry.modalCount() > 0, "modal handlers must be registered")
        assertTrue(registry.autoCompleteCount() > 0, "autocomplete handlers must be registered")

        println("Registry summary: ${registry.summary()}")
        println("Slash paths: ${registry.slashPaths()}")
    }
}
