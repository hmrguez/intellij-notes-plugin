package hmrguez.notesplugin.notes

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class TagEditor(
    private val availableTags: MutableList<String>,
    private val selectedTags: MutableSet<String> = mutableSetOf(),
    private val onSelectionChanged: (Set<String>) -> Unit = {},
    private val onNewTagCreated: (String) -> Unit = {}
) : JPanel(BorderLayout()) {

    private val chipsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
    private val newTagField = com.intellij.ui.components.JBTextField()
    private val addTagButton = JButton("Add Tag")

    init {
        background = com.intellij.util.ui.UIUtil.getPanelBackground()

        // Setup chips panel
        chipsPanel.background = com.intellij.util.ui.UIUtil.getPanelBackground()
        chipsPanel.border = BorderFactory.createTitledBorder("Select Tags")

        // Setup new tag input panel
        val inputPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        inputPanel.background = com.intellij.util.ui.UIUtil.getPanelBackground()
        inputPanel.border = BorderFactory.createTitledBorder("Create New Tag")

        newTagField.columns = 15
        newTagField.emptyText.text = "Enter new tag name..."

        addTagButton.addActionListener {
            val newTag = newTagField.text.trim().lowercase()
            if (newTag.isNotEmpty() && !availableTags.contains(newTag)) {
                availableTags.add(newTag)
                selectedTags.add(newTag)
                newTagField.text = ""
                onNewTagCreated(newTag)
                refreshChips()
            }
        }

        // Add Enter key support to text field
        newTagField.addActionListener {
            addTagButton.doClick()
        }

        inputPanel.add(JLabel("New tag:"))
        inputPanel.add(newTagField)
        inputPanel.add(addTagButton)

        // Layout the components
        add(chipsPanel, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)

        refreshChips()
    }

    fun setSelectedTags(tags: Set<String>) {
        selectedTags.clear()
        selectedTags.addAll(tags.filter { availableTags.contains(it.lowercase()) })
        refreshChips()
    }

    fun getSelectedTags(): Set<String> = selectedTags.toSet()

    private fun refreshChips() {
        chipsPanel.removeAll()

        availableTags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            val chip = createSelectableChip(tag, isSelected) { selected ->
                if (selected) {
                    selectedTags.add(tag)
                } else {
                    selectedTags.remove(tag)
                }
                refreshChips()
                onSelectionChanged(selectedTags.toSet())
            }
            chipsPanel.add(chip)
        }

        chipsPanel.revalidate()
        chipsPanel.repaint()
    }

    private fun createSelectableChip(tag: String, isSelected: Boolean, onToggle: (Boolean) -> Unit): JComponent {
        return object : JComponent() {
            private var isHovered = false

            init {
                // Ensure we have a font before getting metrics
                if (font == null) {
                    font = com.intellij.util.ui.UIUtil.getLabelFont()
                }
                val metrics = getFontMetrics(font)
                val width = metrics.stringWidth(tag) + 20
                val height = 24
                preferredSize = Dimension(width, height)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        onToggle(!isSelected)
                    }

                    override fun mouseEntered(e: MouseEvent) {
                        isHovered = true
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        isHovered = false
                        repaint()
                    }
                })
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val width = width
                val height = height

                // Get tag color (similar to NoteCellPanel)
                val baseColor = getTagColor(tag)

                // Determine background color based on selection and hover state
                val backgroundColor = when {
                    isSelected && isHovered -> baseColor.darker()
                    isSelected -> baseColor
                    isHovered -> baseColor.brighter().brighter()
                    else -> JBColor.LIGHT_GRAY
                }

                // Draw chip background
                g2d.color = backgroundColor
                g2d.fillRoundRect(0, 0, width, height, 12, 12)

                // Draw border
                g2d.color = if (isSelected) baseColor.darker() else JBColor.GRAY
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

                // Draw text
                val metrics = g2d.fontMetrics
                val textColor = if (isSelected) JBColor.WHITE else JBColor.BLACK
                g2d.color = textColor
                val x = (width - metrics.stringWidth(tag)) / 2
                val y = (height - metrics.height) / 2 + metrics.ascent
                g2d.drawString(tag, x, y)

                // Draw selection indicator (checkmark or plus)
                if (isSelected) {
                    g2d.color = JBColor.WHITE
                    val checkSize = 8
                    val checkX = width - checkSize - 3
                    val checkY = 3
                    // Simple checkmark
                    g2d.drawLine(checkX, checkY + checkSize/2, checkX + checkSize/3, checkY + 2*checkSize/3)
                    g2d.drawLine(checkX + checkSize/3, checkY + 2*checkSize/3, checkX + checkSize, checkY)
                } else if (isHovered) {
                    g2d.color = JBColor.GRAY
                    val plusSize = 6
                    val plusX = width - plusSize - 4
                    val plusY = height/2 - plusSize/2
                    // Simple plus sign
                    g2d.drawLine(plusX + plusSize/2, plusY, plusX + plusSize/2, plusY + plusSize)
                    g2d.drawLine(plusX, plusY + plusSize/2, plusX + plusSize, plusY + plusSize/2)
                }
            }
        }
    }

    private fun getTagColor(tag: String): Color {
        return when (tag.lowercase()) {
            "todo" -> JBColor(Color(0x4CAF50), Color(0x6BC86B))      // Green (light/dark theme)
            "mental" -> JBColor(Color(0x2196F3), Color(0x42A5F5))    // Blue (light/dark theme)
            "giberish" -> JBColor(Color(0xFF9800), Color(0xFFB74D))  // Orange (light/dark theme)
            else -> JBColor.GRAY
        }
    }
}
