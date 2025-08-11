package hmrguez.fastendpointsplugin.actions

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager

class NewThingAction : AnAction("New Thing"), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        val directory = ideView?.orChooseDirectory ?: return

        val name = Messages.showInputDialog(
            project,
            "Enter class name:",
            "New Thing",
            Messages.getQuestionIcon(),
            "",
            ClassNameValidator()
        ) ?: return

        val fileName = "$name.cs"
        val psiDir: PsiDirectory = directory
        val existing = psiDir.findFile(fileName)
        if (existing != null) {
            Messages.showErrorDialog(project, "File '$fileName' already exists in the selected directory.", "Cannot Create File")
            return
        }

        val classContent = buildString {
            appendLine("public class $name")
            appendLine("{")
            appendLine("}")
        }

        WriteCommandAction.runWriteCommandAction(project, "Create New Thing", null, Runnable {
            val vDir = psiDir.virtualFile
            val vFile = vDir.createChildData(this, fileName)
            VfsUtil.saveText(vFile, classContent)
            // Select the created file in the project view
            val psiFile = PsiManager.getInstance(project).findFile(vFile)
            if (psiFile != null) {
                ideView?.selectElement(psiFile)
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val ideView = e.getData(LangDataKeys.IDE_VIEW)
        val hasDirectory = ideView?.directories?.isNotEmpty() == true
        e.presentation.isEnabledAndVisible = project != null && hasDirectory
    }

    private class ClassNameValidator : InputValidatorEx {
        private val regex = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
        override fun getErrorText(inputString: String?): String? {
            val s = inputString ?: return "Name is required"
            return if (s.isBlank()) "Name is required" else if (!regex.matches(s)) "Only letters, digits and underscore. Must not start with a digit." else null
        }
        override fun checkInput(inputString: String?): Boolean = getErrorText(inputString) == null
        override fun canClose(inputString: String?): Boolean = checkInput(inputString)
    }
}