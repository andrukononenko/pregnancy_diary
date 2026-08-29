// pregnancy_diary.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strconv"
	"time"
)

const dataFile = "pregnancy.json"

type Entry struct {
	Date     string `json:"date"`
	Week     int    `json:"week"`
	Note     string `json:"note"`
	Symptoms string `json:"symptoms"`
	Created  string `json:"created"`
}

type Reminder struct {
	ID      int    `json:"id"`
	Date    string `json:"date"`
	Note    string `json:"note"`
	Done    bool   `json:"done"`
	Created string `json:"created"`
}

type DiaryData struct {
	StartDate string     `json:"start_date"`
	Entries   []Entry    `json:"entries"`
	Reminders []Reminder `json:"reminders"`
}

type PregnancyDiary struct {
	data           DiaryData
	nextReminderID int
}

func NewPregnancyDiary() *PregnancyDiary {
	d := &PregnancyDiary{data: DiaryData{Entries: []Entry{}, Reminders: []Reminder{}}}
	d.load()
	return d
}

func (d *PregnancyDiary) load() {
	file, err := os.ReadFile(dataFile)
	if err != nil {
		return
	}
	if err := json.Unmarshal(file, &d.data); err != nil {
		return
	}
	maxID := 0
	for _, r := range d.data.Reminders {
		if r.ID > maxID {
			maxID = r.ID
		}
	}
	d.nextReminderID = maxID + 1
}

func (d *PregnancyDiary) save() {
	data, _ := json.MarshalIndent(d.data, "", "  ")
	os.WriteFile(dataFile, data, 0644)
}

func (d *PregnancyDiary) setStartDate(dateStr string) {
	d.data.StartDate = dateStr
	d.save()
	fmt.Printf("\033[32mДата начала беременности установлена: %s\033[0m\n", dateStr)
}

func (d *PregnancyDiary) calculateWeek(dateStr string) *int {
	if d.data.StartDate == "" {
		return nil
	}
	start, _ := time.Parse("2006-01-02", d.data.StartDate)
	target, _ := time.Parse("2006-01-02", dateStr)
	delta := int(target.Sub(start).Hours() / 24)
	if delta < 0 {
		return nil
	}
	week := delta/7 + 1
	return &week
}

func (d *PregnancyDiary) addEntry(dateStr string, week int, note, symptoms string) {
	if week == 0 {
		w := d.calculateWeek(dateStr)
		if w == nil {
			fmt.Println("\033[31mНе удалось определить неделю. Укажите --week или --start-date\033[0m")
			return
		}
		week = *w
	}
	entry := Entry{
		Date:     dateStr,
		Week:     week,
		Note:     note,
		Symptoms: symptoms,
		Created:  time.Now().Format(time.RFC3339),
	}
	d.data.Entries = append(d.data.Entries, entry)
	d.save()
	fmt.Printf("\033[32mЗапись добавлена: %s, неделя %d\033[0m\n", dateStr, week)
}

func (d *PregnancyDiary) addReminder(dateStr, note string) {
	reminder := Reminder{
		ID:      d.nextReminderID,
		Date:    dateStr,
		Note:    note,
		Done:    false,
		Created: time.Now().Format(time.RFC3339),
	}
	d.nextReminderID++
	d.data.Reminders = append(d.data.Reminders, reminder)
	d.save()
	fmt.Printf("\033[32mНапоминание добавлено (ID: %d)\033[0m\n", reminder.ID)
}

func (d *PregnancyDiary) listEntries() {
	if len(d.data.Entries) == 0 {
		fmt.Println("\033[33mНет записей.\033[0m")
		return
	}
	fmt.Println("\033[36m📖 Дневник беременности:\033[0m")
	for _, e := range d.data.Entries {
		fmt.Printf("  %s | Неделя %d | %s | Симптомы: %s\n", e.Date, e.Week, e.Note, e.Symptoms)
	}
}

func (d *PregnancyDiary) listReminders() {
	if len(d.data.Reminders) == 0 {
		fmt.Println("\033[33mНет напоминаний.\033[0m")
		return
	}
	fmt.Println("\033[36m⏰ Напоминания:\033[0m")
	for _, r := range d.data.Reminders {
		status := "\033[32m✓\033[0m"
		if !r.Done {
			status = "\033[31m✗\033[0m"
		}
		fmt.Printf("  %d. %s %s - %s\n", r.ID, status, r.Date, r.Note)
	}
}

func (d *PregnancyDiary) markReminderDone(id int) {
	for i, r := range d.data.Reminders {
		if r.ID == id {
			d.data.Reminders[i].Done = true
			d.save()
			fmt.Printf("\033[32mНапоминание #%d отмечено как выполненное.\033[0m\n", id)
			return
		}
	}
	fmt.Printf("\033[31mНапоминание #%d не найдено.\033[0m\n", id)
}

func (d *PregnancyDiary) exportJSON(filename string) {
	data, _ := json.MarshalIndent(d.data, "", "  ")
	os.WriteFile(filename, data, 0644)
	fmt.Printf("\033[32mЭкспортировано в %s (JSON)\033[0m\n", filename)
}

func (d *PregnancyDiary) exportCSV(filename string) {
	f, _ := os.Create(filename)
	defer f.Close()
	w := csv.NewWriter(f)
	defer w.Flush()
	w.Write([]string{"date", "week", "note", "symptoms", "created"})
	for _, e := range d.data.Entries {
		w.Write([]string{e.Date, strconv.Itoa(e.Week), e.Note, e.Symptoms, e.Created})
	}
	fmt.Printf("\033[32mЭкспортировано в %s (CSV)\033[0m\n", filename)
}

func (d *PregnancyDiary) exportTXT(filename string) {
	f, _ := os.Create(filename)
	defer f.Close()
	fmt.Fprintf(f, "Дата начала беременности: %s\n\n", d.data.StartDate)
	fmt.Fprintln(f, "=== ЗАПИСИ ===")
	for _, e := range d.data.Entries {
		fmt.Fprintf(f, "%s | Неделя %d | %s | Симптомы: %s\n", e.Date, e.Week, e.Note, e.Symptoms)
	}
	fmt.Fprintln(f, "\n=== НАПОМИНАНИЯ ===")
	for _, r := range d.data.Reminders {
		done := "[ ]"
		if r.Done {
			done = "[x]"
		}
		fmt.Fprintf(f, "%d. %s - %s %s\n", r.ID, r.Date, r.Note, done)
	}
	fmt.Printf("\033[32mЭкспортировано в %s (TXT)\033[0m\n", filename)
}

func main() {
	var (
		add        bool
		dateStr    string
		week       int
		note       string
		symptoms   string
		startDate  string
		remind     bool
		list       bool
		reminders  bool
		remindDone int
		expJson    string
		expCsv     string
		expTxt     string
	)
	flag.BoolVar(&add, "add", false, "Добавить запись")
	flag.StringVar(&dateStr, "date", "", "Дата (YYYY-MM-DD)")
	flag.IntVar(&week, "week", 0, "Неделя беременности")
	flag.StringVar(&note, "note", "", "Заметка")
	flag.StringVar(&symptoms, "symptoms", "", "Симптомы")
	flag.StringVar(&startDate, "start-date", "", "Дата начала беременности")
	flag.BoolVar(&remind, "remind", false, "Добавить напоминание")
	flag.BoolVar(&list, "list", false, "Показать записи")
	flag.BoolVar(&reminders, "reminders", false, "Показать напоминания")
	flag.IntVar(&remindDone, "remind-done", 0, "Отметить напоминание как выполненное")
	flag.StringVar(&expJson, "export-json", "", "Экспорт в JSON")
	flag.StringVar(&expCsv, "export-csv", "", "Экспорт в CSV")
	flag.StringVar(&expTxt, "export-txt", "", "Экспорт в TXT")
	flag.Parse()

	diary := NewPregnancyDiary()

	if startDate != "" {
		diary.setStartDate(startDate)
	} else if add {
		if dateStr == "" {
			fmt.Println("\033[31mДля добавления записи требуется --date\033[0m")
			os.Exit(1)
		}
		diary.addEntry(dateStr, week, note, symptoms)
	} else if remind {
		if dateStr == "" || note == "" {
			fmt.Println("\033[31mДля добавления напоминания требуются --date и --note\033[0m")
			os.Exit(1)
		}
		diary.addReminder(dateStr, note)
	} else if list {
		diary.listEntries()
	} else if reminders {
		diary.listReminders()
	} else if remindDone != 0 {
		diary.markReminderDone(remindDone)
	} else if expJson != "" {
		diary.exportJSON(expJson)
	} else if expCsv != "" {
		diary.exportCSV(expCsv)
	} else if expTxt != "" {
		diary.exportTXT(expTxt)
	} else {
		fmt.Println("Используйте --help для справки.")
	}
}
