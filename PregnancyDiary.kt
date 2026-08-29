// PregnancyDiary.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PregnancyDiary {
    @Parameter(names = ["--add"])
    private var add: Boolean = false

    @Parameter(names = ["--date"])
    private var dateStr: String? = null

    @Parameter(names = ["--week"])
    private var week: Int? = null

    @Parameter(names = ["--note"])
    private var note: String? = null

    @Parameter(names = ["--symptoms"])
    private var symptoms: String? = null

    @Parameter(names = ["--start-date"])
    private var startDate: String? = null

    @Parameter(names = ["--remind"])
    private var remind: Boolean = false

    @Parameter(names = ["--list"])
    private var list: Boolean = false

    @Parameter(names = ["--reminders"])
    private var reminders: Boolean = false

    @Parameter(names = ["--remind-done"])
    private var remindDone: Int? = null

    @Parameter(names = ["--export-json"])
    private var exportJson: String? = null

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    @Parameter(names = ["--export-txt"])
    private var exportTxt: String? = null

    data class Entry(val date: String, val week: Int, val note: String, val symptoms: String, val created: String = Instant.now().toString())
    data class Reminder(val id: Int, val date: String, val note: String, val done: Boolean = false, val created: String = Instant.now().toString())

    data class DiaryData(var start_date: String = "", val entries: MutableList<Entry> = mutableListOf(), val reminders: MutableList<Reminder> = mutableListOf())

    private val dataFile = "pregnancy.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<DiaryData>() {}.type
    private var data = DiaryData()
    private var nextReminderId = 1

    private fun load() {
        val f = File(dataFile)
        if (!f.exists()) return
        try {
            val json = f.readText()
            data = gson.fromJson(json, type) ?: DiaryData()
            nextReminderId = (data.reminders.maxOfOrNull { it.id } ?: 0) + 1
        } catch (e: Exception) { /* ignore */ }
    }

    private fun save() {
        val json = gson.toJson(data)
        File(dataFile).writeText(json)
    }

    private fun calculateWeek(dateStr: String): Int? {
        if (data.start_date.isEmpty()) return null
        val start = LocalDate.parse(data.start_date)
        val target = LocalDate.parse(dateStr)
        val delta = target.toEpochDay() - start.toEpochDay()
        if (delta < 0) return null
        return (delta / 7).toInt() + 1
    }

    fun setStartDate(date: String) {
        data.start_date = date
        save()
        println("\u001B[32mДата начала беременности установлена: $date\u001B[0m")
    }

    fun addEntry(date: String, week: Int?, note: String, symptoms: String) {
        val w = week ?: calculateWeek(date) ?: run {
            println("\u001B[31mНе удалось определить неделю. Укажите --week или --start-date\u001B[0m")
            return
        }
        data.entries.add(Entry(date, w, note, symptoms))
        save()
        println("\u001B[32mЗапись добавлена: $date, неделя $w\u001B[0m")
    }

    fun addReminder(date: String, note: String) {
        val reminder = Reminder(nextReminderId++, date, note)
        data.reminders.add(reminder)
        save()
        println("\u001B[32mНапоминание добавлено (ID: ${reminder.id})\u001B[0m")
    }

    fun listEntries() {
        if (data.entries.isEmpty()) {
            println("\u001B[33mНет записей.\u001B[0m")
            return
        }
        println("\u001B[36m📖 Дневник беременности:\u001B[0m")
        data.entries.sortedBy { it.date }.forEach {
            println("  ${it.date} | Неделя ${it.week} | ${it.note} | Симптомы: ${it.symptoms}")
        }
    }

    fun listReminders() {
        if (data.reminders.isEmpty()) {
            println("\u001B[33mНет напоминаний.\u001B[0m")
            return
        }
        println("\u001B[36m⏰ Напоминания:\u001B[0m")
        data.reminders.sortedBy { it.date }.forEach {
            val status = if (it.done) "\u001B[32m✓\u001B[0m" else "\u001B[31m✗\u001B[0m"
            println("  ${it.id}. $status ${it.date} - ${it.note}")
        }
    }

    fun markReminderDone(id: Int) {
        val idx = data.reminders.indexOfFirst { it.id == id }
        if (idx == -1) {
            println("\u001B[31mНапоминание #$id не найдено.\u001B[0m")
            return
        }
        data.reminders[idx] = data.reminders[idx].copy(done = true)
        save()
        println("\u001B[32mНапоминание #$id отмечено как выполненное.\u001B[0m")
    }

    fun exportJson(filename: String) {
        val json = gson.toJson(data)
        File(filename).writeText(json)
        println("\u001B[32mЭкспортировано в $filename (JSON)\u001B[0m")
    }

    fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("date,week,note,symptoms,created")
            data.entries.forEach {
                pw.println("${it.date},${it.week},${it.note},${it.symptoms},${it.created}")
            }
        }
        println("\u001B[32mЭкспортировано в $filename (CSV)\u001B[0m")
    }

    fun exportTxt(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("Дата начала беременности: ${data.start_date}\n")
            pw.println("=== ЗАПИСИ ===")
            data.entries.forEach {
                pw.println("${it.date} | Неделя ${it.week} | ${it.note} | Симптомы: ${it.symptoms}")
            }
            pw.println("\n=== НАПОМИНАНИЯ ===")
            data.reminders.forEach {
                pw.println("${it.id}. ${it.date} - ${it.note} ${if (it.done) "[x]" else "[ ]"}")
            }
        }
        println("\u001B[32mЭкспортировано в $filename (TXT)\u001B[0m")
    }

    fun run() {
        load()
        when {
            startDate != null -> setStartDate(startDate!!)
            add -> {
                if (dateStr == null) {
                    System.err.println("\u001B[31mДля добавления записи требуется --date\u001B[0m")
                    System.exit(1)
                }
                addEntry(dateStr!!, week, note ?: "", symptoms ?: "")
            }
            remind -> {
                if (dateStr == null || note == null) {
                    System.err.println("\u001B[31mДля добавления напоминания требуются --date и --note\u001B[0m")
                    System.exit(1)
                }
                addReminder(dateStr!!, note!!)
            }
            list -> listEntries()
            reminders -> listReminders()
            remindDone != null -> markReminderDone(remindDone!!)
            exportJson != null -> exportJson(exportJson!!)
            exportCsv != null -> exportCsv(exportCsv!!)
            exportTxt != null -> exportTxt(exportTxt!!)
            else -> println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val diary = PregnancyDiary()
    JCommander.newBuilder().addObject(diary).build().parse(*args)
    diary.run()
}
