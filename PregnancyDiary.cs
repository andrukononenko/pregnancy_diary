// PregnancyDiary.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace PregnancyDiary
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var diary = new PregnancyDiary();
            if (opts.StartDate != null)
            {
                diary.SetStartDate(opts.StartDate);
            }
            else if (opts.Add)
            {
                if (opts.Date == null)
                {
                    Console.Error.WriteLine("\u001B[31mДля добавления записи требуется --date\u001B[0m");
                    return;
                }
                diary.AddEntry(opts.Date, opts.Week, opts.Note ?? "", opts.Symptoms ?? "");
            }
            else if (opts.Remind)
            {
                if (opts.Date == null || opts.Note == null)
                {
                    Console.Error.WriteLine("\u001B[31mДля добавления напоминания требуются --date и --note\u001B[0m");
                    return;
                }
                diary.AddReminder(opts.Date, opts.Note);
            }
            else if (opts.List)
            {
                diary.ListEntries();
            }
            else if (opts.Reminders)
            {
                diary.ListReminders();
            }
            else if (opts.RemindDone.HasValue)
            {
                diary.MarkReminderDone(opts.RemindDone.Value);
            }
            else if (opts.ExportJson != null)
            {
                diary.ExportJson(opts.ExportJson);
            }
            else if (opts.ExportCsv != null)
            {
                diary.ExportCsv(opts.ExportCsv);
            }
            else if (opts.ExportTxt != null)
            {
                diary.ExportTxt(opts.ExportTxt);
            }
            else
            {
                Console.WriteLine("Используйте --help для справки.");
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--add": opts.Add = true; break;
                    case "--date": opts.Date = args[++i]; break;
                    case "--week": opts.Week = int.Parse(args[++i]); break;
                    case "--note": opts.Note = args[++i]; break;
                    case "--symptoms": opts.Symptoms = args[++i]; break;
                    case "--start-date": opts.StartDate = args[++i]; break;
                    case "--remind": opts.Remind = true; break;
                    case "--list": opts.List = true; break;
                    case "--reminders": opts.Reminders = true; break;
                    case "--remind-done": opts.RemindDone = int.Parse(args[++i]); break;
                    case "--export-json": opts.ExportJson = args[++i]; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                    case "--export-txt": opts.ExportTxt = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public bool Add { get; set; }
            public string Date { get; set; }
            public int? Week { get; set; }
            public string Note { get; set; }
            public string Symptoms { get; set; }
            public string StartDate { get; set; }
            public bool Remind { get; set; }
            public bool List { get; set; }
            public bool Reminders { get; set; }
            public int? RemindDone { get; set; }
            public string ExportJson { get; set; }
            public string ExportCsv { get; set; }
            public string ExportTxt { get; set; }
        }

        class Entry
        {
            public string Date { get; set; }
            public int Week { get; set; }
            public string Note { get; set; }
            public string Symptoms { get; set; }
            public string Created { get; set; }
        }

        class Reminder
        {
            public int Id { get; set; }
            public string Date { get; set; }
            public string Note { get; set; }
            public bool Done { get; set; }
            public string Created { get; set; }
        }

        class DiaryData
        {
            public string StartDate { get; set; } = "";
            public List<Entry> Entries { get; set; } = new List<Entry>();
            public List<Reminder> Reminders { get; set; } = new List<Reminder>();
        }

        class PregnancyDiary
        {
            private const string DataFile = "pregnancy.json";
            private DiaryData data = new DiaryData();
            private int nextReminderId = 1;

            public PregnancyDiary() => Load();

            private void Load()
            {
                try
                {
                    if (File.Exists(DataFile))
                    {
                        string json = File.ReadAllText(DataFile);
                        data = JsonSerializer.Deserialize<DiaryData>(json) ?? new DiaryData();
                        nextReminderId = data.Reminders.Select(r => r.Id).DefaultIfEmpty(0).Max() + 1;
                    }
                }
                catch { data = new DiaryData(); }
            }

            private void Save()
            {
                string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(DataFile, json);
            }

            public void SetStartDate(string date)
            {
                data.StartDate = date;
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Дата начала беременности установлена: {date}");
                Console.ResetColor();
            }

            private int? CalculateWeek(string date)
            {
                if (string.IsNullOrEmpty(data.StartDate)) return null;
                var start = DateTime.ParseExact(data.StartDate, "yyyy-MM-dd", null);
                var target = DateTime.ParseExact(date, "yyyy-MM-dd", null);
                var delta = (target - start).Days;
                if (delta < 0) return null;
                return delta / 7 + 1;
            }

            public void AddEntry(string date, int? week, string note, string symptoms)
            {
                if (!week.HasValue)
                {
                    week = CalculateWeek(date);
                    if (!week.HasValue)
                    {
                        Console.ForegroundColor = ConsoleColor.Red;
                        Console.WriteLine("Не удалось определить неделю. Укажите --week или --start-date");
                        Console.ResetColor();
                        return;
                    }
                }
                data.Entries.Add(new Entry
                {
                    Date = date,
                    Week = week.Value,
                    Note = note,
                    Symptoms = symptoms,
                    Created = DateTime.UtcNow.ToString("o")
                });
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Запись добавлена: {date}, неделя {week}");
                Console.ResetColor();
            }

            public void AddReminder(string date, string note)
            {
                data.Reminders.Add(new Reminder
                {
                    Id = nextReminderId++,
                    Date = date,
                    Note = note,
                    Done = false,
                    Created = DateTime.UtcNow.ToString("o")
                });
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Напоминание добавлено (ID: {nextReminderId - 1})");
                Console.ResetColor();
            }

            public void ListEntries()
            {
                if (data.Entries.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет записей.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("📖 Дневник беременности:");
                Console.ResetColor();
                foreach (var e in data.Entries.OrderBy(e => e.Date))
                {
                    Console.WriteLine($"  {e.Date} | Неделя {e.Week} | {e.Note} | Симптомы: {e.Symptoms}");
                }
            }

            public void ListReminders()
            {
                if (data.Reminders.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет напоминаний.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("⏰ Напоминания:");
                Console.ResetColor();
                foreach (var r in data.Reminders.OrderBy(r => r.Date))
                {
                    var status = r.Done ? "\u001B[32m✓\u001B[0m" : "\u001B[31m✗\u001B[0m";
                    Console.WriteLine($"  {r.Id}. {status} {r.Date} - {r.Note}");
                }
            }

            public void MarkReminderDone(int id)
            {
                var r = data.Reminders.FirstOrDefault(r => r.Id == id);
                if (r == null)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine($"Напоминание #{id} не найдено.");
                    Console.ResetColor();
                    return;
                }
                r.Done = true;
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Напоминание #{id} отмечено как выполненное.");
                Console.ResetColor();
            }

            public void ExportJson(string filename)
            {
                string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(filename, json);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (JSON)");
                Console.ResetColor();
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine("date,week,note,symptoms,created");
                foreach (var e in data.Entries)
                    sw.WriteLine($"{e.Date},{e.Week},{e.Note},{e.Symptoms},{e.Created}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (CSV)");
                Console.ResetColor();
            }

            public void ExportTxt(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine($"Дата начала беременности: {data.StartDate}\n");
                sw.WriteLine("=== ЗАПИСИ ===");
                foreach (var e in data.Entries)
                    sw.WriteLine($"{e.Date} | Неделя {e.Week} | {e.Note} | Симптомы: {e.Symptoms}");
                sw.WriteLine("\n=== НАПОМИНАНИЯ ===");
                foreach (var r in data.Reminders)
                    sw.WriteLine($"{r.Id}. {r.Date} - {r.Note} {(r.Done ? "[x]" : "[ ]")}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (TXT)");
                Console.ResetColor();
            }
        }
    }
}
