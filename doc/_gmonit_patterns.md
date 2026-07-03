# Паттерны из gmonit → документация

Артефакт исследования 2026-07-02. Не для публикации.

Источники: два продакшен-проекта на di —
`~/projects/HyperSoftLab/gmonit/collector` (далее `collector/`) и
`~/projects/HyperSoftLab/gmonit-1c-agent/gmonit-1c-java-agent`
(далее `1c/`). Номера строк — на момент исследования, могут уехать.

Уже покрытое отфильтровано (базовый `update-key` + `conj` — в
туториале K, основы side dependencies и `->memoize` — в своих
рецептах).

## Приоритетные статьи (how-to)

### 1+2. Feature flags (включая bootstrap) — ✔ сделано

Реализовано одной статьёй:
`test/darkleaf/di/how_to/feature_flags_test.clj`
(«Feature flags» в навигации). Покрыто: флаги как карта, условные
registry-функции подсистем (`when → nil`), null-object подмена
выключенных компонентов, карта флагов в реестре — флаги доступны
и в рантайме, bootstrap-старт как разрыв цикла «реестры нужны до
старта, а флаги сами требуют di», скетч `prod-registries` с
вектором ключей и результатами bootstrap как константами.

Концы в gmonit:

- `flags`-компонент: `collector/src/gmonit/features.clj:6-110`;
- карта флагов в реестре: `collector/src/gmonit/system.clj:53`;
- условная registry-функция: `collector/src/gmonit/apm/core.clj:18-48`;
- null-object: `collector/src/gmonit/common/middlewares/geoip.clj:69-76`
  (+ `collector/src/gmonit/browser/core.clj:58-61`),
  `collector/src/gmonit/apm/settings.clj:196-216`,
  `collector/src/gmonit/distributed_tracing/core.clj:81`,
  `collector/src/gmonit/grafana/incident_manager/core.clj:29-32`;
- bootstrap: `collector/src/gmonit/system.clj:200-213`.

### 3. Fail-fast проверки при старте — ✔ сделано

Реализовано статьёй
`test/darkleaf/di/how_to/startup_checks_test.clj`
(«Startup checks» в навигации).

Из `collector/src/gmonit/requirements.clj`:

- микро-компоненты, чья единственная работа — бросить исключение
  (гард на deprecated env `DATOMIC_URI`, typo-guard
  `BROWSER_MONTINORING_ENABLED`, проверка версии ClickHouse),
  включаются через `di/add-side-dependency`: `requirements.clj:58-60`;
- `di/update-key` как ассерт после сборки: `redis-pooled` проверяет
  версию Redis и возвращает компонент нетронутым:
  `requirements.clj:40-64`.

### 4. Декоратор со своими зависимостями — ✔ сделано

Реализовано расширением туториала K
(`test/darkleaf/di/tutorial/k_composition_with_update_key_test.clj`):
абзац прозы перед заключением — все параметры `update-key`
фабрики, `f` включительно; примеры с `di/ref` (обёртка со своими
зависимостями) и `di/template` (собранная системой карта опций).

Самое неочевидное из 1c-agent:

- `(di/update-key `http-metric-exporter (di/ref `health/wrap-metric-exporter))` —
  обёртка резолвится из системы и получает свой `heartbeat` через di:
  `1c/src/gmonit/one_c/agent/health.clj:51-57`,
  применение `1c/src/gmonit/one_c/agent/otel.clj:405-411`.
- Там же: цепочки `update-key` на одном ключе — два независимых
  декоратора, каждый видит уже обёрнутое значение
  (`create-new-conn`, `http-metric-exporter`).
- Вариант с `comp` и prepend в вектор из newrelic-декоратора:
  `collector/src/gmonit/common/decorators/newrelic.clj:49-50`.

Туториал K показывает только обёртку-plain-fn — этот рецепт
расширяет его.

// наверное можно просто туториал расширить. просто у update-key все параметры могут быть фабриками
// часто это обычные объекты, но можно и на другой компонент сослаться, например, если он с состоянием

### 5. Документация env-переменных из метаданных

Свой тулинг поверх `di/inspect`:

- `^{:gmonit.env/doc "..."}` на биндингах dep-map:
  `collector/src/gmonit/common/adapters/clickhouse.clj`,
  `jetty.clj`, `curator.clj`, `connect.clj`,
  `collector/src/gmonit/browser/root.clj`;
- `collector/dev/env_doc.clj` обходит вывод `di/inspect`, находит
  `::di/variable`, читает `arglists` var-а, парсит деструктуринг и
  генерирует markdown-таблицу всех env-переменных системы.

Статья показывает, что inspect открывает дорогу к собственному
конфигурационному тулингу. Хороший парный материал к reference
«Inspect».

// там код вроде бы не очень. можно наверное код не приводить в статье, а просто идею передать
// еще нужно объяснить, что метаданные мы можем поставить не на ключ, а на биндинг. Т.к. ключи и строки не могут хранить метаданные.

### 6. Подменяемые адаптеры для тестов (clock/random)

Пара `registry`/`test-registry` в одном ns:

- `collector/src/gmonit/common/adapters/clock.clj` —
  `Clock/systemUTC` vs `Clock/fixed`;
- `collector/src/gmonit/common/adapters/random.clj` —
  `SecureRandom.` vs `reify RandomGenerator`;
- подключение: `collector/src/gmonit/system.clj:153-154`.

Нюанс: ключи названы по Java-интерфейсам (`:java.time/Clock`,
`:java.util.random/RandomGenerator`). Ложится рядом со статьёй
«Reusing components between tests».

// пока сложно сказать, нужно ли. покажи пример

## Дополнения к существующим страницам

### side_dependencies (how-to)

- Кейс «зависимость выключенного компонента»: воркер выключен
  флагом, но его зависимость нужна другим — side-dependency на неё:
  `collector/src/gmonit/system.clj:146-149`.

  // я не очень понял, нужно смотреть

- Анти-паттерн `{::syms [...]}` вместо `add-side-dependency`:
  живой авторский `#_"TODO: rewrite to di/add-side-dependency"` и
  подавление `:unused-binding`:
  `1c/src/gmonit/one_c/agent/otel.clj:353-361`. Прямое свидетельство,
  что страница недостаточно мотивирует.

  // это довольно старый код, страницы еще не было

- Примеры schema-миграций через side-dependency:
  `collector/src/gmonit/otlp/core.clj:127-128`,
  `collector/src/gmonit/apm/core.clj:46-48`.

// подумай хорошо, стоит ли столько деталей выкладывать

### Reference «Inspect»

Секция «ассерты на граф в тестах»: фильтрация `di/inspect` по
`:dependencies` — проверка, что `basic-auth/wrap-auth` достижим
только из `grafana/middleware`:
`collector/test/gmonit/system_test.clj:13-28`.

// это определенно не reference, это отдельная статья про "как тестировать систему с фича-флагами"

### Reference «The middleware argument» (или страница о протоколах)

`instrument-service` — единственный в кодовой базе собственный
registry-middleware поверх `p/Factory`: ветвится по
`(-> factory p/description ::di/kind)`, оборачивает `build`
NewRelic-трейсом с ключом компонента в имени:
`collector/src/gmonit/common/utils/di.clj:1-21`, применение в
`collector/src/gmonit/common/decorators/newrelic.clj`.

// можно добавить, но я бы хотел видеть правки, чтобы решить, надо ли

### How-to «Multiple systems»

Вложенная система как компонент: `mcp-query` делает `di/start`
внутри конструктора (второй ClickHouse-коннект с read-only кредами)
и вешает `{::di/stop di/stop}`:
`collector/src/gmonit/mcp/core.clj:22-51`. В коде есть комментарий,
что это workaround дизайна clickhouse-адаптера — подать как приём,
не как идеал.

// не рассказывать про это

### How-to «Reusing components between tests»

Возможная секция про `def+start`: макрос, который стартует вектор
ключей на memoized-реестре и def-ает их в ns тестов; в докстринге —
предупреждение про утёкший datomic-коннект, повесивший CI:
`collector/test/gmonit_test/utils.clj:63-74`.

// нафиг, это наш локальный прикол

## Пачка в Tips

- `:as-alias` для namespace-ов, у которых берёшь только ключи:
  `collector/src/gmonit/system.clj:9-18`,
  `collector/src/gmonit/features.clj` (даже di подключён через
  `:as-alias` ради метаданных).

// ага, и даже я бы отдельным пунктом указал про то, что удобнее даже не рекваирить, а просто полные ключи писать

- EDN-файл как слой конфигурации:
  `(-> "env_test.edn" slurp edn/read-string)` последним элементом
  реестра: `collector/src/gmonit/system.clj:118,191`, сами файлы
  `collector/env_dev.edn`, `collector/env_test.edn`.

// супер пример, обзательно нужен

- Рантайм-значение (CLI-аргумент) через keyword-ключ в карте:
  `{:one-c-config-cmd-arg one-c-config}` —
  `1c/src/gmonit/one_c/agent/otel.clj:401-417`, потребитель
  `1c/src/gmonit/one_c/connector.clj:33-42`.

// хз что это

- `@(di/start `config {...})` в REPL — глянуть значение одного
  компонента: comment-блок `1c/src/gmonit/one_c/agent/otel.clj:423-451`.

// хз что это

- `di/derive` для одиночного env с нетривиальным парсингом
  (PEM → SSL factory): `collector/src/gmonit/common/adapters/clickhouse.clj:49-51`.
  Перед написанием свериться с туториалом J («A typed env var»),
  чтобы не дублировать.
- `di/with-open` управляет и обычным `AutoCloseable`, и системой в
  одном биндинге: `1c/test/gmonit/one_c/agent/otel_test.clj:173-188`.

// ну как бы да. with-open это расширение, и все это можно делать

## Формы `::di/stop` — возможно, отдельная reference-страница «Stopping»

Весь спектр из продакшена:

- method reference на классе: `Server/.stop`
  (`collector/src/gmonit/common/adapters/jetty.clj:34`),
  `JedisPooled/.close` (`redis.clj:49`),
  `CuratorFramework/.close` (`curator.clj:32`),
  `AgentAdminConnector/.shutdown`
  (`1c/src/gmonit/one_c/connector.clj:22-26` — заодно пример
  `::di/stop` без явного `::di/kind`);
- method reference на интерфейсе: `AutoCloseable/.close`
  (`1c/src/gmonit/one_c/agent/otel.clj:337-344`);
- именованная функция: `jdk-http/stop`
  (`1c/src/gmonit/one_c/agent/health.clj:41-49`),
  `pool/stop` (`collector/src/gmonit/browser/middlewares/user_agent.clj:81`);
- inline-lambda c graceful shutdown:
  `(fn [es] (.shutdown es) (.awaitTermination es 5 SECONDS))` —
  `collector/src/gmonit/grafana/incident_manager/rca/service.clj:119-121`;
- `di/stop` для вложенной системы:
  `collector/src/gmonit/mcp/core.clj:23-24`.

// в доке уже есть про это. наверное не нужно

## Интеграционный тест с Java-стабами (кандидат в how-to, ниже приоритетом)

`1c/test/gmonit/one_c/agent/otel_test.clj:172-193` совмещает сразу
три приёма: подмена компонента функцией-стабом прямо по symbol-ключу
в карте (`'gmonit.one-c.connector/create-new-conn create-new-conn-mock`,
без `di/ref`), `reify` реальных Java-интерфейсов
(`InMemoryMetricReader`), `di/update-key` с di-инжектируемой
обёрткой-ловушкой исключений.

// хз, на последок
