# Spring Batch CSV to DB and DB to CSV

[English version](#english-version) | [Русская версия](#russian-version)

---

## English Version {#english-version}

This project uses Spring Batch to import data from CSV into a database and export data from the database into CSV files. It uses an in-memory H2 database.

### Description

- Import data from the CSV file `csv-input-data/users.csv` into the `users` table.
- Export data from the `users` table to CSV files in the `csv-update-data` directory.
- Uses Spring Batch for job management.
- The H2 database runs in-memory.
- A scheduler triggers batch jobs every 10 seconds.
- Configuration is done through XML and Java configs.

### How to run

1. Clone the repository and open the project in your IDE.
2. Make sure the necessary dependencies (spring-context, spring-batch, h2) are on the classpath.
3. Run the application from the `main` method in the `SpringBatchXmlApplication` class.
4. Batch jobs will run automatically every 10 seconds (can be changed in the configuration).
5. To view the database, you can connect to the H2 console.

Output CSV files will appear in the `csv-update-data` folder.

### Important settings

- The time format for filenames and batch logic is set by `spring.time.format` — `yyyy-MM-dd_HH-mm-ss.SSS`.
- Input CSV path — `csv-input-data/users.csv`.
- Output directory path — `csv-update-data`.
- SQL queries for updating and selecting users are located in `sql/user_merge.sql` and `sql/user-select.sql`.

---

## Русская версия {#russian-version}

Проект на Spring Batch для импорта данных из CSV в базу данных и экспорта данных из базы в CSV. Используется H2 база данных в памяти.

### Описание

- Импорт данных из CSV файла `csv-input-data/users.csv` в таблицу `users`.
- Экспорт данных из таблицы `users` в CSV файлы в директорию `csv-update-data`.
- Используется Spring Batch для управления задачами.
- H2 база данных работает в памяти (in-memory).
- Планировщик запускает batch-задачи каждые 10 секунд.
- Конфигурация через XML и Java-конфиги.

### Как запустить

1. Склонируйте репозиторий и откройте проект в IDE.
2. Убедитесь, что в classpath добавлены необходимые зависимости, включая `spring-context`, `spring-batch`, `h2`.
3. Запустите приложение из метода `main` в классе `SpringBatchXmlApplication`.
4. Batch-задачи будут запускаться автоматически каждые 10 секунд (можно изменить в конфигурации).
5. Для просмотра базы данных можно подключиться к H2 консоли.

Выходные CSV файлы появятся в папке `csv-update-data`.

### Важные настройки

- Формат времени для имен файлов и логики batch задан в `spring.time.format` — `yyyy-MM-dd_HH-mm-ss.SSS`.
- Путь к входному CSV — `csv-input-data/users.csv`.
- Путь к выходной директории — `csv-update-data`.
- SQL запросы для обновления и выборки пользователей находятся в `sql/user_merge.sql` и `sql/user-select.sql`.
