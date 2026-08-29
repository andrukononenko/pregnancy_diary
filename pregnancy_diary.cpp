// pregnancy_diary.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm>
#include <ctime>
#include <iomanip>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const string DATA_FILE = "pregnancy.json";

struct Entry {
    string date;
    int week;
    string note;
    string symptoms;
    string created;
};

struct Reminder {
    int id;
    string date;
    string note;
    bool done;
    string created;
};

struct DiaryData {
    string start_date;
    vector<Entry> entries;
    vector<Reminder> reminders;
};

class PregnancyDiary {
private:
    DiaryData data;
    int nextReminderId = 1;

    string currentTime() {
        time_t t = time(nullptr);
        char buf[64];
        strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", gmtime(&t));
        return string(buf);
    }

    void load() {
        ifstream ifs(DATA_FILE);
        if (!ifs) return;
        Json::Value root;
        ifs >> root;
        data.start_date = root.get("start_date", "").asString();
        for (const auto& item : root["entries"]) {
            Entry e;
            e.date = item["date"].asString();
            e.week = item["week"].asInt();
            e.note = item["note"].asString();
            e.symptoms = item["symptoms"].asString();
            e.created = item["created"].asString();
            data.entries.push_back(e);
        }
        for (const auto& item : root["reminders"]) {
            Reminder r;
            r.id = item["id"].asInt();
            r.date = item["date"].asString();
            r.note = item["note"].asString();
            r.done = item["done"].asBool();
            r.created = item["created"].asString();
            data.reminders.push_back(r);
        }
        nextReminderId = data.reminders.empty() ? 1 : data.reminders.back().id + 1;
    }

    void save() {
        Json::Value root;
        root["start_date"] = data.start_date;
        for (const auto& e : data.entries) {
            Json::Value item;
            item["date"] = e.date;
            item["week"] = e.week;
            item["note"] = e.note;
            item["symptoms"] = e.symptoms;
            item["created"] = e.created;
            root["entries"].append(item);
        }
        for (const auto& r : data.reminders) {
            Json::Value item;
            item["id"] = r.id;
            item["date"] = r.date;
            item["note"] = r.note;
            item["done"] = r.done;
            item["created"] = r.created;
            root["reminders"].append(item);
        }
        ofstream ofs(DATA_FILE);
        ofs << root.toStyledString();
    }

    int calculateWeek(const string& dateStr) {
        if (data.start_date.empty()) return -1;
        tm tm1 = {}, tm2 = {};
        strptime(data.start_date.c_str(), "%Y-%m-%d", &tm1);
        strptime(dateStr.c_str(), "%Y-%m-%d", &tm2);
        time_t t1 = mktime(&tm1);
        time_t t2 = mktime(&tm2);
        int delta = (t2 - t1) / 86400;
        if (delta < 0) return -1;
        return delta / 7 + 1;
    }

public:
    PregnancyDiary() { load(); }

    void setStartDate(const string& date) {
        data.start_date = date;
        save();
        cout << "\033[32mДата начала беременности установлена: " << date << "\033[0m" << endl;
    }

    void addEntry(const string& date, int week, const string& note, const string& symptoms) {
        if (week == 0) {
            week = calculateWeek(date);
            if (week == -1) {
                cout << "\033[31mНе удалось определить неделю. Укажите --week или --start-date\033[0m" << endl;
                return;
            }
        }
        Entry e{date, week, note, symptoms, currentTime()};
        data.entries.push_back(e);
        save();
        cout << "\033[32mЗапись добавлена: " << date << ", неделя " << week << "\033[0m" << endl;
    }

    void addReminder(const string& date, const string& note) {
        Reminder r{nextReminderId++, date, note, false, currentTime()};
        data.reminders.push_back(r);
        save();
        cout << "\033[32mНапоминание добавлено (ID: " << r.id << ")\033[0m" << endl;
    }

    void listEntries() {
        if (data.entries.empty()) {
            cout << "\033[33mНет записей.\033[0m" << endl;
            return;
        }
        cout << "\033[36m📖 Дневник беременности:\033[0m" << endl;
        sort(data.entries.begin(), data.entries.end(), [](const Entry& a, const Entry& b) {
            return a.date < b.date;
        });
        for (const auto& e : data.entries) {
            cout << "  " << e.date << " | Неделя " << e.week << " | " << e.note << " | Симптомы: " << e.symptoms << endl;
        }
    }

    void listReminders() {
        if (data.reminders.empty()) {
            cout << "\033[33mНет напоминаний.\033[0m" << endl;
            return;
        }
        cout << "\033[36m⏰ Напоминания:\033[0m" << endl;
        sort(data.reminders.begin(), data.reminders.end(), [](const Reminder& a, const Reminder& b) {
            return a.date < b.date;
        });
        for (const auto& r : data.reminders) {
            string status = r.done ? "\033[32m✓\033[0m" : "\033[31m✗\033[0m";
            cout << "  " << r.id << ". " << status << " " << r.date << " - " << r.note << endl;
        }
    }

    void markReminderDone(int id) {
        for (auto& r : data.reminders) {
            if (r.id == id) {
                r.done = true;
                save();
                cout << "\033[32mНапоминание #" << id << " отмечено как выполненное.\033[0m" << endl;
                return;
            }
        }
        cout << "\033[31mНапоминание #" << id << " не найдено.\033[0m" << endl;
    }

    void exportJSON(const string& filename) {
        Json::Value root;
        root["start_date"] = data.start_date;
        for (const auto& e : data.entries) {
            Json::Value item;
            item["date"] = e.date;
            item["week"] = e.week;
            item["note"] = e.note;
            item["symptoms"] = e.symptoms;
            item["created"] = e.created;
            root["entries"].append(item);
        }
        for (const auto& r : data.reminders) {
            Json::Value item;
            item["id"] = r.id;
            item["date"] = r.date;
            item["note"] = r.note;
            item["done"] = r.done;
            item["created"] = r.created;
            root["reminders"].append(item);
        }
        ofstream ofs(filename);
        ofs << root.toStyledString();
        cout << "\033[32mЭкспортировано в " << filename << " (JSON)\033[0m" << endl;
    }

    void exportCSV(const string& filename) {
        ofstream ofs(filename);
        ofs << "date,week,note,symptoms,created\n";
        for (const auto& e : data.entries) {
            ofs << e.date << "," << e.week << "," << e.note << "," << e.symptoms << "," << e.created << "\n";
        }
        cout << "\033[32mЭкспортировано в " << filename << " (CSV)\033[0m" << endl;
    }

    void exportTXT(const string& filename) {
        ofstream ofs(filename);
        ofs << "Дата начала беременности: " << data.start_date << "\n\n";
        ofs << "=== ЗАПИСИ ===\n";
        for (const auto& e : data.entries) {
            ofs << e.date << " | Неделя " << e.week << " | " << e.note << " | Симптомы: " << e.symptoms << "\n";
        }
        ofs << "\n=== НАПОМИНАНИЯ ===\n";
        for (const auto& r : data.reminders) {
            ofs << r.id << ". " << r.date << " - " << r.note << " " << (r.done ? "[x]" : "[ ]") << "\n";
        }
        cout << "\033[32mЭкспортировано в " << filename << " (TXT)\033[0m" << endl;
    }
};

int main(int argc, char* argv[]) {
    bool add = false, remind = false, list = false, reminders = false;
    string dateStr, note, symptoms, startDate, json, csv, txt;
    int week = 0, remindDone = 0;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--add") add = true;
        else if (arg == "--date" && i+1 < argc) dateStr = argv[++i];
        else if (arg == "--week" && i+1 < argc) week = stoi(argv[++i]);
        else if (arg == "--note" && i+1 < argc) note = argv[++i];
        else if (arg == "--symptoms" && i+1 < argc) symptoms = argv[++i];
        else if (arg == "--start-date" && i+1 < argc) startDate = argv[++i];
        else if (arg == "--remind") remind = true;
        else if (arg == "--list") list = true;
        else if (arg == "--reminders") reminders = true;
        else if (arg == "--remind-done" && i+1 < argc) remindDone = stoi(argv[++i]);
        else if (arg == "--export-json" && i+1 < argc) json = argv[++i];
        else if (arg == "--export-csv" && i+1 < argc) csv = argv[++i];
        else if (arg == "--export-txt" && i+1 < argc) txt = argv[++i];
    }

    PregnancyDiary diary;
    if (!startDate.empty()) {
        diary.setStartDate(startDate);
    } else if (add) {
        if (dateStr.empty()) {
            cerr << "\033[31mДля добавления записи требуется --date\033[0m" << endl;
            return 1;
        }
        diary.addEntry(dateStr, week, note, symptoms);
    } else if (remind) {
        if (dateStr.empty() || note.empty()) {
            cerr << "\033[31mДля добавления напоминания требуются --date и --note\033[0m" << endl;
            return 1;
        }
        diary.addReminder(dateStr, note);
    } else if (list) {
        diary.listEntries();
    } else if (reminders) {
        diary.listReminders();
    } else if (remindDone != 0) {
        diary.markReminderDone(remindDone);
    } else if (!json.empty()) {
        diary.exportJSON(json);
    } else if (!csv.empty()) {
        diary.exportCSV(csv);
    } else if (!txt.empty()) {
        diary.exportTXT(txt);
    } else {
        cout << "Используйте --help для справки." << endl;
    }
    return 0;
}
