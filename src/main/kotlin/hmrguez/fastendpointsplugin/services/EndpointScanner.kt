package hmrguez.fastendpointsplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import java.util.concurrent.atomic.AtomicReference

@Service(Level.PROJECT)
class EndpointScanner(private val project: Project) {
    data class Endpoint(val className: String, val filePath: String)

    // Observer used by toolwindow to receive updates when endpoints are refreshed
    var onEndpointsChanged: ((List<Endpoint>) -> Unit)? = null

    private val cache = AtomicReference<List<Endpoint>>(emptyList())

    fun current(): List<Endpoint> = cache.get()

    fun scanEndpoints(): List<Endpoint> {
        val root = project.baseDir ?: return emptyList()
        val results = mutableListOf<Endpoint>()
        val classRegex = Regex("class\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*([A-Za-z0-9_.]+)")
        val fastEndpointsNames = setOf("Endpoint", "FastEndpoints.Endpoint")

        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Unit>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension?.equals("cs", ignoreCase = true) == true) {
                    val text = try {
                        VfsUtilCore.loadText(file)
                    } catch (t: Throwable) {
                        ""
                    }
                    // naive, best-effort: find any class : Endpoint or : FastEndpoints.Endpoint
                    classRegex.findAll(text).forEach { m ->
                        val className = m.groupValues[1]
                        val base = m.groupValues[2]
                        if (fastEndpointsNames.contains(base) || base.endsWith(".Endpoint")) {
                            results.add(Endpoint(className, file.path))
                        }
                    }
                }
                return true
            }
        })

        cache.set(results)
        onEndpointsChanged?.invoke(results)
        return results
    }
}
