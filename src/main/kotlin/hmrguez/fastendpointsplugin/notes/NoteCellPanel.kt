package hmrguez.fastendpointsplugin.notes

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class NoteCellPanel(
    private val note: NotesService.Note,
    private val isSelected: Boolean,
    private val hasFocus: Boolean
) : JPanel() {

    init {
        layout = BorderLayout()
        isOpaque = true
        background = if (isSelected) UIManager.getColor("List.selectionBackground") else UIManager.getColor("List.background")

        val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        contentPanel.isOpaque = false

        // Note content
        val firstLine = note.content.lines().firstOrNull()?.take(80) ?: "(empty)"
        val contentLabel = JLabel(firstLine)
        contentLabel.font = contentLabel.font.deriveFont(Font.BOLD)
        contentLabel.foreground = if (isSelected) UIManager.getColor("List.selectionForeground") else UIManager.getColor("List.foreground")
        contentPanel.add(contentLabel)

        // Add tags directly to the same panel
        if (note.tags.isNotEmpty()) {
            val order = NotesService.AVAILABLE_TAGS.withIndex().associate { it.value to it.index }
            val sortedTags = note.tags.sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))

            sortedTags.forEach { tag ->
                contentPanel.add(createChip(tag))
            }
        }

        add(contentPanel, BorderLayout.WEST)
        border = if (hasFocus) UIManager.getBorder("List.focusCellHighlightBorder") else EmptyBorder(2, 5, 2, 5)
    }

    private fun createTagsPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        panel.isOpaque = false

        val order = NotesService.AVAILABLE_TAGS.withIndex().associate { it.value to it.index }
        val sortedTags = note.tags.sortedWith(compareBy({ order[it] ?: Int.MAX_VALUE }, { it }))

        sortedTags.forEach { tag ->
            panel.add(createChip(tag))
        }

        return panel
    }

    private fun createChip(tag: String): JComponent {
        return object : JComponent() {
            override fun getPreferredSize(): Dimension {
                val metrics = getFontMetrics(font ?: UIManager.getFont("Label.font"))
                val width = metrics.stringWidth(tag) + 16
                val height = metrics.height + 4
                return Dimension(width, height)
            }

            override fun getMinimumSize(): Dimension = preferredSize
            override fun getMaximumSize(): Dimension = preferredSize

            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val color = getTagColor(tag)

                // Draw chip background
                g2d.color = color
                g2d.fillRoundRect(0, 0, width, height, height, height)

                // Draw text
                g2d.color = Color.WHITE
                val metrics = g2d.fontMetrics
                val x = (width - metrics.stringWidth(tag)) / 2
                val y = (height + metrics.ascent - metrics.descent) / 2
                g2d.drawString(tag, x, y)
            }
        }
    }

    private fun getTagColor(tag: String): Color {
        return when (tag.lowercase()) {
            "todo" -> Color(9, 105, 218)     // #0969da
            "mental" -> Color(130, 80, 223)   // #8250df
            "giberish" -> Color(154, 103, 0)  // #9a6700
            else -> Color(87, 96, 106)        // #57606a
        }
    }
}