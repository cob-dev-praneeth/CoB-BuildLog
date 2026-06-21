@file:OptIn(ExperimentalMaterial3Api::class)

package org.childrenofbharat.buildlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.childrenofbharat.buildlog.data.EntryType
import org.childrenofbharat.buildlog.data.ProjectEntity
import org.childrenofbharat.buildlog.data.TimelineItem
import org.childrenofbharat.buildlog.ui.theme.Acid
import org.childrenofbharat.buildlog.ui.theme.BuildLogTheme
import org.childrenofbharat.buildlog.ui.theme.Coral
import org.childrenofbharat.buildlog.ui.theme.Ink
import org.childrenofbharat.buildlog.ui.theme.Muted
import org.childrenofbharat.buildlog.ui.theme.Sky
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Destination(val label: String, val icon: ImageVector) {
    Timeline("Timeline", Icons.Default.History),
    Projects("Projects", Icons.Default.Folder),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun BuildLogApp(
    viewModel: BuildLogViewModel,
    quickCaptureRequest: Int,
    overlayAllowed: Boolean,
    onEnableOverlay: () -> Unit
) {
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(Destination.Timeline) }
    var composerOpen by remember { mutableStateOf(quickCaptureRequest > 0) }
    LaunchedEffect(quickCaptureRequest) {
        if (quickCaptureRequest > 0) composerOpen = true
    }

    Scaffold(
        topBar = { AppHeader(destination, timeline.size) },
        bottomBar = {
            NavigationBar(containerColor = Ink) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == Destination.Timeline) {
                FloatingActionButton(
                    onClick = { composerOpen = true },
                    containerColor = Acid,
                    contentColor = Ink,
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp)
                ) { Icon(Icons.Default.Add, "Capture note", Modifier.size(32.dp)) }
            }
        }
    ) { padding ->
        when (destination) {
            Destination.Timeline -> TimelineScreen(timeline, Modifier.padding(padding))
            Destination.Projects -> ProjectsScreen(
                projects = projects,
                onAddProject = { viewModel.addProject(it) {} },
                modifier = Modifier.padding(padding)
            )
            Destination.Settings -> SettingsScreen(overlayAllowed, onEnableOverlay, Modifier.padding(padding))
        }
    }

    if (composerOpen) {
        NoteComposer(
            projects = projects,
            onDismiss = { composerOpen = false },
            onSave = { content, projectId, tags ->
                viewModel.captureNote(content, projectId, tags) { composerOpen = false }
            }
        )
    }
}

@Composable
private fun AppHeader(destination: Destination, entryCount: Int) {
    Surface(color = Ink) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CoB / BUILD LOG", color = Acid, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
                Text(destination.label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            if (destination == Destination.Timeline) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Acid, CircleShape))
                        Text("  $entryCount captured", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineScreen(timeline: List<TimelineItem>, modifier: Modifier = Modifier) {
    if (timeline.isEmpty()) {
        Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EditNote, null, Modifier.size(42.dp), tint = Acid) }
                }
                Spacer(Modifier.height(20.dp))
                Text("Begin the record", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Tap + and capture what you're building.\nNo filing ceremony required.", color = Muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        return
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Text("TODAY", color = Acid, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.4.sp)
                HorizontalDivider(Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.outline)
            }
        }
        items(timeline, key = { it.id }) { TimelineCard(it) }
    }
}

@Composable
private fun TimelineCard(item: TimelineItem) {
    val accent = when (item.type) {
        EntryType.NOTE -> Acid
        EntryType.TASK -> Coral
        EntryType.VOICE -> Sky
        else -> Muted
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatTime(item.createdAt), fontSize = 11.sp, color = Muted)
                Box(Modifier.padding(top = 8.dp).size(10.dp).background(accent, CircleShape))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.type.name, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    item.projectName?.let { Text("  /  $it", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(item.content, Modifier.padding(top = 7.dp), fontSize = 17.sp, lineHeight = 24.sp)
                if (item.tags.isNotBlank()) Text(item.tags.split(',').joinToString("  ") { "#$it" }, color = Sky, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun NoteComposer(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String?, List<String>) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        NoteComposerContent(projects = projects, onDismiss = onDismiss, onSave = onSave)
    }
}

@Composable
private fun NoteComposerContent(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String?, List<String>) -> Unit,
    initialContent: String = "",
    initialTags: String = "",
    initialProjectId: String? = null
) {
    var content by remember { mutableStateOf(initialContent) }
    var tags by remember { mutableStateOf(initialTags) }
    var selectedProject by remember { mutableStateOf(initialProjectId) }
    val focusRequester = remember { FocusRequester() }

    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("QUICK CAPTURE", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text("What's happening?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 16.dp).focusRequester(focusRequester),
                placeholder = { Text("A thought, decision, discovery…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                shape = RoundedCornerShape(16.dp)
            )
            if (projects.isNotEmpty()) {
                Text("PROJECT · OPTIONAL", Modifier.padding(top = 18.dp, bottom = 8.dp), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.take(3).forEach { project ->
                        AssistChip(
                            onClick = { selectedProject = if (selectedProject == project.id) null else project.id },
                            label = { Text(project.name) },
                            leadingIcon = { Icon(if (selectedProject == project.id) Icons.Outlined.CheckCircle else Icons.Outlined.Circle, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (optional)") },
                placeholder = { Text("research, decision") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (content.isNotBlank()) onSave(content, selectedProject, tags.split(',')) })
            )
            Button(
                onClick = { onSave(content, selectedProject, tags.split(',')) },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Bolt, null)
                Text("  CAPTURE NOW", fontWeight = FontWeight.Black)
            }
            Text("Saved locally · Timestamped · Never overwritten", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
    }
}

@Composable
private fun ProjectsScreen(
    projects: List<ProjectEntity>,
    onAddProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Connect captures to a body of work. Association stays optional at capture time.", color = Muted, lineHeight = 21.sp)
            OutlinedButton(onClick = { adding = !adding }, Modifier.padding(top = 16.dp)) { Icon(Icons.Default.Add, null); Text("  New project") }
            if (adding) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    trailingIcon = {
                        TextButton(enabled = name.isNotBlank(), onClick = {
                            onAddProject(name)
                            name = ""
                            adding = false
                        }) { Text("ADD") }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        if (projects.isEmpty()) item { EmptyProjectCard() }
        items(projects, key = { it.id }) { project ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Acid, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(project.name.take(1).uppercase(), color = Ink, fontWeight = FontWeight.Black) }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(project.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ready for captures", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(20.dp)) {
            Icon(Icons.Default.Folder, null, tint = Sky)
            Text("No projects yet", Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold)
            Text("Your notes still work perfectly without one.", color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SettingsScreen(overlayAllowed: Boolean, onEnableOverlay: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Acid, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Ink) } }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Floating capture", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text(if (overlayAllowed) "Permission ready" else "Permission required", color = if (overlayAllowed) Acid else Coral, fontSize = 12.sp)
                        }
                    }
                    Text("Keep a draggable capture button above other apps. Tapping it opens a fresh note instantly.", color = Muted, modifier = Modifier.padding(vertical = 16.dp), lineHeight = 21.sp)
                    Button(onClick = onEnableOverlay, modifier = Modifier.fillMaxWidth()) { Text(if (overlayAllowed) "START OVERLAY" else "ALLOW & START") }
                }
            }
        }
        item { InfoRow(Icons.Default.Bolt, "Local first", "All Phase 1 data stays on this device.") }
        item { InfoRow(Icons.Default.Search, "Search", "Planned for Phase 2; the data model is ready.") }
        item { InfoRow(Icons.Outlined.KeyboardVoice, "Voice capture", "Coming in Phase 2.") }
        item { InfoRow(Icons.Outlined.PhotoCamera, "Visual capture", "Coming in Phase 3.") }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Sky)
        Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = Muted, fontSize = 13.sp) }
    }
}

private fun formatTime(epochMillis: Long): String = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))

// Preview data is intentionally realistic enough to exercise long copy, tags,
// project associations, and the different visual accents used by the timeline.
private val previewProjects = listOf(
    ProjectEntity(
        id = "cob-platform",
        name = "CoB Platform",
        description = "Core contributor experience",
        colorHex = "#E9FF70"
    ),
    ProjectEntity(
        id = "build-log",
        name = "Build Log",
        description = "Local-first continuity capture",
        colorHex = "#8DD8FF"
    ),
    ProjectEntity(
        id = "bharat-archive",
        name = "Bharat Archive",
        description = "Open knowledge preservation",
        colorHex = "#FF7D66"
    )
)

private val previewTimeline = listOf(
    TimelineItem(
        id = "entry-1",
        createdAt = Instant.parse("2026-06-20T12:12:00Z").toEpochMilli(),
        type = EntryType.NOTE,
        content = "Mapped the three-second capture flow and removed project selection from the critical path.",
        tags = "ux,decision",
        projectName = "Build Log",
        projectColor = "#8DD8FF"
    ),
    TimelineItem(
        id = "entry-2",
        createdAt = Instant.parse("2026-06-20T10:45:00Z").toEpochMilli(),
        type = EntryType.TASK,
        content = "Test the floating overlay on a Samsung device before the contributor pilot.",
        tags = "android,pilot",
        projectName = "Build Log",
        projectColor = "#8DD8FF"
    ),
    TimelineItem(
        id = "entry-3",
        createdAt = Instant.parse("2026-06-20T09:20:00Z").toEpochMilli(),
        type = EntryType.NOTE,
        content = "Project histories should be assembled from immutable contributor events, not rewritten status updates.",
        tags = "architecture,history",
        projectName = "CoB Platform",
        projectColor = "#E9FF70"
    ),
    TimelineItem(
        id = "entry-4",
        createdAt = Instant.parse("2026-06-20T07:55:00Z").toEpochMilli(),
        type = EntryType.VOICE,
        content = "Voice memo: explore a daily report that keeps decisions separate from open questions.",
        tags = "reports,idea",
        projectName = null,
        projectColor = null
    )
)

@Composable
private fun PreviewAppFrame(
    destination: Destination,
    entryCount: Int = previewTimeline.size,
    content: @Composable (PaddingValues) -> Unit
) {
    BuildLogTheme {
        Scaffold(
            topBar = { AppHeader(destination, entryCount) },
            bottomBar = {
                NavigationBar(containerColor = Ink) {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = {},
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (destination == Destination.Timeline) {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = Acid,
                        contentColor = Ink,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) { Icon(Icons.Default.Add, "Capture note", Modifier.size(32.dp)) }
                }
            },
            content = content
        )
    }
}

@Preview(
    name = "01 · Timeline",
    group = "CoB Build Log · Mock Screens",
    device = Devices.PIXEL_7,
    showSystemUi = true
)
@Composable
private fun TimelineMockScreenshot() {
    PreviewAppFrame(Destination.Timeline) { padding ->
        TimelineScreen(previewTimeline, Modifier.padding(padding))
    }
}

@Preview(
    name = "02 · Quick Capture",
    group = "CoB Build Log · Mock Screens",
    device = Devices.PIXEL_7,
    showSystemUi = true
)
@Composable
private fun QuickCaptureMockScreenshot() {
    BuildLogTheme {
        Box(Modifier.fillMaxSize().background(Ink)) {
            Column(Modifier.fillMaxSize().alpha(0.28f)) {
                AppHeader(Destination.Timeline, previewTimeline.size)
                TimelineScreen(previewTimeline.take(2), Modifier.weight(1f))
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 18.dp
            ) {
                Column {
                    Box(
                        Modifier.padding(top = 10.dp).size(width = 42.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outline, CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )
                    NoteComposerContent(
                        projects = previewProjects,
                        onDismiss = {},
                        onSave = { _, _, _ -> },
                        initialContent = "The daily report should surface decisions with links back to their original captures.",
                        initialTags = "reports, decision",
                        initialProjectId = "build-log"
                    )
                }
            }
        }
    }
}

@Preview(
    name = "03 · Projects",
    group = "CoB Build Log · Mock Screens",
    device = Devices.PIXEL_7,
    showSystemUi = true
)
@Composable
private fun ProjectsMockScreenshot() {
    PreviewAppFrame(Destination.Projects) { padding ->
        ProjectsScreen(
            projects = previewProjects,
            onAddProject = {},
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview(
    name = "04 · Settings",
    group = "CoB Build Log · Mock Screens",
    device = Devices.PIXEL_7,
    showSystemUi = true
)
@Composable
private fun SettingsMockScreenshot() {
    PreviewAppFrame(Destination.Settings) { padding ->
        SettingsScreen(
            overlayAllowed = true,
            onEnableOverlay = {},
            modifier = Modifier.padding(padding)
        )
    }
}
