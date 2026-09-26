package com.prismgrade.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One saved inspection. The subgrades and findings are flattened rather than
 * stored as a JSON blob so history stays queryable (e.g. "everything that
 * graded 9 or better") without a migration.
 */
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "card_name") val cardName: String,
    @ColumnInfo(name = "set_name") val setName: String,
    val year: String,
    @ColumnInfo(name = "card_number") val cardNumber: String,
    val confidence: String,
    @ColumnInfo(name = "value_low") val valueLow: Int,
    @ColumnInfo(name = "value_high") val valueHigh: Int,
    val currency: String,
    @ColumnInfo(name = "value_basis") val valueBasis: String,
    @ColumnInfo(name = "overall_grade") val overallGrade: Int,
    @ColumnInfo(name = "grade_label") val gradeLabel: String,
    @ColumnInfo(name = "centering_score") val centeringScore: Int,
    @ColumnInfo(name = "centering_note") val centeringNote: String,
    @ColumnInfo(name = "corners_score") val cornersScore: Int,
    @ColumnInfo(name = "corners_note") val cornersNote: String,
    @ColumnInfo(name = "edges_score") val edgesScore: Int,
    @ColumnInfo(name = "edges_note") val edgesNote: String,
    @ColumnInfo(name = "surface_score") val surfaceScore: Int,
    @ColumnInfo(name = "surface_note") val surfaceNote: String,
    @ColumnInfo(name = "authenticity_flag") val authenticityFlag: String,
    @ColumnInfo(name = "authenticity_note") val authenticityNote: String,
    /** Newline-separated; findings are display-only, so a list table would be overkill. */
    val findings: String,
    val source: String,
    @ColumnInfo(name = "front_image_path") val frontImagePath: String?,
    @ColumnInfo(name = "back_image_path") val backImagePath: String?,
)

@Dao
interface InspectionDao {

    @Query("SELECT * FROM inspections ORDER BY created_at DESC")
    fun observeAll(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun findById(id: Long): InspectionEntity?

    @Insert
    suspend fun insert(entity: InspectionEntity): Long

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM inspections")
    suspend fun count(): Int
}
