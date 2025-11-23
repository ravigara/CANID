package com.example.dogregistration.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// Enums
enum class DogType(val displayName: String) {
    PET("Pet"),
    STREET("Street Dog"),
    ADOPTED("Adopted")
}
enum class DogGender {
    Male, Female, None
}
enum class DogColor(val displayName: String) {
    NONE("None"),
    BLACK("Black"),
    WHITE("White"),
    BROWN("Brown"),
    MIXED("Mixed"),
    GOLDEN("Golden")
}

// Vaccination record
data class VaccinationRecord(
    val date: String,
    val id: UUID = UUID.randomUUID()
)

/**
 * DogProfile entity stored in Room.
 *
 * NOTE:
 * - We add `createdAt: Long` (timestamp) because many UI files expect it.
 * - We add `storageUri: String?` as the old name many screens used (alias to imageUri).
 * - Keep `imageUri` for the new code that uses it.
 */
@Entity(tableName = "dog_profiles")
data class DogProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val dogName: String = "",
    val breed: String = "",
    val gender: DogGender = DogGender.None,
    val primaryColor: DogColor = DogColor.NONE,
    val secondaryColor: DogColor = DogColor.NONE,
    val ageInMonths: Int = 0,
    val dogType: DogType = DogType.PET,
    val ownerName: String? = null,
    val adoptionDate: String? = null,
    val vaccinations: List<VaccinationRecord> = emptyList(),
    val microchipNumber: String? = null,

    // New persistent fields:
    // stable URI string (new name)
    val imageUri: String? = null,

    // keep the old name used across UI files to avoid large refactors
    val storageUri: String? = null,

    // keep createdAt timestamp (milliseconds since epoch) since UI expects this
    val createdAt: Long = System.currentTimeMillis(),

    // the embedding vector for identification
    val embedding: FloatArray? = null
) {
    // Provide equals/hashCode where FloatArray uses contentEquals/contentHashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DogProfile) return false

        if (id != other.id) return false
        if (dogName != other.dogName) return false
        if (breed != other.breed) return false
        if (gender != other.gender) return false
        if (primaryColor != other.primaryColor) return false
        if (secondaryColor != other.secondaryColor) return false
        if (ageInMonths != other.ageInMonths) return false
        if (dogType != other.dogType) return false
        if (ownerName != other.ownerName) return false
        if (adoptionDate != other.adoptionDate) return false
        if (vaccinations != other.vaccinations) return false
        if (microchipNumber != other.microchipNumber) return false
        if (imageUri != other.imageUri) return false
        if (storageUri != other.storageUri) return false
        if (createdAt != other.createdAt) return false

        // For arrays, use contentEquals
        if (embedding == null && other.embedding != null) return false
        if (embedding != null && other.embedding == null) return false
        if (embedding != null && other.embedding != null) {
            if (!embedding.contentEquals(other.embedding)) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dogName.hashCode()
        result = 31 * result + breed.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + primaryColor.hashCode()
        result = 31 * result + secondaryColor.hashCode()
        result = 31 * result + ageInMonths
        result = 31 * result + dogType.hashCode()
        result = 31 * result + (ownerName?.hashCode() ?: 0)
        result = 31 * result + (adoptionDate?.hashCode() ?: 0)
        result = 31 * result + vaccinations.hashCode()
        result = 31 * result + (microchipNumber?.hashCode() ?: 0)
        result = 31 * result + (imageUri?.hashCode() ?: 0)
        result = 31 * result + (storageUri?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
