package com.example.dogregistration.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogregistration.data.*
import java.util.UUID
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFormScreen(
    capturedUris: List<Uri>,
    onSubmit: (DogProfile) -> Unit
) {
    var dogName by rememberSaveable { mutableStateOf("") }
    var breed by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(DogGender.None) }
    var primaryColor by rememberSaveable { mutableStateOf(DogColor.NONE) }
    var secondaryColor by rememberSaveable { mutableStateOf(DogColor.NONE) }
    var ageInMonths by rememberSaveable { mutableStateOf("") }
    var dogType by rememberSaveable { mutableStateOf(DogType.PET) }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var adoptionDate by rememberSaveable { mutableStateOf("") }
    var microchipNumber by rememberSaveable { mutableStateOf("") }

    val vaccinations = remember { mutableStateListOf<VaccinationRecord>() }
    var newVaccinationDate by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Dog Registration Details", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (capturedUris.isNotEmpty()) {
            item {
                Text("Attached Photos:", style = MaterialTheme.typography.titleMedium)
                LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(capturedUris) { uri ->
                        AsyncImage(model = uri, contentDescription = "Captured photo", modifier = Modifier.size(80.dp))
                    }
                }
                Divider()
            }
        }

        item { OutlinedTextField(value = dogName, onValueChange = { dogName = it }, label = { Text("Dog Name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") }, modifier = Modifier.fillMaxWidth()) }
        item { GenderDropdown(gender) { gender = it } }
        item { ColorDropdown("Primary Color", primaryColor) { primaryColor = it } }
        item { ColorDropdown("Secondary Color", secondaryColor) { secondaryColor = it } }
        item {
            OutlinedTextField(
                value = ageInMonths,
                onValueChange = { ageInMonths = it.filter { c -> c.isDigit() } },
                label = { Text("Age in Months") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { DogTypeDropdown(dogType) { dogType = it } }

        if (dogType == DogType.PET || dogType == DogType.ADOPTED) {
            item { OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth()) }
        }
        if (dogType == DogType.ADOPTED) {
            item { OutlinedTextField(value = adoptionDate, onValueChange = { adoptionDate = it }, label = { Text("Adoption Date (e.g., YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Text("Vaccinations", style = MaterialTheme.typography.titleMedium)
            vaccinations.forEach { record ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.date, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vaccinations.remove(record) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove vaccination")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newVaccinationDate, onValueChange = { newVaccinationDate = it }, label = { Text("New Vaccination Date") }, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (newVaccinationDate.isNotBlank()) {
                        vaccinations.add(VaccinationRecord(date = newVaccinationDate))
                        vaccinations.sortByDescending { it.date }
                        newVaccinationDate = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add vaccination")
                }
            }
            Divider(modifier = Modifier.padding(top = 8.dp))
        }

        item { OutlinedTextField(value = microchipNumber, onValueChange = { microchipNumber = it }, label = { Text("Microchip Number (optional)") }, modifier = Modifier.fillMaxWidth()) }

        item {
            Button(
                onClick = {
                    val profile = DogProfile(
                        dogName = dogName,
                        breed = breed,
                        gender = gender,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        ageInMonths = ageInMonths.toIntOrNull() ?: 0,
                        dogType = dogType,
                        ownerName = if (dogType == DogType.PET || dogType == DogType.ADOPTED) ownerName else null,
                        adoptionDate = if (dogType == DogType.ADOPTED) adoptionDate else null,
                        vaccinations = vaccinations.toList(),
                        microchipNumber = microchipNumber.ifBlank { null }
                    )
                    onSubmit(profile)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Submit Registration")
            }
        }
    }
}

// Dropdown helpers unchanged (same code as before)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogTypeDropdown(selected: DogType, onSelected: (DogType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = DogType.values()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Dog Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { type ->
                DropdownMenuItem(text = { Text(type.displayName) }, onClick = {
                    onSelected(type)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(selected: DogGender, onSelected: (DogGender) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = DogGender.values().filterNot { it == DogGender.None }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = if (selected == DogGender.None) "" else selected.name,
            onValueChange = {}, readOnly = true, label = { Text("Gender") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { gender -> DropdownMenuItem(text = { Text(gender.name) }, onClick = { onSelected(gender); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDropdown(label: String, selected: DogColor, onSelected: (DogColor) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = DogColor.values()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { color -> DropdownMenuItem(text = { Text(color.displayName) }, onClick = { onSelected(color); expanded = false }) }
        }
    }
}
