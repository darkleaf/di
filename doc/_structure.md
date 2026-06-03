# Новая структура туториала — v6

Артефакт Этапа 1. Не для публикации.
v6 — точечные правки в процессе Этапа 2.

Изменения v5 → v6:

- **Starting many keys** возвращён в Tutorial (после Environment
  variables, перед Wiring inside data). Причина: Wiring inside
  data использует мульти-ключ для теста, без введения это
  спотыкает читателя.

Изменения v4 → v5:

- **Multi-arity services** возвращён в How-to (как было предложено
  изначально).

---

## Что я нашёл по твоим указаниям из v2

**Docstrings в `darkleaf.di.core`.** Я их прочитал. Несколько
наблюдений для будущей работы:

- Docstring у `start` уже содержит 9-строчный код-пример со
  стеком middlewares — хороший материал для главы 5 (Registries)
  или 10 (Composition).
- Docstring у `Factory` (`protocols.clj`) очень крепкий, его можно
  взять почти буквально для Reference-страницы.
- `update-key`, `env-parsing`, `ns-publics`, `log`, `add-side-dependency`,
  `inspect` — все documented. В Этапе 2 не будем выдумывать
  формулировки с нуля, а будем опираться на эти docstrings.

**Тесты для Factory.** `dependency_types_test.clj` — идеальный
короткий пример кастомной фабрики (factory с required vs optional,
detection циклов). Эту мини-фабрику использую как канонический
пример в Reference-странице Factory.

**Registry как middleware на registries.** Подтверждаю твоё
наблюдение. В `start` registry — это функция `key -> Factory`,
а map это её частный случай. Это меняет структуру (см. ниже).

---

## Принципы (зафиксировано)

1. **Tutorial и How-to разделены.** Tutorial линейно; How-to —
   независимые рецепты под задачу.
2. **Главная идея библиотеки — композиция через `update-key`.**
3. **Stop — упоминается рано, продолжается в конце.**
4. **Interactive redef — отдельная глава туториала.**
5. **Reddit-Motivation — основа «Why DI?» (перепишу своими словами, без цитат).**
6. **«Modules» — не наш термин.** Глава 10 называется
   «Composition with `update-key`».
7. **Реальный пример (gmonit) — деобфусцированные точечные вставки,
   не сквозной пример.**
8. **Граф концептов не разрываем:** registry упоминается рано,
   формально вводится в гл.5, как middleware-chain раскрывается
   в гл.10.

### Про math-style имена (`a`, `b`, `c`)

Ты спросил моё мнение. Вот оно:

В сообществе Clojure документация в основном использует
реалистичные имена (`:database`, `:web-server`, `:scheduler`) —
это даёт читателю якорь «это всё для настоящих приложений».
С другой стороны, в туториале, где фокус на **механике связывания**,
абстрактные имена убирают шум: читатель не отвлекается на то,
что такое scheduler, и смотрит как именно `a` зависит от `b`.

Сочетание, которое мы уже зафиксировали — реалистичные имена
в **главе 1 «Your first system»** (чтобы тимлид не споткнулся
и читатель видел «это для настоящих апп»), и `a`/`b`/`c`
в остальных главах — стандартный приём в учебных текстах
(«concretize then abstract»). Это нормально и осознанно.

Ничего лишнего изобретать не нужно. Твои буквы остаются.

> автор:

---

## Верхнеуровневое дерево

```
- Why DI?              ← новая глава
- Tutorial             ← линейно, 12 глав
- How-to               ← независимые рецепты
- Reference            ← Factory, Inspect, Middleware types
- Example app          ← без изменений
- Integrant vs DI      ← без изменений
```

> автор:

---

## Tutorial — 12 глав

| #  | Глава                       | Источник                             |
|----|-----------------------------|--------------------------------------|
| 1  | Your first system           | `a_intro` (без interactive redef)    |
| 2  | Dependencies                | `b_dependencies`                     |
| 3  | Stopping components         | `c_stop`                             |
| 4  | Interactive development     | секция из `a_intro` + новый материал |
| 5  | Registries                  | `l_registries`                       |
| 6  | Abstractions                | `m_abstractions` (расширяем)         |
| 7  | Environment variables       | `n_env`                              |
| 8  | Starting many keys          | `q_starting_many_keys`               |
| 9  | Wiring inside data          | `o_data_dsl` (gmonit-пример)         |
| 10 | Transforming values         | `p_derive`                           |
| 11 | Composition with `update-key`| `x_update_key` (повышаем)            |
| 12 | Graceful failures           | `y_graceful_stop`                    |


### Логика прогрессии

- **1–2:** запустили систему, объявили зависимости.
- **3:** stop. По умолчанию ничего не нужно; `::di/stop` цепляется
  метаданными только когда он нужен. К концу гл.3 у читателя —
  полная картина статической системы.
- **4:** runtime story — REPL workflow, redef сервисов.
  «Ты не обязан перезапускать систему, чтобы итерироваться».
  Stuart Sierra "Reloaded Workflow" — ссылка.
- **5:** регистры. Представляем как map ключей в значения и стек
  map'ов с last-wins. Forward-link на Reference (Middleware
  types) — там общая форма «registry = функция key→Factory» и
  список всех middlewares.
- **6–7:** отвязка от vars (keyword) и от окружения (string).
- **8:** запуск нескольких ключей за раз — Indexed/ILookup root,
  `di/with-open`. Нужно до Wiring inside data, чтобы тест ref+template
  можно было записать через сравнение equality.
- **9–10:** работа с данными — встраиваем deps в data-DSL,
  трансформируем значения.
- **11:** кульминация. `update-key` как главный инструмент
  композиции: декорирование built value + расширение shared
  коллекции через модули. Это то, ради чего был весь путь.
- **12:** что происходит при сбое старта.

---

## How-to — 8 рецептов

Порядок неважен; читаются независимо.

| Рецепт                          | Источник                                |
|---------------------------------|------------------------------------------|
| Side dependencies (миграции)    | `x_add_side_dependency`                  |
| Multimethod services            | `r_multimethods`                         |
| Multi-arity services            | `y_multi_arity_service`                  |
| Multiple systems                | `z_multi_system`                         |
| Multiple of the same thing      | `z_two_databases`                        |
| All public vars as a component  | `x_ns_publics`                           |
| Logging system lifecycle        | `x_log`                                  |
| Visualizing your system         | новая, на основе `inspect` + Graphviz    |

- **Starting many keys** — описание расширяем: это не только
  тесты, а любая система с несколькими корнями (webserver +
  миграции + воркеры).
- **All public vars as a component** — выбранное название для
  рецепта про `di/ns-publics`. Точно отражает что фактически
  делает функция: собирает один компонент (map var-name → value)
  из всех public vars неймспейса.
- **Visualizing your system** — новая страница. Берём пример из
  PR #30. Показываем как пропустить вывод `di/inspect` через
  Graphviz и получить картинку графа. Ты сам отметил: inspect
  важнее логирования, должен быть видимым.

> автор:

мне нравится  «A namespace as a system»

---

## Reference — 3 страницы

| Страница            | Источник                                   |
|---------------------|---------------------------------------------|
| The Factory protocol| Docstring `protocols.clj` + `dependency_types_test.clj` |
| Inspect             | `x_inspect`                                 |
| Middleware types    | Из docstrings `core.clj` (новая)            |

- **Factory protocol** — docstring и тест есть; собираем страницу
  «когда и как реализовать кастомную фабрику».
- **Inspect** — текущая глава `x_inspect` (377 строк перечисления
  форм `:description`). Текст уже неплохой — переносим в Reference,
  меняется только роль и навигация.
- **Middleware types** — отвечает на твою фразу «можно даже
  в референс добавить типы мидлвар». Перечисляем формы middleware
  (function `registry -> key -> Factory`, map, sequence, nil) и
  встроенные middlewares: `update-key`, `add-side-dependency`,
  `env-parsing`, `ns-publics`, `log`. Каждая — две-три строки
  с одной ссылкой на How-to/Tutorial где она объясняется подробно.

> автор:

---

## Что меняется относительно текущего

**Добавляется:**
- Новая глава Tutorial: **Why DI?**.
- Новая глава Tutorial: **Interactive development**.
- Новая глава Tutorial: **Composition with `update-key`**
  (повышение из Advanced).
- Tutorial: **Multi-arity** повышается из Advanced.
- Новая How-to: **Visualizing your system**.
- Новый раздел: **Reference** (3 страницы).

**Переезжает:**
- `Inspect` Advanced → Reference.
- `Two databases` Advanced → How-to (Factory выносим в Reference).
- Остальные Advanced → How-to.

**Не сливаем:**
- `Custom stop` (гл.4) и `Graceful failures` (гл.12) — разные
  главы, разные позиции.

**Не трогаем:**
- Example app, Integrant vs DI, Readme.

> автор:

---

## Закрытые вопросы (для прозрачности)

- **V1.** Название гл.10 = «Composition with `update-key`».
- **V2.** Why DI? — переписываю Motivation своими словами, без цитат.
- **V3.** Деобфускацию `gmonit.infra` делаю в Этапе 2, когда
  возьмёмся за гл.8.
- **V4.** Renames файлов: `doc/tutorial/01_..` / `doc/how_to/<name>` /
  `doc/reference/<name>`. Подтверждено.

> автор:

---

## Следующий шаг — Этап 2

Порядок переписывания:

1. **Why DI?** — новая глава, нет давления существующего текста.
2. **Your first system** — самая видимая, на свежих принципах.
3. Дальше по порядку Tutorial.
4. How-to и Reference — параллельно по мере необходимости.

**Артефакт одной сессии:** одна глава, один diff на одном файле.
Возможные исключения — мелкие правки cljdoc.edn и связанные
переименования.

**Чек-лист для каждой главы** (фиксируется в Этапе 2 в момент
первой переработки):

1. Сначала ответить «зачем эта глава» одним предложением.
2. Минимальный код-пример, который иллюстрирует ровно одну
   мысль главы.
3. Текст вокруг кода — что читатель видит, что узнаёт,
   что попробует дальше.
4. Cross-ссылки наружу: где предыдущая мысль, где следующая.
5. Грамматическая вычитка англоязычного текста.

> автор:
