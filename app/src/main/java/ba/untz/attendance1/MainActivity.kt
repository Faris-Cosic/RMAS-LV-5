package ba.untz.attendance1

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ba.untz.attendance1.CheckInStatus.Absent
import ba.untz.attendance1.ui.theme.Attendance1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Attendance1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProfessorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Composable
fun StudentName(name: String) {
    Text(
        text = "Student: $name",
    )
}

@Composable
fun CheckInCard(
    checkIn: CheckIn,
    allCheckIns : List<CheckIn>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.clickable { expanded = !expanded }) {
        CheckInRow(checkIn)
        if (expanded) {
            val stud = allCheckIns.filter { it.student == checkIn.student }

            Column(modifier = Modifier.padding(start = 64.dp, top = 8.dp)) {
                stud.forEach {
                    Text(text = "${it.window.label}: ${it.status.label()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AttendanceSummary(
    CheckIns: List<CheckIn>,
    modifier: Modifier = Modifier,
) {
    val onTime = CheckIns.count { it.status is CheckInStatus.OnTime }
    val late = CheckIns.count { it.status is CheckInStatus.Late }
    val absent = CheckIns.count { it.status is Absent }

    Row {
        Text(text = "On time: $onTime · Late: $late · Absent: $absent")
    }
}
// Ako stavimo window.Middle dobit cemo manji broj onTime, late i Absent jer se proslijedjuju samo podaci
// sa Window.middle

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
        Text(text = initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusBadge(
    status: CheckInStatus,
    modifier: Modifier = Modifier,
) {
    val badgeBackground =
        when (status) {
            is CheckInStatus.Late -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    Text(
        text = status.label(),
        color = status.color(),
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clip(MaterialTheme.shapes.small).background(badgeBackground).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun CheckInRow(
    checkIn: CheckIn,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(checkIn.student.initials())
            if (checkIn.status is CheckInStatus.Late) {
                Box(modifier = Modifier.size(10.dp).align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
        ) {
            Text(
                text = checkIn.student.name,
                style = MaterialTheme.typography.titleMedium,
            )
            StatusBadge(checkIn.status)
        }

        Text(
            text = checkIn.timeText(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun CheckInStatus.color(): Color =
    when (this) {
        CheckInStatus.OnTime -> MaterialTheme.colorScheme.primary
        is CheckInStatus.Late -> MaterialTheme.colorScheme.error
        CheckInStatus.Absent -> MaterialTheme.colorScheme.outline
    }

// Varijabla ce se mjenjati pritiskom na dugme medjutim nece se prikazati jer nije state
@Composable
fun WindowControls(
    openWindow: Window?,
    onOpen: (Window) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier) {
        Text("Open: ${openWindow?.label ?: "None"} ")
        Row {
            Window.entries.forEach { w ->
                Button(
                    onClick = { onOpen(w) },
                    enabled = openWindow != w,
                ) {
                    Text(w.label)
                }
            }
        }
    }
}

@Composable
fun ProfessorScreen(modifier: Modifier = Modifier) {
    var openWindow by rememberSaveable { mutableStateOf<Window?>(null) }
    var checkIns by remember {mutableStateOf(sampleCheckIns)}

    var query by rememberSaveable {mutableStateOf<String>("")}
    var lateOnly by rememberSaveable {mutableStateOf(false)}


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        WindowControls(openWindow = openWindow, onOpen = { openWindow = it })
        Spacer(Modifier.height(8.dp))

        if (openWindow == null) {
            Text("Open a window to see check ins")
        } else {
            val visible = checkIns.filter { it.window == openWindow && it.student.name.contains(query, ignoreCase = true) && (!lateOnly || it.status is CheckInStatus.Late)}
            AttendanceSummary(visible)
            SearchField(query, {query = it})
            LateOnlySwitch(lateOnly, {lateOnly = it})
            Spacer(Modifier.height(8.dp))
            Column {
                if(visible.isEmpty()){
                    Text("No check-ins match the filter.")
                }
                visible.forEach { checkIn ->
                    CheckInCard(checkIn, checkIns)
                }
            }
        }
    }
}

@Composable
fun SearchField(query : String, onQueryChange: (String) -> Unit, modifier : Modifier = Modifier) {
    Row(modifier = Modifier.fillMaxWidth()){
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = {Text("Search student")},
            placeholder = {Text("e.g. Amir")}
        )
    }
}

@Composable
fun LateOnlySwitch(onlyLate : Boolean, onlyLateChange : (Boolean) -> Unit, modifier : Modifier = Modifier){
    Row(modifier = Modifier.fillMaxWidth().toggleable(value = onlyLate, role = Role.Switch, onValueChange = {
        onlyLateChange(!onlyLate)
    }), verticalAlignment = Alignment.CenterVertically){
        Text("Only late", Modifier.weight(1f))
        Switch(checked = onlyLate, onCheckedChange = null)
    }
}

@Preview(showBackground = true)
@Composable
fun LateOnlySwitchPreview(){
    LateOnlySwitch(false, {})
}


@Preview(showBackground = true)
@Composable
fun CheckInCardPreview(){
    CheckInCard(sampleCheckIns[0], allCheckIns = sampleCheckIns)
}

@Preview(showBackground = true)
@Composable
fun SearchFieldPreview(){
    SearchField("ami", onQueryChange = {})
}