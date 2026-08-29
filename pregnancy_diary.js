#!/usr/bin/env node
// pregnancy_diary.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

const DATA_FILE = 'pregnancy.json';

class Entry {
    constructor(date, week, note = '', symptoms = '', created = null) {
        this.date = date;
        this.week = week;
        this.note = note;
        this.symptoms = symptoms;
        this.created = created || new Date().toISOString();
    }
}

class Reminder {
    constructor(id, date, note, done = false, created = null) {
        this.id = id;
        this.date = date;
        this.note = note;
        this.done = done;
        this.created = created || new Date().toISOString();
    }
}

class PregnancyDiary {
    constructor() {
        this.entries = [];
        this.reminders = [];
        this.startDate = null;
        this.nextReminderId = 1;
        this.load();
    }

    load() {
        try {
            if (fs.existsSync(DATA_FILE)) {
                const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
                this.startDate = data.start_date || null;
                this.entries = (data.entries || []).map(e => new Entry(e.date, e.week, e.note, e.symptoms, e.created));
                this.reminders = (data.reminders || []).map(r => new Reminder(r.id, r.date, r.note, r.done, r.created));
                this.nextReminderId = this.reminders.reduce((max, r) => Math.max(max, r.id), 0) + 1;
            }
        } catch (e) {
            this.entries = [];
            this.reminders = [];
        }
    }

    save() {
        fs.writeFileSync(DATA_FILE, JSON.stringify({
            start_date: this.startDate,
            entries: this.entries.map(e => ({ date: e.date, week: e.week, note: e.note, symptoms: e.symptoms, created: e.created })),
            reminders: this.reminders.map(r => ({ id: r.id, date: r.date, note: r.note, done: r.done, created: r.created }))
        }, null, 2));
    }

    setStartDate(dateStr) {
        this.startDate = dateStr;
        this.save();
        console.log(chalk.green(`Дата начала беременности установлена: ${dateStr}`));
    }

    calculateWeek(dateStr) {
        if (!this.startDate) return null;
        const start = new Date(this.startDate + 'T00:00:00');
        const target = new Date(dateStr + 'T00:00:00');
        const delta = Math.floor((target - start) / (1000 * 60 * 60 * 24));
        if (delta < 0) return null;
        return Math.floor(delta / 7) + 1;
    }

    addEntry(dateStr, week, note = '', symptoms = '') {
        if (week === undefined || week === null) {
            week = this.calculateWeek(dateStr);
            if (week === null) {
                console.log(chalk.red('Не удалось определить неделю. Укажите --week или --start-date'));
                return;
            }
        }
        this.entries.push(new Entry(dateStr, week, note, symptoms));
        this.save();
        console.log(chalk.green(`Запись добавлена: ${dateStr}, неделя ${week}`));
    }

    addReminder(dateStr, note) {
        const reminder = new Reminder(this.nextReminderId++, dateStr, note);
        this.reminders.push(reminder);
        this.save();
        console.log(chalk.green(`Напоминание добавлено (ID: ${reminder.id})`));
    }

    listEntries() {
        if (this.entries.length === 0) {
            console.log(chalk.yellow('Нет записей.'));
            return;
        }
        console.log(chalk.cyan('📖 Дневник беременности:'));
        this.entries.sort((a, b) => a.date.localeCompare(b.date));
        for (const e of this.entries) {
            console.log(`  ${e.date} | Неделя ${e.week} | ${e.note} | Симптомы: ${e.symptoms}`);
        }
    }

    listReminders() {
        if (this.reminders.length === 0) {
            console.log(chalk.yellow('Нет напоминаний.'));
            return;
        }
        console.log(chalk.cyan('⏰ Напоминания:'));
        this.reminders.sort((a, b) => a.date.localeCompare(b.date));
        for (const r of this.reminders) {
            const status = r.done ? chalk.green('✓') : chalk.red('✗');
            console.log(`  ${r.id}. ${status} ${r.date} - ${r.note}`);
        }
    }

    markReminderDone(id) {
        const r = this.reminders.find(r => r.id === id);
        if (!r) {
            console.log(chalk.red(`Напоминание #${id} не найдено.`));
            return;
        }
        r.done = true;
        this.save();
        console.log(chalk.green(`Напоминание #${id} отмечено как выполненное.`));
    }

    exportJson(filename) {
        const data = {
            start_date: this.startDate,
            entries: this.entries.map(e => ({ date: e.date, week: e.week, note: e.note, symptoms: e.symptoms, created: e.created })),
            reminders: this.reminders.map(r => ({ id: r.id, date: r.date, note: r.note, done: r.done, created: r.created }))
        };
        fs.writeFileSync(filename, JSON.stringify(data, null, 2));
        console.log(chalk.green(`Экспортировано в ${filename} (JSON)`));
    }

    exportCsv(filename) {
        const header = 'date,week,note,symptoms,created\n';
        const rows = this.entries.map(e => `${e.date},${e.week},${e.note},${e.symptoms},${e.created}`).join('\n');
        fs.writeFileSync(filename, header + rows);
        console.log(chalk.green(`Экспортировано в ${filename} (CSV)`));
    }

    exportTxt(filename) {
        let content = `Дата начала беременности: ${this.startDate}\n\n`;
        content += '=== ЗАПИСИ ===\n';
        for (const e of this.entries) {
            content += `${e.date} | Неделя ${e.week} | ${e.note} | Симптомы: ${e.symptoms}\n`;
        }
        content += '\n=== НАПОМИНАНИЯ ===\n';
        for (const r of this.reminders) {
            content += `${r.id}. ${r.date} - ${r.note} ${r.done ? '[x]' : '[ ]'}\n`;
        }
        fs.writeFileSync(filename, content);
        console.log(chalk.green(`Экспортировано в ${filename} (TXT)`));
    }
}

program
    .option('--add', 'Добавить запись')
    .option('--date <date>', 'Дата (YYYY-MM-DD)')
    .option('--week <number>', 'Неделя беременности', parseInt)
    .option('--note <text>', 'Заметка')
    .option('--symptoms <text>', 'Симптомы')
    .option('--start-date <date>', 'Дата начала беременности')
    .option('--remind', 'Добавить напоминание')
    .option('--list', 'Показать записи')
    .option('--reminders', 'Показать напоминания')
    .option('--remind-done <id>', 'Отметить напоминание как выполненное', parseInt)
    .option('--export-json <file>', 'Экспорт в JSON')
    .option('--export-csv <file>', 'Экспорт в CSV')
    .option('--export-txt <file>', 'Экспорт в TXT')
    .parse(process.argv);

const opts = program.opts();
const diary = new PregnancyDiary();

if (opts.startDate) {
    diary.setStartDate(opts.startDate);
} else if (opts.add) {
    if (!opts.date) {
        console.error(chalk.red('Для добавления записи требуется --date'));
        process.exit(1);
    }
    diary.addEntry(opts.date, opts.week, opts.note || '', opts.symptoms || '');
} else if (opts.remind) {
    if (!opts.date || !opts.note) {
        console.error(chalk.red('Для добавления напоминания требуются --date и --note'));
        process.exit(1);
    }
    diary.addReminder(opts.date, opts.note);
} else if (opts.list) {
    diary.listEntries();
} else if (opts.reminders) {
    diary.listReminders();
} else if (opts.remindDone !== undefined) {
    diary.markReminderDone(opts.remindDone);
} else if (opts.exportJson) {
    diary.exportJson(opts.exportJson);
} else if (opts.exportCsv) {
    diary.exportCsv(opts.exportCsv);
} else if (opts.exportTxt) {
    diary.exportTxt(opts.exportTxt);
} else {
    program.help();
}
