# ПРОМПТ: логотип и иконка «Кто Я: Универ Edition»

> **Как этим пользоваться.** Три режима, выбери нужный:
> **(A)** скопируй раздел 4 в генератор картинок (Midjourney / Flux / DALL·E 3 / Ideogram / Recraft) — получишь исходную картинку;
> **(B)** скопируй раздел 7 в агента-исполнителя (Claude Code / Cursor / Hermes), который живёт в папке проекта — он сделает из картинки настоящую иконку приложения со всеми размерами и слоями;
> **(C)** раздел 1–3 и 6 отдай дизайнеру-человеку как бриф.
>
> Проект уже собран: `com.kstoya.univer`, приложение «Кто Я: Универ», APK лежит в
> `C:\Users\Rubin\Desktop\kto-ya-univer\app-debug.apk`. Сейчас иконка — временный жёлтый
> смайлик (`res/drawable/ic_launcher_foreground.xml`). Логотип заменяет её и уходит в шапку,
> сплэш, обложку чата и на картинку итогов.

---

## 1. Бриф (5 строк, читать перед генерацией)

1. **Что это:** вечериночная игра «угадай, кто ты» для компании студентов 2–10 человек.
   Телефон прикладывают ко лбу, остальные видят карточку с лицом друга или препода.
2. **Эмоция знака:** ржач и лёгкое хулиганство, а не «серьёзный софт». Должно хотеться нажать.
3. **Аудитория:** 17–23 года, играют на перемене в столовой, делятся скриншотами в чате группы.
4. **Носители:** иконка 48 px в лончере, сплэш на весь экран, шапка главного экрана, обложка
   Telegram-чата, водяной знак на картинке итогов.
5. **Чем быть не должно:** не корпоративный логотип, не «айтишный» щит/куб/мозг, не лицо
   конкретного человека, не мем из интернета (чужие права + устареет за месяц).

---

## 2. Палитра и шрифты (взяты из ТЗ приложения, не менять)

| Токен | HEX | Где в знаке |
|---|---|---|
| `accent` (янтарь) | `#FFB020` | главный цвет знака: телефон, вопросы, обводки |
| `bg` (глубокий графит) | `#101014` | фон иконки, контур |
| `friend` (синий) | `#2563EB` | акцент №2: «дружеская» половина карточки |
| `teacher` (красный) | `#DC2626` | акцент №3: «преподавательская» половина, использовать редко |
| `text` | `#F7F7FA` | белый в знаке (вопросительный знак, глаза) |

Правило: **в иконке не больше трёх цветов** (фон + два акцента), иначе на 48 px получается каша.
Шрифт для вордмарка: Rubik Bold / Inter ExtraBold, всё капсом, трекинг −1…−2 %.
Кириллица обязательна.

---

## 3. Три концепции знака (выбрать одну; по умолчанию — К1)

### К1. «Телефон на лбу» — главная метафора, рекомендуемая

Голова в профиль или анфас, к ней плашмя прижат телефон, на экране — крупный знак вопроса.
Плюсы: сразу объясняет механику, узнаётся даже в 32 px, легко рисуется двумя цветами.
Минусы: чуть сложнее «буквенной» иконки, нужна аккуратная геометрия, чтобы не вышло «человек
с кирпичом».

### К2. «Карточка с лицом»

Скошенная карточка 3:4 (как в игре), фон — синий градиент, вместо лица — силуэт головы
и огромный «?». Плюсы: прямая связь с карточкой персонажа, хорошо ложится на превью итогов.
Минусы: в маленьком размере карточка читается как «файл/документ», а не как игра.

### К3. «Вопросительный знак-улыбка»

Знак «?» из двух элементов: точка — телефон, дуга — улыбка. Максимально просто, живёт в 24 px.
Минусы: теряет универ, конкурирует с сотней других «?»-иконок, хуже продаёт игру.

---

## 4. Готовые промпты для генераторов картинок

Все промпты — на английском (модели понимают его точнее), отрицания для Midjourney идут
параметром `--no`, для Flux/DALL·E — отдельным блоком после промпта.

### 4.0. Общий негатив (добавляй в любую модель)

```
negative: photorealistic photo, real human face, celebrity likeness, 3d glossy render,
chrome, bevel, drop shadow, heavy gradient, lens flare, texture noise, tiny details, thin
1px lines, clip art, emoji, stock icon, watermark, signature, brand logo, letters, words,
text, caption, frame, multiple objects, cropped, off-center, low contrast, pastel
```

### 4.1. К1 — телефон на лбу, плоский вектор (основной вариант)

```
Flat vector app icon on a solid deep navy background #101014, square 1:1. A friendly
side-profile human head simplified to geometric shapes, one continuous thick outline, a
smartphone pressed flat against the forehead like a headband, its screen facing the viewer
and showing a big bold white question mark. Two accent colors only: warm amber #FFB020 for
the phone and the outline, off-white #F7F7FA for the question mark. No gradients, no shading,
no texture. Bold 10px strokes, rounded joins, generous negative space, mark occupies 78% of
the canvas, everything inside the central circle. Crisp edges, poster-like, designed to stay
legible at 48 pixels.
--ar 1:1 --style raw --v 7 --no text, letters, photo, 3d, gradient, shadow, clutter
```

### 4.2. К1b — то же, но анфас (для варианта «в круге»)

```
Minimal geometric app icon: a round human head seen from the front, no facial features
except a long thin smile, a smartphone fixed horizontally across the middle of the face like
a visor, screen glowing amber with a single white question mark. Flat duotone vector, amber
#FFB020 and off-white on graphite #101014, thick uniform outlines, symmetrical, centered,
inside a circle, no text. Reads clearly at 48 px. 
--ar 1:1 --style raw --v 7 --no text, letters, gradient, 3d, photo, detail, noise
```

### 4.3. К2 — карточка с лицом и вопросом

```
Flat vector icon: a slightly tilted rounded card (3:4) with a vivid blue gradient fill
#2563EB to #1E40AF, a simplified white head silhouette cut out of the upper half, and a huge
bold amber question mark in the lower half. Thin off-white outline around the card so it
separates from the dark graphite background #101014. Minimal shapes, no text, no photo,
centered, 1:1, designed as an app icon that stays readable at 48 px.
--ar 1:1 --style raw --v 7 --no text, letters, 3d, shadow, texture, photo, clutter
```

### 4.4. К3 — «?» из телефона и улыбки

```
Extremely simple vector mark: a question mark built from two shapes — a curved amber stroke
ending in a small smartphone with a glowing screen, and a separate dot below formed by a
short smile arc. Two colors, flat, no outline fill gradients, centered on graphite #101014,
1:1, designed as a 24–48 px app icon, high contrast, generous padding.
--ar 1:1 --style raw --v 7 --no text, letters, 3d, gradient, shadow, photo, details
```

### 4.5. Вордмарк «КТО Я?» (отдельно от знака, шрифтом)

```
Bold uppercase Cyrillic wordmark "КТО Я?" in a geometric grotesque (Rubik Bold style), tight
tracking, off-white on deep graphite, with a small amber question mark, plus a thin uppercase
subtitle "УНИВЕР EDITION" underneath at 25% of the headline height, letter-spaced. Clean
flat two-color logo lockup, no effects, no 3d, centered, 1:1 with generous margins.
--ar 1:1 --style raw --v 7 --no photo, 3d, gradient, shadow, extra words
```

> Кириллицу нормально рисуют **Ideogram 3** и **Recraft V3**; Flux и DALL·E 3 часто портят
> «КТО Я» — генерируй только знак, а текст ставь кодом (раздел 7) или в Figma. В иконке
> приложения текста быть не должно вообще: имя покажет сам лончер.

### 4.6. Иконка для стора, «мягкий объём» (опция, для красоты в каталоге)

```
Soft 3d clay app icon, matte plasticine material, a cute round head with a smartphone stuck
to its forehead, screen with a white question mark, warm amber and graphite palette, gentle
studio light from top-left, one soft shadow, centered, rounded square, 1:1, toy-like but not
childish, high quality product render, no text.
--ar 1:1 --v 7 --no text, letters, glossy plastic, chrome, reflection, busy background
```

Использовать **только** для обложки каталога/презентации: в 48 px объёмный знак темнеет и
разваливается, поэтому в лончере оставляем плоский вариант 4.1.

### 4.7. Если модель — DALL·E 3 (в чате)

Промпт тот же, но без `--ar` и `--no`: начни с «Квадратная иконка приложения 1:1», в конце
добавь фразой: «No text, no letters, no photographs, no 3D gloss, flat vector only».
DALL·E не держит точный HEX — после генерации всё равно перекрашивай под палитру.

### 4.8. Параметры по моделям (шпаргалка)

| Модель | Что добавить | Ожидание |
|---|---|---|
| Midjourney v7 | `--ar 1:1 --style raw --v 7`, негатив через `--no` | лучший «постерный» вид, HEX примерно |
| Flux 1.1 / Flux dev | промпт как есть + негатив отдельной строкой | точная композиция, чистый вектор |
| Ideogram 3 | + `style: design`, `--no photo` | единственный, кто рисует «КТО Я?» без ошибок |
| Recraft V3 | + `style: vector art`, `colors: #FFB020, #101014, #F7F7FA` | настоящий SVG-подобный результат |
| DALL·E 3 | без параметров, негатив словами | быстрый черновик идей |

---

## 5. Правила, которые важнее красоты

1. **Никакого текста внутри иконки.** На 48 px кириллица превращается в грязь, а имя
   приложения лончер и так печатает рядом.
2. **Никаких реальных лиц.** Преподаватель или друг в логотипе — это ст. 152.1 ГК РФ и
   ссора с человеком; знак должен быть абстрактным.
3. **Три цвета максимум**, плоские заливки, толщина линий ≥ 8 px при канве 108 dp.
4. **Safe zone:** весь смысл знака — внутри вписанного круга 78 % канвы. Круглый лончер
   Android и маска iOS срежут углы без предупреждения.
5. **Проверка на 32 px.** Если в 32 px не видно, что это голова с телефоном — знак
   упрощается (убрать уши, шею, пальцы, любые внутренние линии), а не уменьшается.
6. **Силуэт на тёмном и светлом фоне.** У знака должен быть тонкий контур (2–3 px при 108 dp),
   иначе на светлых обоях он растворяется.
7. **Никакого мелкого декора** — точки, штриховка, «блики»: умирают первыми.

---

## 6. Технические размеры (Android; ничего не пропустить)

| Что | Размер / формат | Куда |
|---|---|---|
| Adaptive foreground | 432×432 px PNG c альфой (канва 108 dp @4x) | `res/mipmap-xxxhdpi/ic_launcher_foreground.png` |
| Adaptive background | 432×432 px, непрозрачный, можно градиент | `res/mipmap-xxxhdpi/ic_launcher_background.png` или `@color/ic_launcher_bg` |
| Visible area | центральные **72 dp** из 108 | — |
| Safe zone | центральные **66 dp** (≈ 61 % канвы) | весь смысл знака |
| Monochrome (Android 13+) | 432×432 PNG, один цвет, только альфа | `res/mipmap-anydpi-v26/ic_launcher_monochrome.xml` |
| Legacy / round | 512×512 и 512×512 круг | `res/mipmap-*/ic_launcher.png`, `ic_launcher_round.png` |
| Store (Google Play / RuStore) | 512×512 PNG 32-bit, без альфы | загрузка в консоль |
| Splash (Android 12+) | знак 288 dp, из них видно 2/3 | `values-v31` стиль + `windowSplashScreenAnimatedIcon` |
| Шапка приложения | 96×96 dp (знак) + вордмарк отдельным вектором | `res/drawable/logo_header.xml` |
| Обложка чата / превью ссылки | 1200×630 px, знак слева, «КТО Я? Универ» справа | скрин/обмен |
| Водяной знак итогов | 64×64 px, 40 % прозрачности | canvas при шаринге |

Форматы сдачи: **SVG (главный, редактируемый) + PNG** в 1024, 512, 432, 192, 144, 96, 72, 48 px.
Вектор обязателен: иконку попросят «сделай жёлтый потемнее» через неделю.

---

## 7. Промпт для агента-исполнителя (вставить в Claude Code / Hermes из папки проекта)

```
Ты — дизайнер-инженер, работаешь в существующем Android-проекте
C:\Users\Rubin\Desktop\kto-ya-univer (Kotlin + Compose, applicationId com.kstoya.univer,
minSdk 26). Сделай логотип и полный набор иконок приложения «Кто Я: Универ».

ЗНАК: голова в профиль с телефоном, прижатым ко лбу, на экране телефона — знак вопроса.
Плоский вектор, 3 цвета: фон #101014, янтарь #FFB020, белый #F7F7FA. Толщина линий
>= 8 px при канве 108 dp, скруглённые стыки, весь смысл внутри вписанного круга 78 % канвы.

1. Нарисуй знак вектором (SVG как источник правды) + HTML-страницу 1080×1080 с этим знаком,
   отрендери PNG через headless Edge:
   "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" --headless=new --disable-gpu
   --hide-scrollbars --force-device-scale-factor=1 --window-size=1080,1080
   --screenshot="C:/.../logo_1080.png" "file:///C:/.../logo.html"
   Источник (svg/html) положи в tools/logo/, чтобы знак можно было перекрасить и перерендерить.

2. Собери adaptive icon:
   - foreground 432×432 PNG с альфой, знак занимает центральные 66 dp (то есть ~61 % канвы,
     поля по 61 px с каждой стороны при 432 px);
   - background: сплошной #101014 (через @color/ic_launcher_bg) либо мягкий градиент, если
     плоский фон смотрится пусто — но тогда проверь, что знак не теряется;
   - monochrome: тот же силуэт в один цвет (белый), только альфа — для тематических иконок
     Android 13+. Клади отдельный `res/mipmap-anydpi-v33/ic_launcher.xml` с
     `<monochrome android:drawable="@drawable/ic_launcher_mono"/>` — так элемент не попадёт в
     разбор старыми инструментами, а на API 33+ иконка перекрашивается системой;
   - legacy ic_launcher.png и ic_launcher_round.png во всех mipmap-* (mdpi..xxxhdpi);
   - удали старый жёлтый смайлик из res/drawable/ic_launcher_foreground.xml, если он больше не нужен.

3. Сплэш: Android 12+ получает иконку автоматически через windowSplashScreenAnimatedIcon —
   положи знак отдельным вектором, фон #101014, и проверь, что он не обрезан маской.

4. Вордмарк «КТО Я?» + подпись «УНИВЕР EDITION» — отдельный вектор (текст переводи в кривые;
   каталога res/font в проекте пока нет — если нужен живой шрифт, положи Rubik Bold .ttf (OFL)
   в res/font и подключи через FontFamily). В иконку приложения текст НЕ класть.
   Вордмарк использовать в шапке главного экрана (S1) слева от кнопок, высота 28 dp.

5. Проверка (обязательно, с реальными файлами):
   - отрендери превью-страницу: знак в круге и в скруглённом квадрате на 300, 96, 72, 48 и 32 px,
     на тёмном #101014 и светлом #F6F6F8 фоне, плюс строка лончера «иконка + Кто Я: Универ»;
     сделай скриншот и оцени через vision: что читается в 32 px, не срезала ли маска смысл;
   - исправь и перерендери, если 32 px не читается (упростить, а не уменьшить);
   - собери ./gradlew :app:assembleDebug и убедись, что aapt2 dump badging показывает новую иконку;
   - приложи скриншот установленного приложения в лончере, если есть доступ к телефону (adb).

Сдай: SVG-исходник, PNG 1024/512/432/192/144/96/72/48, превью-страницу, скриншот проверки,
список изменённых файлов и вывод сборки. Не отчитывайся «готово», пока assembleDebug не зелёный.
```

---

## 8. Приёмка

- [ ] В 48 px и в 32 px понятно, что это голова с телефоном (проверено скриншотом, не «на глаз»).
- [ ] Знак целиком внутри круга: круглый лончер ничего не срезает.
- [ ] На тёмных и светлых обоях силуэт не растворяется (есть контур/контраст ≥ 4.5:1).
- [ ] В иконке нет текста, нет фото, нет чужих лиц и логотипов.
- [ ] Есть SVG, PNG всех размеров, foreground/background/monochrome слои, legacy и round.
- [ ] `assembleDebug` зелёный, `aapt2 dump badging` показывает `ic_launcher` из mipmap.
- [ ] Вордмарк с кириллицей без ошибок в буквах («КТО Я?», а не «KTO R»).
- [ ] Палитра совпадает с приложением: янтарь #FFB020 на графите #101014.

---

## 9. Чего не делать

- Не ставить в иконку имя приложения текстом — лончер печатает его сам.
- Не брать готовую иконку из стока («человек с телефоном» их сотни) — узнаваемости ноль.
- Не рисовать реальное лицо, даже стилизованное «похоже на нашего препода».
- Не делать 3D-глянец, хром, блики, тени — на 48 px это грязное пятно.
- Не использовать больше трёх цветов и градиенты на самом знаке.
- Не отдавать один PNG 512×512 «на все случаи»: adaptive-иконке нужны отдельные слои,
  иначе круглый лончер срежет углы и покажет чёрные поля.
- Не менять палитру приложения под логотип: наоборот, логотип подстраивается под `#FFB020`/`#101014`.
