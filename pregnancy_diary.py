
```python
#!/usr/bin/env python3
# pregnancy_diary.py
import argparse
import json
import csv
import os
import sys
from datetime import datetime, timedelta
from colorama import init, Fore, Style

init(autoreset=True)

DATA_FILE = "pregnancy.json"

class Entry:
    def __init__(self, date, week, note="", symptoms="", created=None):
        self.date = date
        self.week = week
        self.note = note
        self.symptoms = symptoms
        self.created = created or datetime.now().isoformat()

    def to_dict(self):
        return {"date": self.date, "week": self.week, "note": self.note, "symptoms": self.symptoms, "created": self.created}

    @classmethod
    def from_dict(cls, data):
        return cls(data["date"], data["week"], data.get("note", ""), data.get("symptoms", ""), data.get("created"))

class Reminder:
    def __init__(self, id, date, note, done=False, created=None):
        self.id = id
        self.date = date
        self.note = note
        self.done = done
        self.created = created or datetime.now().isoformat()

    def to_dict(self):
        return {"id": self.id, "date": self.date, "note": self.note, "done": self.done, "created": self.created}

    @classmethod
    def from_dict(cls, data):
        return cls(data["id"], data["date"], data["note"], data.get("done", False), data.get("created"))

class PregnancyDiary:
    def __init__(self):
        self.entries = []
        self.reminders = []
        self.start_date = None
        self.next_reminder_id = 1
        self.load()

    def load(self):
        if not os.path.exists(DATA_FILE):
            return
        try:
            with open(DATA_FILE, 'r') as f:
                data = json.load(f)
                self.start_date = data.get("start_date")
                self.entries = [Entry.from_dict(item) for item in data.get("entries", [])]
                self.reminders = [Reminder.from_dict(item) for item in data.get("reminders", [])]
                self.next_reminder_id = max([r.id for r in self.reminders] + [0]) + 1
        except:
            pass

    def save(self):
        with open(DATA_FILE, 'w') as f:
            json.dump({
                "start_date": self.start_date,
                "entries": [e.to_dict() for e in self.entries],
                "reminders": [r.to_dict() for r in self.reminders]
            }, f, indent=2)

    def set_start_date(self, date_str):
        self.start_date = date_str
        self.save()
        print(Fore.GREEN + f"Дата начала беременности установлена: {date_str}")

    def calculate_week(self, date_str):
        if not self.start_date:
            return None
        start = datetime.strptime(self.start_date, "%Y-%m-%d")
        target = datetime.strptime(date_str, "%Y-%m-%d")
        delta = (target - start).days
        if delta < 0:
            return None
        return delta // 7 + 1

    def add_entry(self, date_str, week=None, note="", symptoms=""):
        if week is None:
            week = self.calculate_week(date_str)
            if week is None:
                print(Fore.RED + "Не удалось определить неделю. Укажите --week или --start-date")
                return
        entry = Entry(date_str, week, note, symptoms)
        self.entries.append(entry)
        self.save()
        print(Fore.GREEN + f"Запись добавлена: {date_str}, неделя {week}")

    def add_reminder(self, date_str, note):
        reminder = Reminder(self.next_reminder_id, date_str, note)
        self.reminders.append(reminder)
        self.next_reminder_id += 1
        self.save()
        print(Fore.GREEN + f"Напоминание добавлено (ID: {reminder.id})")

    def list_entries(self):
        if not self.entries:
            print(Fore.YELLOW + "Нет записей.")
            return
        print(Fore.CYAN + "📖 Дневник беременности:")
        for e in sorted(self.entries, key=lambda x: x.date):
            print(f"  {e.date} | Неделя {e.week} | {e.note} | Симптомы: {e.symptoms}")

    def list_reminders(self):
        if not self.reminders:
            print(Fore.YELLOW + "Нет напоминаний.")
            return
        print(Fore.CYAN + "⏰ Напоминания:")
        for r in sorted(self.reminders, key=lambda x: x.date):
            status = Fore.GREEN + "✓" if r.done else Fore.RED + "✗"
            print(f"  {r.id}. {status} {r.date} - {r.note}")

    def mark_reminder_done(self, id):
        for r in self.reminders:
            if r.id == id:
                r.done = True
                self.save()
                print(Fore.GREEN + f"Напоминание #{id} отмечено как выполненное.")
                return
        print(Fore.RED + f"Напоминание #{id} не найдено.")

    def export_json(self, filename):
        data = {
            "start_date": self.start_date,
            "entries": [e.to_dict() for e in self.entries],
            "reminders": [r.to_dict() for r in self.reminders]
        }
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)
        print(Fore.GREEN + f"Экспортировано в {filename} (JSON)")

    def export_csv(self, filename):
        with open(filename, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["date", "week", "note", "symptoms", "created"])
            for e in self.entries:
                writer.writerow([e.date, e.week, e.note, e.symptoms, e.created])
        print(Fore.GREEN + f"Экспортировано в {filename} (CSV)")

    def export_txt(self, filename):
        with open(filename, 'w') as f:
            f.write(f"Дата начала беременности: {self.start_date}\n\n")
            f.write("=== ЗАПИСИ ===\n")
            for e in self.entries:
                f.write(f"{e.date} | Неделя {e.week} | {e.note} | Симптомы: {e.symptoms}\n")
            f.write("\n=== НАПОМИНАНИЯ ===\n")
            for r in self.reminders:
                f.write(f"{r.id}. {r.date} - {r.note} {'[x]' if r.done else '[ ]'}\n")
        print(Fore.GREEN + f"Экспортировано в {filename} (TXT)")

def main():
    parser = argparse.ArgumentParser(description="Дневник беременности (напоминания)")
    parser.add_argument("--add", action="store_true", help="Добавить запись")
    parser.add_argument("--date", help="Дата (YYYY-MM-DD)")
    parser.add_argument("--week", type=int, help="Неделя беременности")
    parser.add_argument("--note", help="Заметка")
    parser.add_argument("--symptoms", help="Симптомы")
    parser.add_argument("--start-date", help="Дата начала беременности (YYYY-MM-DD)")
    parser.add_argument("--remind", action="store_true", help="Добавить напоминание")
    parser.add_argument("--list", action="store_true", help="Показать записи")
    parser.add_argument("--reminders", action="store_true", help="Показать напоминания")
    parser.add_argument("--remind-done", type=int, help="Отметить напоминание как выполненное")
    parser.add_argument("--export-json", help="Экспорт в JSON")
    parser.add_argument("--export-csv", help="Экспорт в CSV")
    parser.add_argument("--export-txt", help="Экспорт в TXT")
    args = parser.parse_args()

    diary = PregnancyDiary()

    if args.start_date:
        diary.set_start_date(args.start_date)
    elif args.add:
        if not args.date:
            print(Fore.RED + "Для добавления записи требуется --date")
            sys.exit(1)
        diary.add_entry(args.date, args.week, args.note or "", args.symptoms or "")
    elif args.remind:
        if not args.date or not args.note:
            print(Fore.RED + "Для добавления напоминания требуются --date и --note")
            sys.exit(1)
        diary.add_reminder(args.date, args.note)
    elif args.list:
        diary.list_entries()
    elif args.reminders:
        diary.list_reminders()
    elif args.remind_done is not None:
        diary.mark_reminder_done(args.remind_done)
    elif args.export_json:
        diary.export_json(args.export_json)
    elif args.export_csv:
        diary.export_csv(args.export_csv)
    elif args.export_txt:
        diary.export_txt(args.export_txt)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
