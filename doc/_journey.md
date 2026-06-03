# Карта читателя по туториалу — черновик

Рабочий артефакт Этапа 0. Не для публикации.
Формат для каждой главы: вход → выход → комментарий.

Метки: 🟢 нормально / 🟡 есть проблемы / 🔴 нужна серьёзная переработка.

Оставляй реакции под каждым пунктом строкой `> автор:`, либо
правь/вычёркивай сам текст, если что-то описано неточно. Когда
закончишь — переходим к Этапу 1 (новая структура + `cljdoc.edn`).

---

## Base (текущий порядок)

### 1. Intro — `a_intro_test.clj`

- **Вход:** базовый Clojure.
- **Выход:** умеет вызвать `di/start`; знает `root`, `di/stop`,
  `with-open`; отличает `:component` от service; видел зависимость-
  плейсхолдер; видел interactive redef.
- 🔴 **Проблема:** глава учит 5+ концептам сразу. Hook отсутствует
  («Let's start. In this chapter I'll show you how to deal with
  components» — это не hook). Interactive redef — отдельный концепт,
  не должен ехать в Intro.

> автор:
одна из проблем - мой тимлид говорит, что a, b, c, d это абстрактные шутки и ему не удобно.
но я инженер-математик по образованию и мне ок, я так показываю суть отношений.

почему там много разных концепций, тут все базовые концепции.
и если выделять по одной, то имхо будет мало текста плюс много воды

### 2. Dependencies — `b_dependencies_test.clj`

- **Вход:** знает, что такое component.
- **Выход:** пишет зависимости через assoc destructuring; понимает
  `:or`, `:as deps`; видел ошибку missing dep; **уже использует
  registry** для подмены deps.
- 🟡 **Проблема:** глава молча использует `di/start` с map-аргументом
  (registry), но термин «registry» не вводится. Это путает, когда
  читатель доходит до главы 4 (Registries).

> автор:
то, что не вводится реестр, это осознанно, иначе как этот граф разорвать?
я решил, что лучше потом объяснить

### 3. Stop — `c_stop_test.clj`

- **Вход:** знает про components.
- **Выход:** умеет навесить `::di/stop`; знает `memfn`.
- 🟡 **Проблема:** очень короткая, нет «когда тебе это понадобится».
  Stop логически должен идти сразу после Intro, но Intro уже использует
  `with-open`. Возможно, слить start/stop lifecycle в одну главу.

> автор:
про логичность - я хз. вопрос опять про очередность. и как граф сделать линейным повествованием.
если бы я сделал иначе тогда, сейчас ты бы снова сказал, а почему у тебя тут не stop, а зависимости.

### 4. Registries — `l_registries_test.clj`

- **Вход:** уже видел, как map передаётся в `di/start`.
- **Выход:** знает термин «registry»; знает, что регистров может быть
  несколько и работает «last wins»; знает про seqable.
- 🟡 **Проблема:** наполовину дублирует главу 2. Сама концепция
  registry должна вводиться *в* главе 2.

> автор:

### 5. Abstractions — `m_abstractions_test.clj`

- **Вход:** знает про symbol-ключи.
- **Выход:** использует keyword-ключи для отвязки от vars.
- 🔴 **Проблема:** это **главная фича DI** (отвязка от конкретных
  vars → подменяемые реализации), но глава самая короткая и
  hand-wavy. «Later in the main function you will be able to bind
  all parts» — не объясняет, почему это важно. Нет руководства
  «symbol vs keyword: когда что брать».

> автор:
я бы не назвал ее главной фичей. главное это про update-key.

когда какое брать - это есть в docstring.
но соглашусь.

### 6. Env — `n_env_test.clj`

- **Вход:** знает разные виды ключей.
- **Выход:** читает env-переменные через string-ключи; знает
  `di/env-parsing` middleware; знает синтаксис `:env.long/X`.
- 🟢 Содержательно нормально. Hook можно усилить
  («подключаем систему к окружению»).

> автор:
видимо тут перевод кривой, я не понимаю смысл «подключаем систему к окружению»

### 7. Data DSL — `o_data_dsl_test.clj`

- **Вход:** знает symbol / keyword / string ключи.
- **Выход:** пишет `di/template`, `di/ref`, `di/opt-ref`.
- 🟡 **Проблема:** 27 строк, hook есть («data-DSLs like reitit»),
  но пример абстрактный — читатель не видит реалистичную reitit-
  конфигурацию. Концепт мощный, недопродан.

> автор:
можешь посмотреть в gmonit/collector, в проетах лежит

### 8. Derive — `p_derive_test.clj`

- **Вход:** знает env-ключи и templates.
- **Выход:** применяет функцию к собранному значению.
- 🟡 **Проблема:** мотивация «components may have a complex
  structure» расплывчатая. Реалистичный пример (parse env var)
  есть, но он крошечный.

> автор:

это было в эпоху до LLM, и мне сложно писать текст

### 9. Starting many keys — `q_starting_many_keys_test.clj`

- **Вход:** знает `di/start` и `with-open`.
- **Выход:** стартует вектор/map ключей; использует `di/with-open`
  с destructuring.
- 🟢 Технически нормально, но это скорее **how-to/recipe**,
  а не шаг прогрессирующего туториала.

> автор:

это важная часть при написании тестов.
и важно, если нужно стартануть не только веб-сервер

### 10. Multimethods — `r_multimethods_test.clj`

- **Вход:** знает services и deps.
- **Выход:** навешивает `::di/deps` на defmulti.
- 🟡 Узкая ниша. Кандидат на переезд в How-to.

> автор:
в gmonit это применяется, но узкая ниша - да.

---

## Advanced (текущий порядок)

### 11. Add a side dependency — `x_add_side_dependency_test.clj`

- **Вход:** знает, что бывают middlewares.
- **Выход:** подключает миграции / init.
- 🟢 Чёткий use-case. **Это recipe.**

> автор:
с этой фичей бывают проблемы, но можно наверное не распространяться, вроде все уладили

### 12. Update key — `x_update_key_test.clj`

- **Вход:** знает components.
- **Выход:** понимает decorator pattern и cross-module composition.
- 🟢 **Хорошо написано** (свежий rewrite). Это эталон voice.
  Но концепт настолько фундаментальный для DI, что должен быть
  в Base, а не в Advanced.

> автор:
хз, про хорошо написан, это LLM сгенеренный
концепт важный, но это тема со звездочкой так-то и я не хотел давать его сразу

### 13. Log — `x_log_test.clj`

- 🟢 Хорошо написано (свежее). Это recipe.

> автор:

### 14. Inspect — `x_inspect_test.clj`

- 🔴 **Это reference, а не tutorial.** 377 строк, перечислены все
  формы `:description`. Полезно, но не как глава туториала. Должно
  стать reference-страницей со ссылкой из туториала.

> автор:
наверное да

### 15. Graceful stop — `y_graceful_stop_test.clj`

- 🟡 Описание поведения (что происходит при сбое), а не how-to.
  Ближе к explanation/reference.

> автор:

### 16. Multi arity service — `y_multi_arity_service_test.clj`

- 🟡 Узкая ниша. Recipe.

> автор:
это пиздец какая широкая ниша

### 17. Multi system — `z_multi_system_test.clj`

- 🟢 Чёткий recipe.

> автор:

### 18. Two databases — `z_two_databases_test.clj`

- 🟢 Чёткий recipe + введение в кастомный `Factory`. Но `Factory`
  нигде раньше не объяснён.

> автор:

---

## Cross-cutting наблюдения

### O1. Нет главы «Why DI? What problem?»

Читатель попадает сразу на `di/start` без мотивации. У Integrant
в Readme такая секция есть.

> автор:
может быть зачаток есть в readme. мне было тяжело это написать, но давай напишем.

### O2. Tutorial и How-to не разделены

6 из 8 глав «Advanced» — это recipes («как сделать X»), а не
следующие шаги обучения. Это и есть источник ощущения «сухости».

> автор:

### O3. Недавно переписанные главы — другой voice

Update key, Log, Inspect, Ns publics — у них есть hook,
объяснение «зачем», cross-ссылки. Остальные суше.
**Это и есть target voice.**

> автор:
это сгенеренное, а "сухое" писал я руками

### O4. Registry вводится дважды

Неявно в главе 2, явно в главе 4. Слить.

> автор:

### O5. Abstractions (главная фича) — самая короткая глава

Несоответствие важность/объём.

> автор:

### O6. Update key должен быть в Base

Это не «продвинутый трюк», это способ собирать модули. Без него
непонятно, как DI решает задачу композиции.

> автор:

### O7. `Inspect`-как-глава-туториала не работает

Это reference-материал. Нужна другая роль.

> автор:

### O8. Ns publics не в TOC

`x_ns_publics_test.clj` есть в файлах, но не в `cljdoc.edn`.
Намеренно? Сам автор главы пишет в конце «`->memoize` is the
preferred way now» — то есть описывает legacy. Или выкинуть,
или явно отметить как историческое примечание.

> автор:
ну пусть будет, зачем выкидывать, я не пометил его как deprecated

---

## Что нужно от тебя

Пройдись по каждой главе и каждому O-пункту. Под строкой `> автор:`
напиши свою реакцию или поправь/вычеркни текст, если описано
неточно. Когда закончишь — скажи «готово», я прочитаю файл и
переходим к Этапу 1.

Если по ходу появятся темы, которые я не учёл (например, важные
use-cases из issues/вопросов пользователей) — добавляй в конец
файла свободным текстом.

ниже переписка из реддита


1)

Аватар u/serefayar
serefayar
•
3 г. назад
Nice work!

I agree with u/telenieko, the page definitely needs such a section.



Нравится
3

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
3 г. назад
I don't want to compare different solutions, that wouldn't be polite to them. But I have written a draft of Motivation section. What do you think?

Motivation

I think a function that uses a stateful component should also be a component. It's ok to have 97 functions and 3 stateful components. So creating of stateless components should be as cheap as possible. I don't want to build the system out of these 3 stateful components and manually pass the system to each function. And I don't want to define my 97 functions as components.

I also need something like Clojure namespaces. I want to define dependencies of a component with that component, just like I do it with Clojure namespaces. I don't want to define the dependency graph in a single file. I want to define regular functions.

I want to build applications separated on low coupled engines or subsystems. So I need a solution to define abstractions and extend (modify) existing common components. For example, in my application, each subsystem has its own route data and the single reitit handler. See di/update-key.

In Clojure it is common to use data DSL. I use reitit, and DI allows you to inject components into plain Clojure data. See di/template.

For some functions I want to add instrumentation. For example, I wrap some functions with NewRelic instrumentation without modifying the source code of those functions. Maybe I'll add schema/spec for my functional components with di/instrument.

I want to have a framework that is smart enough to stop already started components if the system fails to start.

Finally, I want to be able to dynamically redefine my 97 functions during development without having to restart the whole system.



Нравится
4

Не нравится

Ответить

Награда

Поделиться

Аватар u/weavejester
weavejester
•
3 г. назад
I don't want to compare different solutions, that wouldn't be polite to them.

I don't consider this to be rude, personally. If you want to compare DI to Integrant, please do so. It's rare that a single solution is good in all circumstances, and it's often the case that later solutions are better than previous ones.



Нравится
7

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
3 г. назад
https://darkleaf.github.io/di/notebooks/integrant.html

CC u/roguas, u/coffeesounds, u/rmuslimov, u/serefayar



Нравится
2

Не нравится

Ответить

Награда

Поделиться

Аватар u/weavejester
weavejester
•
3 г. назад
Thanks for writing this up!

A small correction regarding the Error Handling section: when Integrant throws an error, it includes the partially started system in the exception, to allow you to halt it if you choose. If you're using the Integrant-REPL library, then the init function does this automatically.

In the Handlers section, I think you may have misunderstood what suspending and resuming is for. If you eval a DI component function again, it presumably doesn't affect an already running system, right?



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
3 г. назад
•
, Отредактированные 3 г. назад
the init function does this automatically.

Thanks, I didn't know that.

It only catches clojure.lang.ExceptionInfo. Any component, such as database connector, will in most cases throw any Throwable.

It loses exceptions. DI uses suppressed exception, so user will see original exception and all possible exceptions thrown on stop.

test

combine-throwable

stop

it presumably doesn't affect an already running system, right?

Right. UPD. It is right for stateful components. If it is a service (stateless component) the behavior of running system will change. See an example

I think you may have misunderstood what suspending and resuming is for.

I think they are useful when someone redefines defmethod of stateless components. Maybe I'm wrong.



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/weavejester
weavejester
•
3 г. назад
It only catches clojure.lang.ExceptionInfo. Any component, such as database connector, will in most cases throw any Throwable.

Yes, because any exceptions arising from a build are wrapped in an ExceptionInfo in order to carry across additional context.

If you're using Integrant REPL, any Throwable exception will trigger a halt of the partially initiated system, before being rethrown for you to inspect along with additional context.

It loses exceptions.

I'm not sure I understand. Do you mean that it stops at the first exception?

I think they are useful when someone redefines defmethod of stateless components. Maybe I'm wrong.

No, their primary use is maintaining resources across restarts. For example, suppose you wanted to restart and rebuild your system, but you also didn't want to close any connections your system might have open. Suspend/resume allows you to persist open resources across restarts (where possible to do so).



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
3 г. назад
Yes, because any exceptions arising from a build are wrapped in an ExceptionInfo in order to carry across additional context.

Exactly. Thank you for the explanation.

I'm not sure I understand. Do you mean that it stops at the first exception?

Let's look at a trivial system: a list. A(root) depends on B, B depends on C.

If a BError occurs when starting B, C will already be running. DI will attempt to stop C. If the attempt fails with CError, the user will get the original BError with the CError suppressed.

JVM has a feature called suppressed Exceptions. (.addSuppressed b-error c-error). (.getSuppressed b-error) will return [c-error].

Some IDE prints suppressed errors. I have heard that IDEA does this.

No, their primary use is maintaining resources across restarts.

Ok. I've never been faced with this kind of task before.



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/weavejester
weavejester
•
3 г. назад
Let's look at a trivial system: a list. A(root) depends on B, B depends on C.

If a BError occurs when starting B, C will already be running. DI will attempt to stop C. If the attempt fails with CError, the user will get the original BError with the CError suppressed.

In this case, Integrant REPL wraps CError in an ExceptionInfo, which has a key :init-exception which references the original wrapped BError. All the exceptions are captured; none are lost.

JVM has a feature called suppressed Exceptions.

That's interesting; I wasn't aware of that feature. It might be an idea to use that in addition to (or instead of) the :init-exception key.


Нравится
2

Не нравится

Ответить

Награда

Поделиться


Еще 2 ответа
roguas
•
3 г. назад
Ok, lets talk. Why not integrant?



Нравится
7

Не нравится

Ответить

Награда

Поделиться

[удалено]
•
3 г. назад
If it's one type of framework Clojure has in spades, its component frameworks.


Нравится
4

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
3 г. назад
Have you seen the [tutorial](https://darkleaf.github.io/di/#tutorial)?


Нравится
1

Не нравится

Ответить

Награда

Поделиться

telenieko
•
3 г. назад
Maybe the page could have a section comparing this to other solutions and what motivated you to create a new one


Нравится
6

Не нравится

Ответить

Награда

Поделиться

rebcabin-r
•
3 г. назад
Excuse my ignorance: Are things like this DI, Integrant, Component for mocking / testing web services? From the docs, I can't see really what use-cases these things are for. (I'm not a web developer, doing compilers and embedded systems mostly)



Нравится
6

Не нравится

Ответить

Награда

Поделиться

Аватар u/weavejester
weavejester
•
3 г. назад
Some applications are systems made from a number of smaller, interdependent components, and often these components require some form of setup and shutdown code.

For example, a web service might consist of a web server, a database connection pool and a queued pool of workers. The web server would have routes, some of which might require a database connection, some of which might require the worker queue.

It's perfectly possible to write code to manually manage starting up and shutting down each component in the right order, and to manually pass each component to the right function. However, a dependency injection framework automates some of the work; you define the dependency tree declaratively, and the framework handles the rest.

The advantage (aside from writing a little less code) is that the dependency tree for the application, and often any associated configuration, is defined in one location. This is good for readability, but also allows components to be more easily swapped out for mocks, or to alter their configuration.



Нравится
5

Не нравится

Ответить

Награда

Поделиться

rebcabin-r
•
3 г. назад
I see. Thank you for your kind reply! In other settings, e.g., pytest, I'll use "fixtures" for such use-cases, including the control of order-of-activation. This sounds like an interesting new approach for me to learn about. Perhaps I've been doing more than I need to do or suffering other pessimalities :)


Нравится
1

Не нравится

Ответить

Награда

Поделиться

coffeesounds
•
3 г. назад
What’s wrong with Component?


Нравится
4

Не нравится

Ответить

Награда

Поделиться

rmuslimov
•
3 г. назад
Integrant.


Нравится
2

Не нравится

Ответить

Награда

Поделиться

slifin
•
3 г. назад
Does anyone remember what that component library was that used Pathom 3 as part of its implementation?



Нравится
1

Не нравится

Ответить

Награда

Поделиться

slifin
•
3 г. назад
Ah here it is

https://www.reddit.com/r/Clojure/comments/xmu1ah/nivekuilnexus_ergonomic_dependency_injection_via/


Нравится
1

Не нравится

Ответить

Награда

Поделиться

Раздел «Информация о сообществе»
r/Clojure
В сообществе
Clojure
Clojure is a dynamic, general-purpose programming language, combining the approachability and interactive development of a scripting language with an efficient and robust infrastructure for multithreaded programming.

Показать больше
Создано 23 июл. 2008 г.
Публичное
5,4 тыс.
посетителя в неделю
63
опубликованных материала за неделю
достижения сообщества
Старейшина
Старейшина
1 разблокировано

Просмотреть все
Закладки сообщества
Clojure Reddit Chat
Правила r/Clojure
1
Please stay on topic. This is the Clojure / ClojureScript subreddit.
2
Keep job posts of any kind to replies to the recurring monthly "Who's hiring" post.
Resources
Finding information about Clojure

Clojure Homepage
A Clojure Newbie Guide
Clojure Documentation
Clojure Cheat Sheet
ClojureScript Cheat Sheet
Clojure by Example
A History of Clojure
API Reference

ClojureDocs API reference
CljDoc
Clojure Guides

Getting Started
Clojure Distilled Beginner Guide
Clojure Style Guide
Clojure for the Brave and True
Clojure from the ground up
ClojureScript in 15 minutes
Practice Problems

Wonderland Katas
Clojure Koans
Interactive Problems

Maria Cloud
4Clojure
ClojureScript Koans
codewars
Clojure Videos

Clojure TV
Clojure Content on InfoQ
Full Disclojure
The Clojure Language
Misc Resources

StackOverflow info page
Clojure Events
The Clojure Community

#clojure on Libera.Chat
Clojure user groups
ClojureScript user groups
Clojure Q&A
Clojure Slack Channel
Clojurians-Zulipchat
Discljord Discord
Clojurians Discord UNMODERATED
Clojureverse: a forum for and by the Clojure community
matrix/riot-im Clojure room
Clojure Books

The Joy of Clojure
Clojure Programming
Clojure In Action
Programming Clojure
Web Development with Clojure
Clojure Cookbook
Professional Clojure
Living Clojure
Getting Clojure
Tools & Libraries

Leiningen - Package management
Deps and CLI Guide - Dependency management and CLI
nREPL - Networked REPL
Gorilla REPL - A rich REPL for Clojure in the notebook style
Clojars - Clojure library repository
The Clojure Toolbox - a list of popular Clojure libraries
Clojure Editors

Calva Visual Studio Code
Emacs CIDER
clojure-mode.el - Emacs mode
Emacs Prelude - a gentler Emacs mode
vim-fireplace - Vim!
Conjure - Conjure for NeoVim
Light Table - interactive Clojure IDE
Cursive - Clojure support for IntelliJ
Counterclockwise - Clojure support for Eclipse
Nightcode - a beginner friendly Clojure editor
Sublime Text 4
Atom Info
clojureVSCode - Clojure support for Visual Studio Code.
Web Platforms

Biff
re-frame
Hoplon
kit
Luminus
Clojure Jobs

Brave Clojure Jobs
Functional Jobs
Functional Works
Clojure Events
Clojure real-world-data 61
June 5, 2026 • 18:00
online On Friday, we&rsquo;ll have the usual weekly meeting of the real-world-data group. As usual, we will focus on community projects, library development, and documentation, but people are invited to propose additional topics to discuss and share. The main topics for the coming meetings will be documentation and additional updates. Updates about the agenda will be shared in the group chat: the real-world-data channel at the Zulip chat (requires login). If you wish to join the group, please reach out beforehand, and we&rsquo;ll add you to the calendar event. parens1920&times;1440 98.2 KB Zulip: https://clojurians.zulipchat.com/#narrow/stream/262224-events/near/599011186



2)

Аватар u/vvwccgz4lh
vvwccgz4lh
•
4 г. назад
What about shutting it down?
Edit: https://github.com/darkleaf/di/blob/master/example/src/example/core.clj#L50



Нравится
4

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
4 г. назад
In example app: 1, 2

In tests: 3


Нравится
2

Не нравится

Ответить

Награда

Поделиться

Аватар u/dustingetz
dustingetz
•
4 г. назад
Love your projects, thinking about all the right things



Нравится
4

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
4 г. назад
Thanks!



Нравится
1

Не нравится

Ответить

Награда

Поделиться

exclaim_bot
•
4 г. назад
Thanks!

You're welcome!


Нравится
-1

Не нравится

Ответить

Награда

Поделиться

Аватар u/agumonkey
agumonkey
•
4 г. назад
isn't DI a hybrid kind of variable / environment passing ?



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
4 г. назад
Could you provide some examples? I don't understand the question.



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/agumonkey
agumonkey
•
4 г. назад
Well, I'm not sure I ever understood DI fully, so it's a noob question if you will. But it seems very close to dynamic variables.

(defn f [x]
   (+ *dyn-var* x))
dyn-var being redefineable from outer context



Нравится
1

Не нравится

Ответить

Награда

Поделиться

Аватар u/kuzmin_m
kuzmin_m
Автор
•
4 г. назад
Ah, there are common things.

The differences are:

DI explicitly defines dependencies

DI starts/stops stateful components

I've wrote the tutorial. I hope it helps you understand concept.



Нравится
5

Не нравится

Ответить

Награда

Поделиться

Аватар u/agumonkey
agumonkey
•
4 г. назад
dank u


Нравится
2

Не нравится

Ответить

Награда

Поделиться

Аватар u/didibus
didibus
•
4 г. назад
DI just means that you pass in dependencies instead of using globals directly.

Dynamic vars would be one way to do DI, as you can redefine dependencies by binding them.

Passing the dependencies as functions arguments or inside a context map is another way.

Edit: referring to the concept of dependency injection,not this library specifically.



Нравится
2

Не нравится

Ответить

Награда

Поделиться

Аватар u/agumonkey
agumonkey
•
4 г. назад
yeah, thanks


Нравится
1

Не нравится

Ответить

Награда

Поделиться
