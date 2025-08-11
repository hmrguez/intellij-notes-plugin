package hmrguez.fastendpointsplugin.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import hmrguez.fastendpointsplugin.services.EndpointScanner

class ProjectInitializer : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val scanner = project.service<EndpointScanner>()
        // Initial scan when project opens
        scanner.scanEndpoints()
        // Listen to project build finished events to rescan
        project.messageBus.connect().subscribe(ProjectTaskListener.TOPIC, object : ProjectTaskListener {
            override fun finished(result: ProjectTaskManager.Result) {
                // After any build, rescan
                scanner.scanEndpoints()
            }
        })
    }
}
