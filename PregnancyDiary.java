// PregnancyDiary.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PregnancyDiary {
    private static final String DATA_FILE = "pregnancy.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<DiaryData>(){}.getType();

    @Parameter(names = "--add")
    private boolean add;
    @Parameter(names = "--date")
    private String dateStr;
    @Parameter(names = "--week")
    private Integer week;
    @Parameter(names = "--note")
    private String note;
    @Parameter(names = "--symptoms")
    private String symptoms;
    @Parameter(names = "--start-date")
    private String startDate;
    @Parameter(names = "--remind")
    private boolean remind;
    @Parameter(names = "--list")
    private boolean list;
    @Parameter(names = "--reminders")
    private boolean reminders;
    @Parameter(names = "--remind-done")
    private Integer remindDone;
    @Parameter(names = "--export-json")
    private String exportJson;
    @Parameter(names = "--export-csv")
    private String exportCsv;
    @Parameter(names = "--export-txt")
    private String exportTxt;

    static class Entry {
        String date, note, symptoms, created;
        int week;
    }

    static class Reminder {
        int id;
        String date, note, created;
        boolean done;
    }

    static class DiaryData {
        String start_date;
        List<Entry> entries = new ArrayList<>();
        List<Reminder> reminders = new ArrayList<>();
    }

    private DiaryData data = new DiaryData();
    private int nextReminderId = 1;

    private void load() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
            data = GSON.fromJson(json, DATA_TYPE);
            nextReminderId = data.reminders.stream().mapToInt(r -> r.id).max().orElse(0) + 1;
        } catch (Exception e) {
            data = new DiaryData();
            nextReminderId = 1;
        }
    }

    private void save() {
        try {
            Files.write(Paths.get(DATA_FILE), GSON.toJson(data).getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private void setStartDate(String date) {
        data.start_date = date;
        save();
        System.out.println("\u001B[32mДата начала беременности установлена: " + date + "\u001B[0m");
    }

    private Integer calculateWeek(String date) {
        if (data.start_date == null || data.start_date.isEmpty()) return null;
        LocalDate start = LocalDate.parse(data.start_date);
        LocalDate target = LocalDate.parse(date);
        long delta = target.toEpochDay() - start.toEpochDay();
        if (delta < 0) return null;
        return (int)(delta / 7) + 1;
    }

    private void addEntry(String date, Integer week, String note, String symptoms) {
        if (week == null) {
            week = calculateWeek(date);
            if (week == null) {
                System.err.println("\u001B[31mНе удалось определить неделю. Укажите --week или --start-date\u001B[0m");
                return;
            }
        }
        Entry e = new Entry();
        e.date = date;
        e.week = week;
        e.note = note != null ? note : "";
        e.symptoms = symptoms != null ? symptoms : "";
        e.created = java.time.Instant.now().toString();
        data.entries.add(e);
        save();
        System.out.println("\u001B[32mЗапись добавлена: " + date + ", неделя " + week + "\u001B[0m");
    }

    private void addReminder(String date, String note) {
        Reminder r = new Reminder();
        r.id = nextReminderId++;
        r.date = date;
        r.note = note;
        r.done = false;
        r.created = java.time.Instant.now().toString();
        data.reminders.add(r);
        save();
        System.out.println("\u001B[32mНапоминание добавлено (ID: " + r.id + ")\u001B[0m");
    }

    private void listEntries() {
        if (data.entries.isEmpty()) {
            System.out.println("\u001B[33mНет записей.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36m📖 Дневник беременности:\u001B[0m");
        data.entries.sort(Comparator.comparing(e -> e.date));
        for (Entry e : data.entries) {
            System.out.printf("  %s | Неделя %d | %s | Симптомы: %s%n", e.date, e.week, e.note, e.symptoms);
        }
    }

    private void listReminders() {
        if (data.reminders.isEmpty()) {
            System.out.println("\u001B[33mНет напоминаний.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36m⏰ Напоминания:\u001B[0m");
        data.reminders.sort(Comparator.comparing(r -> r.date));
        for (Reminder r : data.reminders) {
            String status = r.done ? "\u001B[32m✓\u001B[0m" : "\u001B[31m✗\u001B[0m";
            System.out.printf("  %d. %s %s - %s%n", r.id, status, r.date, r.note);
        }
    }

    private void markReminderDone(int id) {
        for (Reminder r : data.reminders) {
            if (r.id == id) {
                r.done = true;
                save();
                System.out.println("\u001B[32mНапоминание #" + id + " отмечено как выполненное.\u001B[0m");
                return;
            }
        }
        System.out.println("\u001B[31mНапоминание #" + id + " не найдено.\u001B[0m");
    }

    private void exportJson(String filename) throws IOException {
        Files.write(Paths.get(filename), GSON.toJson(data).getBytes());
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (JSON)\u001B[0m");
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("date,week,note,symptoms,created");
            for (Entry e : data.entries) {
                pw.printf("%s,%d,%s,%s,%s%n", e.date, e.week, e.note, e.symptoms, e.created);
            }
        }
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (CSV)\u001B[0m");
    }

    private void exportTxt(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Дата начала беременности: " + data.start_date);
            pw.println("\n=== ЗАПИСИ ===");
            for (Entry e : data.entries) {
                pw.printf("%s | Неделя %d | %s | Симптомы: %s%n", e.date, e.week, e.note, e.symptoms);
            }
            pw.println("\n=== НАПОМИНАНИЯ ===");
            for (Reminder r : data.reminders) {
                pw.printf("%d. %s - %s %s%n", r.id, r.date, r.note, r.done ? "[x]" : "[ ]");
            }
        }
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (TXT)\u001B[0m");
    }

    public void run() throws Exception {
        load();
        if (startDate != null) {
            setStartDate(startDate);
        } else if (add) {
            if (dateStr == null) {
                System.err.println("\u001B[31mДля добавления записи требуется --date\u001B[0m");
                System.exit(1);
            }
            addEntry(dateStr, week, note, symptoms);
        } else if (remind) {
            if (dateStr == null || note == null) {
                System.err.println("\u001B[31mДля добавления напоминания требуются --date и --note\u001B[0m");
                System.exit(1);
            }
            addReminder(dateStr, note);
        } else if (list) {
            listEntries();
        } else if (reminders) {
            listReminders();
        } else if (remindDone != null) {
            markReminderDone(remindDone);
        } else if (exportJson != null) {
            exportJson(exportJson);
        } else if (exportCsv != null) {
            exportCsv(exportCsv);
        } else if (exportTxt != null) {
            exportTxt(exportTxt);
        } else {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        PregnancyDiary diary = new PregnancyDiary();
        JCommander.newBuilder().addObject(diary).build().parse(args);
        diary.run();
    }
}
