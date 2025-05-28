import kotlinx.serialization.json.Json
import org.example.models.Report
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportTest {

    @Test
    fun `test Report initialization`() {
        val report = Report(
            reporterUsername = "user1",
            reportedUsername = "user2",
            reason = "Spam",
            timestamp = "2023-10-01T12:00:00"
        )

        assertEquals("user1", report.reporterUsername)
        assertEquals("user2", report.reportedUsername)
        assertEquals("Spam", report.reason)
        assertEquals("2023-10-01T12:00:00", report.timestamp)
    }

    @Test
    fun `test Report serialization and deserialization`() {
        val report = Report(
            reporterUsername = "user1",
            reportedUsername = "user2",
            reason = "Spam",
            timestamp = "2023-10-01T12:00:00"
        )

        val json = Json.encodeToString(Report.serializer(), report)
        val deserializedReport = Json.decodeFromString(Report.serializer(), json)

        assertEquals(report, deserializedReport)
    }
}