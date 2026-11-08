package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.settings_climate_outdoor_hint
import testvango.composeapp.generated.resources.settings_climate_outdoor_location
import testvango.composeapp.generated.resources.settings_climate_outdoor_location_desc
import testvango.composeapp.generated.resources.settings_climate_outdoor_sensor
import testvango.composeapp.generated.resources.settings_climate_outdoor_sensor_desc
import testvango.composeapp.generated.resources.settings_climate_outdoor_title
import testvango.composeapp.generated.resources.settings_climate_page_title
import testvango.composeapp.generated.resources.settings_climate_save

@Composable
fun ClimateSetting() {
    var selected by remember { mutableStateOf(climateOutdoorSource) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { backToMenuGrid() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
            Text(
                stringResource(Res.string.settings_climate_page_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.settings_climate_outdoor_title),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(Res.string.settings_climate_outdoor_hint),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            ClimateSourceOption(
                selected = selected == ClimateOutdoorSource.Sensor,
                title = stringResource(Res.string.settings_climate_outdoor_sensor),
                desc = stringResource(Res.string.settings_climate_outdoor_sensor_desc),
                icon = Icons.Filled.Sensors,
                onClick = { selected = ClimateOutdoorSource.Sensor },
            )
            ClimateSourceOption(
                selected = selected == ClimateOutdoorSource.Location,
                title = stringResource(Res.string.settings_climate_outdoor_location),
                desc = stringResource(Res.string.settings_climate_outdoor_location_desc),
                icon = Icons.Filled.Cloud,
                onClick = { selected = ClimateOutdoorSource.Location },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    applyClimateOutdoorSource(selected)
                    backToMenuGrid()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
            ) {
                Text(stringResource(Res.string.settings_climate_save), color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}

@Composable
private fun ClimateSourceOption(
    selected: Boolean,
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
        )
        Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(desc, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}
