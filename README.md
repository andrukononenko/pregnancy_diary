## Дневник беременности (напоминания)

Многоязычное консольное приложение для ведения дневника беременности с системой напоминаний.  
Позволяет записывать заметки по дням, отслеживать срок беременности, устанавливать напоминания о важных событиях (визиты к врачу, УЗИ, анализы) и экспортировать данные.

## Особенности
- Добавление записей с датой, сроком беременности (неделя + день), заметками и симптомами.
- Автоматический расчёт срока беременности от указанной даты начала (или предполагаемой даты родов).
- Отображение текущей недели и триместра.
- Установка напоминаний с датой и описанием.
- Просмотр всех записей дневника.
- Просмотр предстоящих напоминаний.
- Отметка напоминаний как выполненных.
- Цветной вывод в терминале (где поддерживается).
- Экспорт данных в JSON, CSV и TXT.
- Хранение данных в локальном JSON-файле.
- Поддержка аргументов командной строки для автоматизации.

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости.

### Запуск на разных языках

1. **Python**  
   Установка: `pip install colorama` (опционально).  
   Запуск: `python pregnancy_diary.py --add --date 2026-08-29 --week 12 --note "Первое УЗИ"`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node pregnancy_diary.js --add --date 2026-08-29 --week 12 --note "Первое УЗИ"`

3. **Go**  
   Запуск: `go run pregnancy_diary.go --add --date 2026-08-29 --week 12 --note "Первое УЗИ"`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run -- --add --date 2026-08-29 --week 12 --note "Первое УЗИ"`

5. **Java**  
   Сборка: `javac -cp gson.jar PregnancyDiary.java`  
   Запуск: `java -cp .;gson.jar PregnancyDiary --add --date 2026-08-29 --week 12`

6. **C# (.NET Core)**  
   Установка: `dotnet add package Newtonsoft.Json`  
   Запуск: `dotnet run -- --add --date 2026-08-29 --week 12`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o pregnancy_diary pregnancy_diary.cpp -ljsoncpp`  
   Запуск: `./pregnancy_diary --add --date 2026-08-29 --week 12`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar PregnancyDiary.kt`  
   Запуск: `kotlin -cp .;gson.jar PregnancyDiaryKt --add --date 2026-08-29 --week 12`

## Использование

Общие аргументы командной строки (везде, где поддерживается):

- `--add` – добавить запись в дневник (требует `--date` и опционально `--week`, `--note`, `--symptoms`).
- `--date <YYYY-MM-DD>` – дата записи (обязательно с `--add` и `--remind`).
- `--week <число>` – неделя беременности (если не указана, вычисляется автоматически при наличии `--start-date`).
- `--note <текст>` – заметка к записи.
- `--symptoms <текст>` – симптомы (опционально).
- `--start-date <YYYY-MM-DD>` – дата начала беременности (первый день последних месячных) для автоматического расчёта срока.
- `--remind` – добавить напоминание (требует `--date` и `--note`).
- `--list` – показать все записи дневника.
- `--reminders` – показать все предстоящие напоминания.
- `--remind-done <ID>` – отметить напоминание как выполненное.
- `--export-json <файл>` – экспорт в JSON.
- `--export-csv <файл>` – экспорт в CSV.
- `--export-txt <файл>` – экспорт в TXT.
- `--help` – справка.

Пример (Python):
```bash
python pregnancy_diary.py --start-date 2026-06-01
python pregnancy_diary.py --add --date 2026-08-29 --note "Чувствую шевеления" --symptoms "легкая тошнота"
python pregnancy_diary.py --remind --date 2026-09-15 --note "Визит к врачу"
python pregnancy_diary.py --list
python pregnancy_diary.py --reminders
Структура репозитория
text
/
├── README.md
├── pregnancy_diary.py
├── pregnancy_diary.js
├── pregnancy_diary.go
├── pregnancy_diary.rs
├── PregnancyDiary.java
├── PregnancyDiary.cs
├── pregnancy_diary.cpp
└── PregnancyDiary.kt
Лицензия
MIT
