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
    data class Endpoint(
        val method: String,
        val path: String,
        val className: String,
        val filePath: String,
        val line: Int
    )

    // Observer used by toolwindow to receive updates when endpoints are refreshed
    var onEndpointsChanged: ((List<Endpoint>) -> Unit)? = null

    private val cache = AtomicReference<List<Endpoint>>(emptyList())

    fun current(): List<Endpoint> = cache.get()

    fun scanEndpoints(): List<Endpoint> {
        val root = project.baseDir ?: return emptyList()
        val results = mutableListOf<Endpoint>()

        // Find classes inheriting Endpoint
        val classRegex = Regex("class\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*([A-Za-z0-9_.<> ,]+)")
        val fastEndpointsNames = setOf("Endpoint", "FastEndpoints.Endpoint")

        // Detect FastEndpoints route definitions
        val simpleVerb = Regex("\\b(Get|Post|Put|Delete|Patch|Head|Options)\\s*\\(\\s*\"([^\"]+)\"")
        val verbsCall = Regex("\\bVerbs\\s*\\(\\s*Http\\.\\s*(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s*,\\s*\"([^\"]+)\"")

        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Unit>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension?.equals("cs", ignoreCase = true) == true) {
                    val text = try {
                        VfsUtilCore.loadText(file)
                    } catch (t: Throwable) {
                        ""
                    }

                    if (text.isBlank()) return true

                    // Map of class name to potential endpoints found in file
                    val classes = classRegex.findAll(text).map { it }.toList()

                    // Collect matches for methods/paths
                    val matches = mutableListOf<Triple<String, String, Int>>() // method, path, startIndex
                    simpleVerb.findAll(text).forEach { m ->
                        val method = m.groupValues[1].uppercase()
                        val path = m.groupValues[2]
                        matches.add(Triple(method, path, m.range.first))
                    }
                    verbsCall.findAll(text).forEach { m ->
                        val method = m.groupValues[1].uppercase()
                        val path = m.groupValues[2]
                        matches.add(Triple(method, path, m.range.first))
                    }

                    if (matches.isEmpty()) {
                        // As a fallback, still list endpoint classes without method/path
                        classes.forEach { m ->
                            val className = m.groupValues[1]
                            val base = m.groupValues[2]
                            if (fastEndpointsNames.any { base.endsWith(it) } || base.endsWith(".Endpoint") || base.contains("Endpoint")) {
                                val line = computeLine(text, m.range.first)
                                results.add(Endpoint(method = "?", path = "", className = className, filePath = file.path, line = line))
                            }
                        }
                    } else {
                        // Try to associate each match to the nearest preceding endpoint class
                        for ((method, path, idx) in matches) {
                            val owningClass = classes.lastOrNull { it.range.first <= idx }
                            val className = if (owningClass != null) owningClass.groupValues[1] else file.nameWithoutExtension
                            val base = owningClass?.groupValues?.getOrNull(2) ?: ""
                            if (owningClass == null || fastEndpointsNames.any { base.endsWith(it) } || base.endsWith(".Endpoint") || base.contains("Endpoint")) {
                                val line = computeLine(text, idx)
                                results.add(Endpoint(method = method, path = path, className = className, filePath = file.path, line = line))
                            }
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

    private fun computeLine(text: String, index: Int): Int {
        if (index <= 0) return 0
        var lines = 0
        var i = 0
        while (i < index && i < text.length) {
            if (text[i] == '\n') lines++
            i++
        }
        return lines
    }
}
