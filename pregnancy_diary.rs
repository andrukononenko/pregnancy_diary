// pregnancy_diary.rs
use chrono::{DateTime, Local, NaiveDate, Utc};
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::Write;
use colored::*;

const DATA_FILE: &str = "pregnancy.json";

#[derive(Serialize, Deserialize)]
struct Entry {
    date: String,
    week: u32,
    note: String,
    symptoms: String,
    created: String,
}

#[derive(Serialize, Deserialize)]
struct Reminder {
    id: u32,
    date: String,
    note: String,
    done: bool,
    created: String,
}

#[derive(Serialize, Deserialize)]
struct DiaryData {
    start_date: String,
    entries: Vec<Entry>,
    reminders: Vec<Reminder>,
}

struct PregnancyDiary {
    data: DiaryData,
    next_reminder_id: u32,
}

impl PregnancyDiary {
    fn new() -> Self {
        let mut d = PregnancyDiary {
            data: DiaryData {
                start_date: "".to_string(),
                entries: Vec::new(),
                reminders: Vec::new(),
            },
            next_reminder_id: 1,
        };
        d.load();
        d
    }

    fn load(&mut self) {
        if let Ok(data) = fs::read_to_string(DATA_FILE) {
            if let Ok(mut diary_data) = serde_json::from_str::<DiaryData>(&data) {
                self.data = diary_data;
                self.next_reminder_id = self.data.reminders.iter().map(|r| r.id).max().unwrap_or(0) + 1;
            }
        }
    }

    fn save(&self) {
        let json = serde_json::to_string_pretty(&self.data).unwrap();
        fs::write(DATA_FILE, json).unwrap();
    }

    fn set_start_date(&mut self, date_str: &str) {
        self.data.start_date = date_str.to_string();
        self.save();
        println!("{}", format!("Дата начала беременности установлена: {}", date_str).green());
    }

    fn calculate_week(&self, date_str: &str) -> Option<u32> {
        if self.data.start_date.is_empty() {
            return None;
        }
        let start = NaiveDate::parse_from_str(&self.data.start_date, "%Y-%m-%d").ok()?;
        let target = NaiveDate::parse_from_str(date_str, "%Y-%m-%d").ok()?;
        let delta = (target - start).num_days();
        if delta < 0 { return None; }
        Some((delta / 7) as u32 + 1)
    }

    fn add_entry(&mut self, date_str: &str, week: Option<u32>, note: &str, symptoms: &str) {
        let week = if let Some(w) = week {
            w
        } else {
            match self.calculate_week(date_str) {
                Some(w) => w,
                None => {
                    println!("{}", "Не удалось определить неделю. Укажите --week или --start-date".red());
                    return;
                }
            }
        };
        let entry = Entry {
            date: date_str.to_string(),
            week,
            note: note.to_string(),
            symptoms: symptoms.to_string(),
            created: Utc::now().to_rfc3339(),
        };
        self.data.entries.push(entry);
        self.save();
        println!("{}", format!("Запись добавлена: {}, неделя {}", date_str, week).green());
    }

    fn add_reminder(&mut self, date_str: &str, note: &str) {
        let reminder = Reminder {
            id: self.next_reminder_id,
            date: date_str.to_string(),
            note: note.to_string(),
            done: false,
            created: Utc::now().to_rfc3339(),
        };
        self.next_reminder_id += 1;
        self.data.reminders.push(reminder);
        self.save();
        println!("{}", format!("Напоминание добавлено (ID: {})", reminder.id).green());
    }

    fn list_entries(&self) {
        if self.data.entries.is_empty() {
            println!("{}", "Нет записей.".yellow());
            return;
        }
        println!("{}", "📖 Дневник беременности:".cyan());
        for e in &self.data.entries {
            println!("  {} | Неделя {} | {} | Симптомы: {}", e.date, e.week, e.note, e.symptoms);
        }
    }

    fn list_reminders(&self) {
        if self.data.reminders.is_empty() {
            println!("{}", "Нет напоминаний.".yellow());
            return;
        }
        println!("{}", "⏰ Напоминания:".cyan());
        for r in &self.data.reminders {
            let status = if r.done { "✓".green() } else { "✗".red() };
            println!("  {}. {} {} - {}", r.id, status, r.date, r.note);
        }
    }

    fn mark_reminder_done(&mut self, id: u32) {
        if let Some(r) = self.data.reminders.iter_mut().find(|r| r.id == id) {
            r.done = true;
            self.save();
            println!("{}", format!("Напоминание #{} отмечено как выполненное.", id).green());
        } else {
            println!("{}", format!("Напоминание #{} не найдено.", id).red());
        }
    }

    fn export_json(&self, filename: &str) {
        let json = serde_json::to_string_pretty(&self.data).unwrap();
        fs::write(filename, json).unwrap();
        println!("{}", format!("Экспортировано в {} (JSON)", filename).green());
    }

    fn export_csv(&self, filename: &str) {
        let mut wtr = csv::Writer::from_path(filename).unwrap();
        wtr.write_record(&["date", "week", "note", "symptoms", "created"]).unwrap();
        for e in &self.data.entries {
            wtr.write_record(&[&e.date, &e.week.to_string(), &e.note, &e.symptoms, &e.created]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Экспортировано в {} (CSV)", filename).green());
    }

    fn export_txt(&self, filename: &str) {
        let mut content = String::new();
        content.push_str(&format!("Дата начала беременности: {}\n\n", self.data.start_date));
        content.push_str("=== ЗАПИСИ ===\n");
        for e in &self.data.entries {
            content.push_str(&format!("{} | Неделя {} | {} | Симптомы: {}\n", e.date, e.week, e.note, e.symptoms));
        }
        content.push_str("\n=== НАПОМИНАНИЯ ===\n");
        for r in &self.data.reminders {
            let done = if r.done { "[x]" } else { "[ ]" };
            content.push_str(&format!("{}. {} - {} {}\n", r.id, r.date, r.note, done));
        }
        fs::write(filename, content).unwrap();
        println!("{}", format!("Экспортировано в {} (TXT)", filename).green());
    }
}

fn main() {
    let matches = App::new("Pregnancy Diary")
        .arg(Arg::with_name("add").long("add").help("Добавить запись"))
        .arg(Arg::with_name("date").long("date").takes_value(true).help("Дата (YYYY-MM-DD)"))
        .arg(Arg::with_name("week").long("week").takes_value(true).help("Неделя беременности"))
        .arg(Arg::with_name("note").long("note").takes_value(true).help("Заметка"))
        .arg(Arg::with_name("symptoms").long("symptoms").takes_value(true).help("Симптомы"))
        .arg(Arg::with_name("start-date").long("start-date").takes_value(true).help("Дата начала беременности"))
        .arg(Arg::with_name("remind").long("remind").help("Добавить напоминание"))
        .arg(Arg::with_name("list").long("list").help("Показать записи"))
        .arg(Arg::with_name("reminders").long("reminders").help("Показать напоминания"))
        .arg(Arg::with_name("remind-done").long("remind-done").takes_value(true).help("Отметить напоминание как выполненное"))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true).help("Экспорт в JSON"))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true).help("Экспорт в CSV"))
        .arg(Arg::with_name("export-txt").long("export-txt").takes_value(true).help("Экспорт в TXT"))
        .get_matches();

    let mut diary = PregnancyDiary::new();

    if let Some(date) = matches.value_of("start-date") {
        diary.set_start_date(date);
    } else if matches.is_present("add") {
        let date = matches.value_of("date").expect("--date required");
        let week = matches.value_of("week").map(|s| s.parse().unwrap());
        let note = matches.value_of("note").unwrap_or("");
        let symptoms = matches.value_of("symptoms").unwrap_or("");
        diary.add_entry(date, week, note, symptoms);
    } else if matches.is_present("remind") {
        let date = matches.value_of("date").expect("--date required");
        let note = matches.value_of("note").expect("--note required");
        diary.add_reminder(date, note);
    } else if matches.is_present("list") {
        diary.list_entries();
    } else if matches.is_present("reminders") {
        diary.list_reminders();
    } else if let Some(id_str) = matches.value_of("remind-done") {
        let id: u32 = id_str.parse().expect("Invalid ID");
        diary.mark_reminder_done(id);
    } else if let Some(file) = matches.value_of("export-json") {
        diary.export_json(file);
    } else if let Some(file) = matches.value_of("export-csv") {
        diary.export_csv(file);
    } else if let Some(file) = matches.value_of("export-txt") {
        diary.export_txt(file);
    } else {
        println!("Используйте --help для справки.");
    }
}
