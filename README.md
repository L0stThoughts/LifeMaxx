### Informace

### Autor: Daniel Štuka

### Aktuální verze 1.0.0

# Seznam použitých zkratek

```
UI = User Interface (Uživatelské rozhraní)
MVVM = Model-View-ViewModel (Architektonický vzor)
DI = Dependency Injection (Vkládání závislostí)
API = Application Programming Interface
```
# Popis produktu

LifeMaxx je komplexní mobilní aplikace pro Android zaměřená na sledování a správu zdravotního stavu uživatele, se zvláštním důrazem na sledování doplňků stravy,
pitného režimu, spánku a celkové výživy. Aplikace vznikla jako řešení problému mnoha lidí, kteří potřebují přehledně a efektivně sledovat svůj denní příjem doplňků
stravy a jejich vliv na celkové zdraví.

## Cíl projektu

Cílem LifeMaxx je poskytnout uživatelům jednoduché a intuitivní rozhraní pro:

```
Správu užívaných doplňků stravy
Sledování hydratace a pitného režimu
Monitoring spánku a jeho kvality
Evidenci příjmu živin a kalorií
Nastavení připomenutí a upozornění
```
## Zaměření projektu

Aplikace se zaměřuje na komplexní přístup ke zdraví uživatele prostřednictvím sledování klíčových zdravotních metrik. LifeMaxx umožňuje uživatelům vidět
souvislosti mezi užívanými doplňky, kvalitou spánku, hydratací a celkovým zdravotním stavem.

## Cílová skupina uživatelů

```
Lidé se zdravotními problémy vyžadující pravidelné užívání doplňků stravy
Sportovci a fitness nadšenci sledující svůj příjem živin
Osoby se zájmem o zlepšení svého zdravotního stavu
Starší lidé, kteří potřebují připomenutí k užívání léků či doplňků
Kdokoliv se zájmem o systematické sledování svých zdravotních návyků
```
# Analýza a návrh

## Analýza problému

Během analýzy byly identifikovány následující problémy, které aplikace řeší:

```
1. Zapomínání užívání doplňkůZapomínání užívání doplňků - Mnoho lidí má problém pravidelně užívat své doplňky stravy
2. Nekonzistentní hydrataceNekonzistentní hydratace - Nedostatečný příjem tekutin je běžný problém
3. Špatná kvalita spánkuŠpatná kvalita spánku - Nedostatek dat pro analýzu spánkových vzorců
4. Nepřehlednost v užívaných produktechNepřehlednost v užívaných produktech - Uživatelé ztrácí přehled o všech užívaných doplňcích
5. Chybějící notifikaceChybějící notifikace - Absence připomenutí v kritických momentech
```
## Požadavky


## Funkční požadavky

```
Správa doplňků stravy (přidání, úprava, mazání)
Sledování denních dávek a jejich plnění
Monitoring příjmu tekutin
Sledování kvality a délky spánku
Sledování nutričních hodnot jídel
Systém upozornění a připomenutí
Offline režim s lokálním ukládáním dat
Skenování čárových kódů doplňků
```
## Nefunkční požadavky

```
Intuitivní a přehledné uživatelské rozhraní
Rychlé reakce aplikace na uživatelské vstupy
Zabezpečení citlivých uživatelských dat
Nízká spotřeba baterie
Kompatibilita se zařízeními Android 8.0+
Minimální využití mobilních dat
```
## Architektura / Návrh aplikace

Aplikace LifeMaxx využívá moderní architekturu MVVM (Model-View-ViewModel) pro oddělení logiky a UI. Klíčové komponenty architektury:

```
1. ModelModel - Datové třídy reprezentující entity jako Supplement, Dose, WaterIntake, SleepEntry apod.
2. ViewView - Uživatelské rozhraní vytvořené pomocí Jetpack Compose
3. ViewModelViewModel - Třída sloužící jako prostředník mezi UI a daty
4. RepositoryRepository - Vrstva pro přístup k datům z různých zdrojů (Firebase, lokální úložiště)
```
Pro persistenci dat využívá aplikace kombinaci:

```
Firebase Firestore pro cloudové ukládání
SharedPreferences pro lokální ukládání v offline režimu
```
# Development/Platforma

## Programovací jazyky


```
KotlinKotlin - Moderní, bezpečný a expresivní jazyk pro vývoj Android aplikací, zvolený pro své pokročilé funkce, null-safety a kompatibilitu s Java ekosystémem.
```
## Vývojové prostředí (IDE)

```
Android StudioAndroid Studio - Oficiální IDE pro vývoj Android aplikací, poskytující kompletní sadu nástrojů pro vývoj, testování a ladění.
```
## Další nástroje a technologie

```
Jetpack ComposeJetpack Compose - Moderní UI toolkit pro deklarativní tvorbu uživatelského rozhraní
KoinKoin - Lehký framework pro dependency injection v Kotlinu
FirebaseFirebase - Platforma od Google pro mobilní a webové aplikace:
Firestore - NoSQL databáze pro ukládání dat
Authentication - Pro autentizaci uživatelů
Analytics - Pro sledování chování uživatelů
Kotlin CoroutinesKotlin Coroutines - Pro asynchronní programování
ML KitML Kit - Pro skenování čárových kódů
AndroidXAndroidX - Modernizované knihovny podpory pro Android
Material Design 3Material Design 3 - Design systém od Google pro konzistentní a intuitivní UI
```

# Licence

LifeMaxx je vyvíjen jako soukromý softwarový produkt s následujícími licenčními podmínkami:

```
Aplikace je poskytována "tak jak je" bez jakýchkoliv záruk.
Uživatelé mohou aplikaci používat pro soukromé účely.
Není povoleno aplikaci upravovat, dekompilovat nebo redistribuovat.
Aplikace využívá knihovny a služby třetích stran, které mohou mít vlastní licenční podmínky.
```
Využívané externí knihovny a služby:

```
Firebase: Apache License 2.
Jetpack Compose: Apache License 2.
Koin: Apache License 2.
ML Kit: Apache License 2.
```
# Support/SLA

LifeMaxx aktuálně nabízí následující možnosti podpory:

```
E-mailová podporaE-mailová podpora: Odpověď do 48 hodin v pracovních dnech
In-app zpětná vazbaIn-app zpětná vazba: Funkce pro zasílání zpětné vazby a nahlašování chyb
AktualizaceAktualizace: Pravidelné aktualizace s opravami chyb a novými funkcemi
```
Pro budoucí verze je plánováno:


```
Online dokumentace a FAQOnline dokumentace a FAQ: Podrobný manuál a často kladené otázky
Komunitní fórumKomunitní fórum: Pro sdílení tipů a řešení problémů mezi uživateli
Premium podporaPremium podpora: Rozšířená podpora pro předplatitele s rychlejší odezvou
```
# Závěr

## Zhodnocení projektu

Projekt LifeMaxx úspěšně implementoval všechny plánované funkce a splnil stanovené cíle. Aplikace poskytuje komplexní řešení pro sledování doplňků stravy,
spánku, hydratace a výživy. Uživatelské rozhraní je intuitivní a responzivní díky využití moderních technologií jako Jetpack Compose.

Během vývoje se objevily výzvy především v oblasti offline synchronizace dat a implementace skenování čárových kódů. Pro zlepšení by bylo vhodné rozšířit testovací
pokrytí a optimalizovat výkon na starších zařízeních.

## Získané zkušenosti

Během vývoje jsem získal cenné zkušenosti v následujících oblastech:

```
Implementace MVVM architektury v Android aplikacích
Práce s Jetpack Compose pro tvorbu moderního UI
Využití Firebase Firestore pro ukládání a synchronizaci dat
Implementace offline režimu s lokálním ukládáním
Dependency injection pomocí Koin
```
## Možné budoucí rozšíření

Pro budoucí verze aplikace zvažuji následující rozšíření:

```
1. Synchronizace s fitness zařízenímiSynchronizace s fitness zařízeními: Integrace dat ze smart hodinek a fitness trackerů
2. Rozšířené analýzy a statistikyRozšířené analýzy a statistiky: Komplexnější grafy a vizualizace zdravotních dat
3. Sociální funkceSociální funkce: Možnost sdílet úspěchy a výzvy s přáteli
4. AI doporučeníAI doporučení: Implementace umělé inteligence pro personalizované doporučení
5. Exportní funkceExportní funkce: Možnost exportovat data ve formátech CSV, PDF
6. Rozšíření databáze doplňkůRozšíření databáze doplňků: Předvyplněná databáze běžných doplňků stravy a léků
7. Multiplatformní verzeMultiplatformní verze: Rozšíření na iOS a webovou platformu
```
# Release notes

```
Přidán offline režim s lokální synchronizací dat
Vylepšené UI v Material Design 3
Optimalizace výkonu a spotřeby baterie
Přidány detailní statistiky pro spánek
Vylepšené notifikace a připomenutí
```
## Verze 1.0.0

```
Kompletní přepracování UI do Jetpack Compose
Migrace na MVVM architekturu
Implementace skenování čárových kódů
Přidáno sledování spánku
Firebase integrace pro cloudové ukládání dat
```
## Verze 0.5.0

```
Přidáno sledování pitného režimu
Implementace sledování výživy a kalorií
Vylepšený systém notifikací
Přidána podpora tmavého režimu
Rozšířené nastavení aplikace
```
## Verze 0.1.0

```
První veřejné vydání
Základní správa doplňků stravy
Jednoduché sledování dávkování
Základní připomenutí
Lokální ukládání dat
```

