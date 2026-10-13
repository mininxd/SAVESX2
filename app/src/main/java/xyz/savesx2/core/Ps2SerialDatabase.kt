package xyz.savesx2.core

import java.util.Locale

/**
 * PS2 Game Disc / Serial information mapped from canonical serial codes.
 */
data class Ps2GameInfo(
    val serial: String,
    val title: String,
    val region: Ps2Region,
    val publisher: String? = null
)

/**
 * High-performance, offline PS2 Game Serial Database.
 * Maps PlayStation 2 save directory serials (e.g. `BASLUS-21445`, `BESLES-52541`, `SLUS-20062`)
 * to canonical game titles, regions, and publisher information with zero network dependency.
 */
object Ps2SerialDatabase {

    private val SERIAL_REGEX = Regex("""([A-Za-z]{4})[-_]?([0-9]{5})""")

    /**
     * Extracts and normalizes a PS2 serial code from a directory name or string.
     * Example: "BASLUS-21445FFXII01" -> "SLUS-21445"
     */
    fun extractSerial(raw: String): String? {
        val match = SERIAL_REGEX.find(raw) ?: return null
        val prefix = match.groupValues[1].uppercase(Locale.US)
        val number = match.groupValues[2]
        return "$prefix-$number"
    }

    /**
     * Queries the database for game metadata given a save directory name or raw serial.
     */
    fun lookupGame(directoryOrSerial: String): Ps2GameInfo? {
        val serial = extractSerial(directoryOrSerial) ?: return null
        return DATABASE[serial]
    }

    /**
     * Returns the canonical game title for a save directory if known.
     */
    fun getCanonicalTitle(directoryOrSerial: String): String? {
        return lookupGame(directoryOrSerial)?.title
    }

    /**
     * Determines whether a given title string looks like a raw unparsed serial or directory name.
     */
    fun isRawSerialTitle(title: String): Boolean {
        val t = title.trim()
        return t.isBlank() ||
                t.matches(Regex("""^[A-Z]{1,2}[A-Z]{4}[-_]?[0-9]{5}.*""")) ||
                t.matches(Regex("""^[A-Z]{4}[-_]?[0-9]{5}.*"""))
    }

    // --- Embedded Database ---
    private val DATABASE: Map<String, Ps2GameInfo> = buildMap(1600) {
        fun add(serial: String, title: String, region: Ps2Region, publisher: String? = null) {
            put(serial.uppercase(Locale.US), Ps2GameInfo(serial.uppercase(Locale.US), title, region, publisher))
        }

        // ==================== NORTH AMERICA (NTSC-U) ====================
        // Final Fantasy Series
        add("SLUS-20268", "Final Fantasy X", Ps2Region.US, "Square Enix")
        add("SLUS-20672", "Final Fantasy X-2", Ps2Region.US, "Square Enix")
        add("SLUS-20968", "Final Fantasy XI", Ps2Region.US, "Square Enix")
        add("SLUS-21445", "Final Fantasy XII", Ps2Region.US, "Square Enix")
        add("SLUS-21464", "Dirge of Cerberus: Final Fantasy VII", Ps2Region.US, "Square Enix")

        // Grand Theft Auto Series
        add("SLUS-20062", "Grand Theft Auto III", Ps2Region.US, "Rockstar Games")
        add("SLUS-20552", "Grand Theft Auto: Vice City", Ps2Region.US, "Rockstar Games")
        add("SLUS-20946", "Grand Theft Auto: San Andreas", Ps2Region.US, "Rockstar Games")
        add("SLUS-21423", "Grand Theft Auto: Liberty City Stories", Ps2Region.US, "Rockstar Games")
        add("SLUS-21590", "Grand Theft Auto: Vice City Stories", Ps2Region.US, "Rockstar Games")

        // Kingdom Hearts Series
        add("SLUS-20370", "Kingdom Hearts", Ps2Region.US, "Square Enix")
        add("SLUS-21005", "Kingdom Hearts II", Ps2Region.US, "Square Enix")
        add("SLUS-21799", "Kingdom Hearts Re:Chain of Memories", Ps2Region.US, "Square Enix")

        // Metal Gear Solid Series
        add("SLUS-20144", "Metal Gear Solid 2: Sons of Liberty", Ps2Region.US, "Konami")
        add("SLUS-20554", "Metal Gear Solid 2: Substance", Ps2Region.US, "Konami")
        add("SLUS-20915", "Metal Gear Solid 3: Snake Eater", Ps2Region.US, "Konami")
        add("SLUS-21247", "Metal Gear Solid 3: Subsistence (Disc 1)", Ps2Region.US, "Konami")
        add("SLUS-21359", "Metal Gear Solid 3: Subsistence (Disc 2)", Ps2Region.US, "Konami")

        // God of War Series
        add("SCUS-97399", "God of War", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97481", "God of War II", Ps2Region.US, "Sony Computer Entertainment")

        // Resident Evil Series
        add("SLUS-20184", "Resident Evil: Code: Veronica X", Ps2Region.US, "Capcom")
        add("SLUS-20669", "Resident Evil Outbreak", Ps2Region.US, "Capcom")
        add("SLUS-20984", "Resident Evil Outbreak File #2", Ps2Region.US, "Capcom")
        add("SLUS-20678", "Resident Evil: Dead Aim", Ps2Region.US, "Capcom")
        add("SLUS-21134", "Resident Evil 4", Ps2Region.US, "Capcom")

        // Silent Hill Series
        add("SLUS-20228", "Silent Hill 2", Ps2Region.US, "Konami")
        add("SLUS-20622", "Silent Hill 3", Ps2Region.US, "Konami")
        add("SLUS-20902", "Silent Hill 4: The Room", Ps2Region.US, "Konami")
        add("SLUS-21738", "Silent Hill: Origins", Ps2Region.US, "Konami")
        add("SLUS-21898", "Silent Hill: Shattered Memories", Ps2Region.US, "Konami")

        // Persona & SMT Series
        add("SLUS-20911", "Shin Megami Tensei: Nocturne", Ps2Region.US, "Atlus")
        add("SLUS-20974", "Shin Megami Tensei: Digital Devil Saga", Ps2Region.US, "Atlus")
        add("SLUS-21152", "Shin Megami Tensei: Digital Devil Saga 2", Ps2Region.US, "Atlus")
        add("SLUS-21386", "Shin Megami Tensei: Persona 3", Ps2Region.US, "Atlus")
        add("SLUS-21569", "Shin Megami Tensei: Persona 3 FES", Ps2Region.US, "Atlus")
        add("SLUS-21782", "Shin Megami Tensei: Persona 4", Ps2Region.US, "Atlus")
        add("SLUS-21431", "Shin Megami Tensei: Devil Summoner", Ps2Region.US, "Atlus")
        add("SLUS-21804", "Shin Megami Tensei: Devil Summoner 2", Ps2Region.US, "Atlus")

        // Team Ico
        add("SCUS-97113", "ICO", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97472", "Shadow of the Colossus", Ps2Region.US, "Sony Computer Entertainment")

        // Gran Turismo Series
        add("SCUS-97102", "Gran Turismo 3: A-Spec", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97328", "Gran Turismo 4", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97103", "Gran Turismo Concept", Ps2Region.US, "Sony Computer Entertainment")

        // Tekken Series
        add("SLUS-20001", "Tekken Tag Tournament", Ps2Region.US, "Namco")
        add("SLUS-20328", "Tekken 4", Ps2Region.US, "Namco")
        add("SLUS-21064", "Tekken 5", Ps2Region.US, "Namco")

        // Devil May Cry Series
        add("SLUS-20216", "Devil May Cry", Ps2Region.US, "Capcom")
        add("SLUS-20484", "Devil May Cry 2 (Dante)", Ps2Region.US, "Capcom")
        add("SLUS-20485", "Devil May Cry 2 (Lucia)", Ps2Region.US, "Capcom")
        add("SLUS-21136", "Devil May Cry 3: Dante's Awakening", Ps2Region.US, "Capcom")
        add("SLUS-21361", "Devil May Cry 3: Special Edition", Ps2Region.US, "Capcom")

        // Dragon Quest
        add("SLUS-21207", "Dragon Quest VIII: Journey of the Cursed King", Ps2Region.US, "Square Enix")

        // Clover / Capcom
        add("SLUS-21377", "Okami", Ps2Region.US, "Capcom")
        add("SLUS-21489", "God Hand", Ps2Region.US, "Capcom")
        add("SLUS-20751", "Viewtiful Joe", Ps2Region.US, "Capcom")
        add("SLUS-21019", "Viewtiful Joe 2", Ps2Region.US, "Capcom")
        add("SLUS-21115", "Haunting Ground", Ps2Region.US, "Capcom")

        // Rockstar Hits
        add("SLUS-20834", "Bully", Ps2Region.US, "Rockstar Games")
        add("SLUS-20239", "Max Payne", Ps2Region.US, "Rockstar Games")
        add("SLUS-20814", "Max Payne 2: The Fall of Max Payne", Ps2Region.US, "Rockstar Games")
        add("SLUS-20874", "Manhunt", Ps2Region.US, "Rockstar Games")
        add("SLUS-21613", "Manhunt 2", Ps2Region.US, "Rockstar Games")
        add("SLUS-21216", "The Warriors", Ps2Region.US, "Rockstar Games")
        add("SLUS-20508", "Red Dead Revolver", Ps2Region.US, "Rockstar Games")

        // Need for Speed Series
        add("SLUS-20362", "Need for Speed: Hot Pursuit 2", Ps2Region.US, "EA Games")
        add("SLUS-20811", "Need for Speed: Underground", Ps2Region.US, "EA Games")
        add("SLUS-21065", "Need for Speed: Underground 2", Ps2Region.US, "EA Games")
        add("SLUS-21258", "Need for Speed: Most Wanted", Ps2Region.US, "EA Games")
        add("SLUS-21494", "Need for Speed: Carbon", Ps2Region.US, "EA Games")
        add("SLUS-21665", "Need for Speed: ProStreet", Ps2Region.US, "EA Games")
        add("SLUS-21808", "Need for Speed: Undercover", Ps2Region.US, "EA Games")

        // Burnout Series
        add("SLUS-20317", "Burnout", Ps2Region.US, "Acclaim")
        add("SLUS-20646", "Burnout 2: Point of Impact", Ps2Region.US, "Acclaim")
        add("SLUS-21050", "Burnout 3: Takedown", Ps2Region.US, "EA Games")
        add("SLUS-21242", "Burnout Revenge", Ps2Region.US, "EA Games")
        add("SLUS-21586", "Burnout Dominator", Ps2Region.US, "EA Games")

        // Midnight Club Series
        add("SLUS-20063", "Midnight Club: Street Racing", Ps2Region.US, "Rockstar Games")
        add("SLUS-20280", "Midnight Club II", Ps2Region.US, "Rockstar Games")
        add("SLUS-21029", "Midnight Club 3: DUB Edition", Ps2Region.US, "Rockstar Games")
        add("SLUS-21355", "Midnight Club 3: DUB Edition Remix", Ps2Region.US, "Rockstar Games")

        // Ratchet & Clank Series
        add("SCUS-97199", "Ratchet & Clank", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97268", "Ratchet & Clank: Going Commando", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97353", "Ratchet & Clank: Up Your Arsenal", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97465", "Ratchet: Deadlocked", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97619", "Ratchet & Clank: Size Matters", Ps2Region.US, "Sony Computer Entertainment")

        // Jak and Daxter Series
        add("SCUS-97124", "Jak and Daxter: The Precursor Legacy", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97265", "Jak II", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97330", "Jak 3", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97429", "Jak X: Combat Racing", Ps2Region.US, "Sony Computer Entertainment")

        // Sly Cooper Series
        add("SCUS-97198", "Sly Cooper and the Thievius Raccoonus", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97316", "Sly 2: Band of Thieves", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97464", "Sly 3: Honor Among Thieves", Ps2Region.US, "Sony Computer Entertainment")

        // Crash & Spyro
        add("SLUS-20238", "Crash Bandicoot: The Wrath of Cortex", Ps2Region.US, "Universal Interactive")
        add("SLUS-20875", "Crash Twinsanity", Ps2Region.US, "Vivendi Universal")
        add("SLUS-20649", "Crash Nitro Kart", Ps2Region.US, "Universal Interactive")
        add("SLUS-21151", "Crash Tag Team Racing", Ps2Region.US, "Vivendi Universal")
        add("SLUS-20412", "Spyro: Enter the Dragonfly", Ps2Region.US, "Universal Interactive")
        add("SLUS-20973", "Spyro: A Hero's Tail", Ps2Region.US, "Vivendi Universal")

        // Star Wars
        add("SLUS-21014", "Star Wars: Battlefront", Ps2Region.US, "LucasArts")
        add("SLUS-21240", "Star Wars: Battlefront II", Ps2Region.US, "LucasArts")
        add("SLUS-21774", "Star Wars: The Force Unleashed", Ps2Region.US, "LucasArts")
        add("SLUS-20416", "Star Wars: Bounty Hunter", Ps2Region.US, "LucasArts")
        add("SLUS-21175", "Star Wars Episode III: Revenge of the Sith", Ps2Region.US, "LucasArts")

        // Mortal Kombat Series
        add("SLUS-20490", "Mortal Kombat: Deadly Alliance", Ps2Region.US, "Midway")
        add("SLUS-20977", "Mortal Kombat: Deception", Ps2Region.US, "Midway")
        add("SLUS-21487", "Mortal Kombat: Armageddon", Ps2Region.US, "Midway")
        add("SLUS-21087", "Mortal Kombat: Shaolin Monks", Ps2Region.US, "Midway")

        // Dragon Ball Z Series
        add("SLUS-20590", "Dragon Ball Z: Budokai", Ps2Region.US, "Bandai")
        add("SLUS-20790", "Dragon Ball Z: Budokai 2", Ps2Region.US, "Bandai")
        add("SLUS-20998", "Dragon Ball Z: Budokai 3", Ps2Region.US, "Atari")
        add("SLUS-21217", "Dragon Ball Z: Budokai Tenkaichi", Ps2Region.US, "Atari")
        add("SLUS-21441", "Dragon Ball Z: Budokai Tenkaichi 2", Ps2Region.US, "Atari")
        add("SLUS-21670", "Dragon Ball Z: Budokai Tenkaichi 3", Ps2Region.US, "Atari")
        add("SLUS-21822", "Dragon Ball Z: Infinite World", Ps2Region.US, "Atari")

        // Naruto Series
        add("SLUS-21421", "Naruto: Ultimate Ninja", Ps2Region.US, "Bandai Namco")
        add("SLUS-21589", "Naruto: Ultimate Ninja 2", Ps2Region.US, "Bandai Namco")
        add("SLUS-21727", "Naruto: Ultimate Ninja 3", Ps2Region.US, "Bandai Namco")
        add("SLUS-21850", "Naruto Shippuden: Ultimate Ninja 4", Ps2Region.US, "Bandai Namco")
        add("SLUS-21893", "Naruto Shippuden: Ultimate Ninja 5", Ps2Region.US, "Bandai Namco")

        // Tony Hawk Series
        add("SLUS-20224", "Tony Hawk's Pro Skater 3", Ps2Region.US, "Activision")
        add("SLUS-20485", "Tony Hawk's Pro Skater 4", Ps2Region.US, "Activision")
        add("SLUS-20739", "Tony Hawk's Underground", Ps2Region.US, "Activision")
        add("SLUS-21010", "Tony Hawk's Underground 2", Ps2Region.US, "Activision")
        add("SLUS-21257", "Tony Hawk's American Wasteland", Ps2Region.US, "Activision")

        // Guitar Hero Series
        add("SLUS-21269", "Guitar Hero", Ps2Region.US, "RedOctane")
        add("SLUS-21447", "Guitar Hero II", Ps2Region.US, "RedOctane")
        add("SLUS-21672", "Guitar Hero III: Legends of Rock", Ps2Region.US, "Activision")
        add("SLUS-21614", "Guitar Hero: Rocks the 80s", Ps2Region.US, "Activision")

        // Fighting & Def Jam
        add("SLUS-20665", "Def Jam Vendetta", Ps2Region.US, "EA Games")
        add("SLUS-21004", "Def Jam: Fight for NY", Ps2Region.US, "EA Games")

        // Katamari
        add("SLUS-21008", "Katamari Damacy", Ps2Region.US, "Namco")
        add("SLUS-21230", "We Love Katamari", Ps2Region.US, "Namco")

        // Prince of Persia
        add("SLUS-20743", "Prince of Persia: The Sands of Time", Ps2Region.US, "Ubisoft")
        add("SLUS-21022", "Prince of Persia: Warrior Within", Ps2Region.US, "Ubisoft")
        add("SLUS-21287", "Prince of Persia: The Two Thrones", Ps2Region.US, "Ubisoft")

        // Level-5 RPGs
        add("SCUS-97111", "Dark Cloud", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97213", "Dark Cloud 2", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97490", "Rogue Galaxy", Ps2Region.US, "Sony Computer Entertainment")

        // Star Ocean & Tri-Ace
        add("SLUS-20403", "Star Ocean: Till the End of Time (Disc 1)", Ps2Region.US, "Square Enix")
        add("SLUS-20891", "Star Ocean: Till the End of Time (Disc 2)", Ps2Region.US, "Square Enix")
        add("SLUS-21481", "Valkyrie Profile 2: Silmeria", Ps2Region.US, "Square Enix")

        // Xenosaga Series
        add("SLUS-20469", "Xenosaga Episode I: Der Wille zur Macht", Ps2Region.US, "Namco")
        add("SLUS-20807", "Xenosaga Episode II (Disc 1)", Ps2Region.US, "Namco")
        add("SLUS-20808", "Xenosaga Episode II (Disc 2)", Ps2Region.US, "Namco")
        add("SLUS-21389", "Xenosaga Episode III (Disc 1)", Ps2Region.US, "Namco")
        add("SLUS-21390", "Xenosaga Episode III (Disc 2)", Ps2Region.US, "Namco")

        // Castlevania
        add("SLUS-20733", "Castlevania: Lament of Innocence", Ps2Region.US, "Konami")
        add("SLUS-21168", "Castlevania: Curse of Darkness", Ps2Region.US, "Konami")

        // Onimusha Series
        add("SLUS-20093", "Onimusha: Warlords", Ps2Region.US, "Capcom")
        add("SLUS-20393", "Onimusha 2: Samurai's Destiny", Ps2Region.US, "Capcom")
        add("SLUS-20694", "Onimusha 3: Demon Siege", Ps2Region.US, "Capcom")
        add("SLUS-21180", "Onimusha: Dawn of Dreams (Disc 1)", Ps2Region.US, "Capcom")
        add("SLUS-21370", "Onimusha: Dawn of Dreams (Disc 2)", Ps2Region.US, "Capcom")

        // Shooter / Action
        add("SLUS-21348", "BLACK", Ps2Region.US, "EA Games")
        add("SCUS-97402", "Killzone", Ps2Region.US, "Sony Computer Entertainment")
        add("SLUS-20497", "TimeSplitters 2", Ps2Region.US, "Eidos Interactive")
        add("SLUS-21110", "TimeSplitters: Future Perfect", Ps2Region.US, "EA Games")
        add("SLUS-20392", "Medal of Honor: Frontline", Ps2Region.US, "EA Games")
        add("SLUS-20757", "Medal of Honor: Rising Sun", Ps2Region.US, "EA Games")
        add("SLUS-20932", "Call of Duty: Finest Hour", Ps2Region.US, "Activision")
        add("SLUS-21228", "Call of Duty 2: Big Red One", Ps2Region.US, "Activision")
        add("SLUS-21426", "Call of Duty 3", Ps2Region.US, "Activision")

        // SOCOM Series
        add("SCUS-97134", "SOCOM: U.S. Navy SEALs", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97275", "SOCOM II: U.S. Navy SEALs", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97479", "SOCOM 3: U.S. Navy SEALs", Ps2Region.US, "Sony Computer Entertainment")
        add("SCUS-97534", "SOCOM: Combined Assault", Ps2Region.US, "Sony Computer Entertainment")

        // Ace Combat Series
        add("SLUS-20152", "Ace Combat 04: Shattered Skies", Ps2Region.US, "Namco")
        add("SLUS-20851", "Ace Combat 5: The Unsung War", Ps2Region.US, "Namco")
        add("SLUS-21346", "Ace Combat Zero: The Belkan War", Ps2Region.US, "Namco")

        // Yakuza Series
        add("SLUS-21349", "Yakuza", Ps2Region.US, "Sega")
        add("SLUS-21769", "Yakuza 2", Ps2Region.US, "Sega")

        // Horror & Cult Classics
        add("SLUS-20388", "Fatal Frame", Ps2Region.US, "Tecmo")
        add("SLUS-20766", "Fatal Frame II: Crimson Butterfly", Ps2Region.US, "Tecmo")
        add("SLUS-21255", "Fatal Frame III: The Tormented", Ps2Region.US, "Tecmo")
        add("SCUS-97355", "Siren", Ps2Region.US, "Sony Computer Entertainment")
        add("SLUS-21448", "Rule of Rose", Ps2Region.US, "Atlus")
        add("SLUS-20840", "Kuon", Ps2Region.US, "Agetec")
        add("SLUS-20641", "Clock Tower 3", Ps2Region.US, "Capcom")

        // WWE Series
        add("SLUS-20486", "WWE SmackDown! Shut Your Mouth", Ps2Region.US, "THQ")
        add("SLUS-20787", "WWE SmackDown! Here Comes the Pain", Ps2Region.US, "THQ")
        add("SLUS-21264", "WWE SmackDown! vs. Raw 2006", Ps2Region.US, "THQ")
        add("SLUS-21469", "WWE SmackDown vs. Raw 2007", Ps2Region.US, "THQ")
        add("SLUS-21639", "WWE SmackDown vs. Raw 2008", Ps2Region.US, "THQ")
        add("SLUS-21789", "WWE SmackDown vs. Raw 2009", Ps2Region.US, "THQ")
        add("SLUS-21884", "WWE SmackDown vs. Raw 2010", Ps2Region.US, "THQ")
        add("SLUS-21926", "WWE SmackDown vs. Raw 2011", Ps2Region.US, "THQ")

        // Sports & Fun
        add("SLUS-21453", "FIFA 07", Ps2Region.US, "EA Sports")
        add("SLUS-21644", "FIFA 08", Ps2Region.US, "EA Sports")
        add("SLUS-21798", "FIFA 09", Ps2Region.US, "EA Sports")
        add("SLUS-21889", "FIFA 10", Ps2Region.US, "EA Sports")
        add("SLUS-21917", "FIFA 11", Ps2Region.US, "EA Sports")
        add("SLUS-21946", "FIFA 12", Ps2Region.US, "EA Sports")
        add("SLUS-21956", "FIFA 13", Ps2Region.US, "EA Sports")
        add("SLUS-21960", "FIFA 14", Ps2Region.US, "EA Sports")
        add("SLUS-21484", "Pro Evolution Soccer 2007", Ps2Region.US, "Konami")
        add("SLUS-20612", "NBA Street Vol. 2", Ps2Region.US, "EA Sports BIG")
        add("SLUS-20986", "NBA Street V3", Ps2Region.US, "EA Sports BIG")
        add("SLUS-20668", "SSX 3", Ps2Region.US, "EA Sports BIG")
        add("SLUS-20227", "SSX Tricky", Ps2Region.US, "EA Sports BIG")

        // Disney / Licensed / Classics
        add("SLUS-20624", "The Simpsons: Hit & Run", Ps2Region.US, "Vivendi Universal")
        add("SLUS-20776", "Spider-Man 2", Ps2Region.US, "Activision")
        add("SLUS-21083", "Lego Star Wars: The Video Game", Ps2Region.US, "Eidos Interactive")
        add("SLUS-21409", "Lego Star Wars II: The Original Trilogy", Ps2Region.US, "LucasArts")
        add("SLUS-20822", "Sonic Heroes", Ps2Region.US, "Sega")
        add("SLUS-20993", "Sonic Mega Collection Plus", Ps2Region.US, "Sega")
        add("SLUS-21323", "Disgaea: Hour of Darkness", Ps2Region.US, "Atlus")
        add("SLUS-21397", "Disgaea 2: Cursed Memories", Ps2Region.US, "NIS America")


        // ==================== EUROPE (PAL) ====================
        add("SLES-50490", "Final Fantasy X", Ps2Region.EU, "Square Enix")
        add("SLES-51815", "Final Fantasy X-2", Ps2Region.EU, "Square Enix")
        add("SLES-54354", "Final Fantasy XII", Ps2Region.EU, "Square Enix")
        add("SLES-54185", "Dirge of Cerberus: Final Fantasy VII", Ps2Region.EU, "Square Enix")

        add("SLES-50330", "Grand Theft Auto III", Ps2Region.EU, "Rockstar Games")
        add("SLES-51061", "Grand Theft Auto: Vice City", Ps2Region.EU, "Rockstar Games")
        add("SLES-52541", "Grand Theft Auto: San Andreas", Ps2Region.EU, "Rockstar Games")
        add("SLES-54135", "Grand Theft Auto: Liberty City Stories", Ps2Region.EU, "Rockstar Games")
        add("SLES-54622", "Grand Theft Auto: Vice City Stories", Ps2Region.EU, "Rockstar Games")

        add("SLES-51283", "Kingdom Hearts", Ps2Region.EU, "Square Enix")
        add("SLES-54114", "Kingdom Hearts II", Ps2Region.EU, "Square Enix")

        add("SLES-50382", "Metal Gear Solid 2: Sons of Liberty", Ps2Region.EU, "Konami")
        add("SLES-51156", "Metal Gear Solid 2: Substance", Ps2Region.EU, "Konami")
        add("SLES-52584", "Metal Gear Solid 3: Snake Eater", Ps2Region.EU, "Konami")
        add("SLES-54157", "Metal Gear Solid 3: Subsistence", Ps2Region.EU, "Konami")

        add("SCES-53133", "God of War", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-54206", "God of War II", Ps2Region.EU, "Sony Computer Entertainment")

        add("SLES-53702", "Resident Evil 4", Ps2Region.EU, "Capcom")
        add("SLES-50306", "Resident Evil: Code: Veronica X", Ps2Region.EU, "Capcom")
        add("SLES-52467", "Resident Evil Outbreak", Ps2Region.EU, "Capcom")

        add("SLES-50383", "Silent Hill 2", Ps2Region.EU, "Konami")
        add("SLES-51434", "Silent Hill 3", Ps2Region.EU, "Konami")
        add("SLES-52730", "Silent Hill 4: The Room", Ps2Region.EU, "Konami")

        add("SLES-53363", "Shin Megami Tensei: Lucifer's Call (Nocturne)", Ps2Region.EU, "Ghostlight")
        add("SLES-54439", "Shin Megami Tensei: Persona 3", Ps2Region.EU, "Koei")
        add("SLES-55354", "Shin Megami Tensei: Persona 3 FES", Ps2Region.EU, "Koei")
        add("SLES-55474", "Shin Megami Tensei: Persona 4", Ps2Region.EU, "Square Enix")

        add("SCES-50760", "ICO", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-53326", "Shadow of the Colossus", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCES-50294", "Gran Turismo 3: A-Spec", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-51719", "Gran Turismo 4", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCES-50001", "Tekken Tag Tournament", Ps2Region.EU, "Namco")
        add("SCES-50878", "Tekken 4", Ps2Region.EU, "Namco")
        add("SCES-53202", "Tekken 5", Ps2Region.EU, "Namco")

        add("SLES-50358", "Devil May Cry", Ps2Region.EU, "Capcom")
        add("SLES-51421", "Devil May Cry 2", Ps2Region.EU, "Capcom")
        add("SLES-53038", "Devil May Cry 3: Dante's Awakening", Ps2Region.EU, "Capcom")
        add("SLES-54186", "Devil May Cry 3: Special Edition", Ps2Region.EU, "Capcom")

        add("SLES-53970", "Dragon Quest: The Journey of the Cursed King", Ps2Region.EU, "Square Enix")
        add("SLES-54435", "Okami", Ps2Region.EU, "Capcom")
        add("SLES-53544", "Bully (Canis Canem Edit)", Ps2Region.EU, "Rockstar Games")
        add("SLES-51203", "Burnout 2: Point of Impact", Ps2Region.EU, "Acclaim")
        add("SLES-52585", "Burnout 3: Takedown", Ps2Region.EU, "EA Games")
        add("SLES-53507", "Burnout Revenge", Ps2Region.EU, "EA Games")

        add("SLES-52151", "Need for Speed: Underground", Ps2Region.EU, "EA Games")
        add("SLES-52725", "Need for Speed: Underground 2", Ps2Region.EU, "EA Games")
        add("SLES-53558", "Need for Speed: Most Wanted", Ps2Region.EU, "EA Games")
        add("SLES-54327", "Need for Speed: Carbon", Ps2Region.EU, "EA Games")

        add("SCES-50916", "Ratchet & Clank", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-51607", "Ratchet & Clank 2 (Going Commando)", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-52453", "Ratchet & Clank 3 (Up Your Arsenal)", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-53285", "Ratchet: Gladiator (Deadlocked)", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCES-50361", "Jak and Daxter: The Precursor Legacy", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-51608", "Jak II: Renegade", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-52469", "Jak 3", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCES-50978", "Sly Raccoon", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-52529", "Sly 2: Band of Thieves", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCES-53439", "Sly 3: Honour Among Thieves", Ps2Region.EU, "Sony Computer Entertainment")

        add("SLES-54204", "Pro Evolution Soccer 6", Ps2Region.EU, "Konami")
        add("SLES-53540", "Pro Evolution Soccer 5", Ps2Region.EU, "Konami")
        add("SLES-52562", "Pro Evolution Soccer 4", Ps2Region.EU, "Konami")


        // ==================== JAPAN (NTSC-J) ====================
        add("SLPS-25088", "Final Fantasy X", Ps2Region.JP, "Square Enix")
        add("SLPS-25089", "Final Fantasy X International", Ps2Region.JP, "Square Enix")
        add("SLPS-25250", "Final Fantasy X-2", Ps2Region.JP, "Square Enix")
        add("SLPS-25340", "Final Fantasy X-2 International + Last Mission", Ps2Region.JP, "Square Enix")
        add("SLPS-25630", "Final Fantasy XII", Ps2Region.JP, "Square Enix")
        add("SLPS-25750", "Final Fantasy XII International Zodiac Job System", Ps2Region.JP, "Square Enix")

        add("SLPM-65494", "Grand Theft Auto III", Ps2Region.JP, "Capcom")
        add("SLPM-65495", "Grand Theft Auto: Vice City", Ps2Region.JP, "Capcom")
        add("SLPM-65719", "Grand Theft Auto: San Andreas", Ps2Region.JP, "Capcom")

        add("SLPS-25105", "Kingdom Hearts", Ps2Region.JP, "Square Enix")
        add("SLPS-25198", "Kingdom Hearts Final Mix", Ps2Region.JP, "Square Enix")
        add("SLPS-25605", "Kingdom Hearts II", Ps2Region.JP, "Square Enix")
        add("SLPS-25760", "Kingdom Hearts II Final Mix+", Ps2Region.JP, "Square Enix")

        add("SLPM-65060", "Metal Gear Solid 2: Sons of Liberty", Ps2Region.JP, "Konami")
        add("SLPM-65220", "Metal Gear Solid 2: Substance", Ps2Region.JP, "Konami")
        add("SLPM-65780", "Metal Gear Solid 3: Snake Eater", Ps2Region.JP, "Konami")
        add("SLPM-66221", "Metal Gear Solid 3: Subsistence", Ps2Region.JP, "Konami")

        add("SLPM-65100", "Biohazard: Code: Veronica Complete", Ps2Region.JP, "Capcom")
        add("SLPM-65460", "Biohazard Outbreak", Ps2Region.JP, "Capcom")
        add("SLPM-66213", "Biohazard 4", Ps2Region.JP, "Capcom")

        add("SLPM-65051", "Silent Hill 2", Ps2Region.JP, "Konami")
        add("SLPM-65288", "Silent Hill 3", Ps2Region.JP, "Konami")
        add("SLPM-65574", "Silent Hill 4: The Room", Ps2Region.JP, "Konami")

        add("SLPM-65232", "Shin Megami Tensei III: Nocturne", Ps2Region.JP, "Atlus")
        add("SLPM-65462", "Shin Megami Tensei III: Nocturne Maniax", Ps2Region.JP, "Atlus")
        add("SLPM-66445", "Persona 3", Ps2Region.JP, "Atlus")
        add("SLPM-66690", "Persona 3 FES", Ps2Region.JP, "Atlus")
        add("SLPM-66975", "Persona 4", Ps2Region.JP, "Atlus")

        add("SCPS-15007", "ICO", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-15097", "Wanda to Kyozou (Shadow of the Colossus)", Ps2Region.JP, "Sony Computer Entertainment")

        add("SCPS-15009", "Gran Turismo 3: A-Spec", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-17001", "Gran Turismo 4", Ps2Region.JP, "Sony Computer Entertainment")

        add("SLPS-20001", "Tekken Tag Tournament", Ps2Region.JP, "Namco")
        add("SLPS-25100", "Tekken 4", Ps2Region.JP, "Namco")
        add("SLPS-25510", "Tekken 5", Ps2Region.JP, "Namco")

        add("SLPM-65038", "Devil May Cry", Ps2Region.JP, "Capcom")
        add("SLPM-65239", "Devil May Cry 2", Ps2Region.JP, "Capcom")
        add("SLPM-65880", "Devil May Cry 3", Ps2Region.JP, "Capcom")

        add("SLPM-65888", "Dragon Quest VIII: Sora to Umi to Daichi to Norowareshi Himegimi", Ps2Region.JP, "Square Enix")
        add("SLPM-65478", "Dragon Quest V: Tenkuu no Hanayome", Ps2Region.JP, "Square Enix")

        add("SLPM-66373", "Okami", Ps2Region.JP, "Capcom")
        add("SLPM-66523", "God Hand", Ps2Region.JP, "Capcom")
        add("SLPM-66233", "Demento (Haunting Ground)", Ps2Region.JP, "Capcom")

        add("SCPS-15024", "Ratchet & Clank", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-15056", "Ratchet & Clank 2", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-15084", "Ratchet & Clank 3", Ps2Region.JP, "Sony Computer Entertainment")

        add("SCPS-15021", "Jak and Daxter", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-15057", "Jak II", Ps2Region.JP, "Sony Computer Entertainment")

        add("SLPM-66449", "Tales of the Abyss", Ps2Region.JP, "Namco")
        add("SLPS-25400", "Tales of Rebirth", Ps2Region.JP, "Namco")
        add("SLPS-25842", "Tales of Destiny", Ps2Region.JP, "Namco")
        add("SLPS-25884", "Tales of Destiny 2", Ps2Region.JP, "Namco")

        add("SLPS-25650", "Yakuza (Ryu ga Gotoku)", Ps2Region.JP, "Sega")
        add("SLPS-25725", "Yakuza 2 (Ryu ga Gotoku 2)", Ps2Region.JP, "Sega")

        add("SLPM-62150", "Zero (Fatal Frame)", Ps2Region.JP, "Tecmo")
        add("SLPS-25303", "Zero: Akai Chou (Fatal Frame II)", Ps2Region.JP, "Tecmo")
        add("SLPS-25544", "Zero: Shisei no Koe (Fatal Frame III)", Ps2Region.JP, "Tecmo")

        add("SCPS-15058", "Siren", Ps2Region.JP, "Sony Computer Entertainment")
        add("SCPS-15103", "Siren 2", Ps2Region.JP, "Sony Computer Entertainment")

        add("SLPM-65666", "Monster Hunter", Ps2Region.JP, "Capcom")
        add("SLPM-65869", "Monster Hunter G", Ps2Region.JP, "Capcom")
        add("SLPM-66280", "Monster Hunter 2 (Dos)", Ps2Region.JP, "Capcom")

        add("SLPM-65983", "Sengoku Basara", Ps2Region.JP, "Capcom")
        add("SLPM-66480", "Sengoku Basara 2", Ps2Region.JP, "Capcom")
        add("SLPM-66880", "Sengoku Basara 2 Heroes", Ps2Region.JP, "Capcom")

        add("SLPM-66020", "World Soccer Winning Eleven 9", Ps2Region.JP, "Konami")
        add("SLPM-66370", "World Soccer Winning Eleven 10", Ps2Region.JP, "Konami")

        // ==================== PLAYSTATION 1 (PS1) CLASSICS ====================
        // Final Fantasy Series (PS1)
        add("SCUS-94163", "Final Fantasy VII", Ps2Region.US, "Square")
        add("SCES-00867", "Final Fantasy VII", Ps2Region.EU, "Square")
        add("SCES-00868", "Final Fantasy VII (German)", Ps2Region.EU, "Square")
        add("SCES-00869", "Final Fantasy VII (French)", Ps2Region.EU, "Square")
        add("SCES-00900", "Final Fantasy VII (Spanish)", Ps2Region.EU, "Square")
        add("SLPS-00700", "Final Fantasy VII", Ps2Region.JP, "Square")
        add("SLPS-01057", "Final Fantasy VII International", Ps2Region.JP, "Square")

        add("SLUS-00892", "Final Fantasy VIII", Ps2Region.US, "Square")
        add("SLES-02080", "Final Fantasy VIII", Ps2Region.EU, "Square")
        add("SLPS-01880", "Final Fantasy VIII", Ps2Region.JP, "Square")

        add("SLUS-01251", "Final Fantasy IX", Ps2Region.US, "Square")
        add("SLES-02965", "Final Fantasy IX", Ps2Region.EU, "Square")
        add("SLPS-02000", "Final Fantasy IX", Ps2Region.JP, "Square")

        add("SCUS-94221", "Final Fantasy Tactics", Ps2Region.US, "Square")
        add("SLPS-00770", "Final Fantasy Tactics", Ps2Region.JP, "Square")

        // Metal Gear Solid Series (PS1)
        add("SLUS-00844", "Metal Gear Solid", Ps2Region.US, "Konami")
        add("SLUS-00957", "Metal Gear Solid: VR Missions", Ps2Region.US, "Konami")
        add("SLES-01507", "Metal Gear Solid", Ps2Region.EU, "Konami")
        add("SLPS-01500", "Metal Gear Solid", Ps2Region.JP, "Konami")
        add("SLPS-02170", "Metal Gear Solid: Integral", Ps2Region.JP, "Konami")

        // Resident Evil Series (PS1)
        add("SLUS-00170", "Resident Evil", Ps2Region.US, "Capcom")
        add("SLUS-00551", "Resident Evil: Director's Cut", Ps2Region.US, "Capcom")
        add("SLES-00200", "Resident Evil", Ps2Region.EU, "Capcom")
        add("SLPS-00222", "Biohazard", Ps2Region.JP, "Capcom")

        add("SLUS-00748", "Resident Evil 2 (DualShock)", Ps2Region.US, "Capcom")
        add("SLUS-00421", "Resident Evil 2", Ps2Region.US, "Capcom")
        add("SLES-00972", "Resident Evil 2", Ps2Region.EU, "Capcom")
        add("SLPS-01222", "Biohazard 2", Ps2Region.JP, "Capcom")

        add("SLUS-00923", "Resident Evil 3: Nemesis", Ps2Region.US, "Capcom")
        add("SLES-02539", "Resident Evil 3: Nemesis", Ps2Region.EU, "Capcom")
        add("SLPS-02300", "Biohazard 3: Last Escape", Ps2Region.JP, "Capcom")

        // Castlevania, Silent Hill & Horror (PS1)
        add("SLUS-00067", "Castlevania: Symphony of the Night", Ps2Region.US, "Konami")
        add("SLES-00524", "Castlevania: Symphony of the Night", Ps2Region.EU, "Konami")
        add("SLPM-86023", "Akumajou Dracula X: Gekka no Yasoukyoku", Ps2Region.JP, "Konami")

        add("SLUS-00972", "Silent Hill", Ps2Region.US, "Konami")
        add("SLES-01514", "Silent Hill", Ps2Region.EU, "Konami")
        add("SLPM-86192", "Silent Hill", Ps2Region.JP, "Konami")

        add("SLUS-00922", "Dino Crisis", Ps2Region.US, "Capcom")
        add("SLES-02207", "Dino Crisis", Ps2Region.EU, "Capcom")
        add("SLUS-01279", "Dino Crisis 2", Ps2Region.US, "Capcom")
        add("SLES-03221", "Dino Crisis 2", Ps2Region.EU, "Capcom")

        add("SLUS-00662", "Parasite Eve", Ps2Region.US, "Square")
        add("SLPS-01303", "Parasite Eve", Ps2Region.JP, "Square")
        add("SLUS-01042", "Parasite Eve II", Ps2Region.US, "Square")
        add("SLES-02558", "Parasite Eve II", Ps2Region.EU, "Square")

        // Crash Bandicoot & Spyro Series (PS1)
        add("SCUS-94900", "Crash Bandicoot", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-00344", "Crash Bandicoot", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCPS-10031", "Crash Bandicoot", Ps2Region.JP, "Sony Computer Entertainment")

        add("SCUS-94154", "Crash Bandicoot 2: Cortex Strikes Back", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-00967", "Crash Bandicoot 2: Cortex Strikes Back", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94244", "Crash Bandicoot 3: Warped", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-01420", "Crash Bandicoot 3: Warped", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94394", "Crash Team Racing", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-02105", "Crash Team Racing", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94228", "Spyro the Dragon", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-01438", "Spyro the Dragon", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94425", "Spyro 2: Ripto's Rage!", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-02104", "Spyro 2: Gateway to Glimmer", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94463", "Spyro: Year of the Dragon", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-02835", "Spyro: Year of the Dragon", Ps2Region.EU, "Sony Computer Entertainment")

        // Tekken Series (PS1)
        add("SLUS-00402", "Tekken 3", Ps2Region.US, "Namco")
        add("SCES-01237", "Tekken 3", Ps2Region.EU, "Namco")
        add("SLPS-01300", "Tekken 3", Ps2Region.JP, "Namco")

        add("SLUS-00213", "Tekken 2", Ps2Region.US, "Namco")
        add("SCES-00255", "Tekken 2", Ps2Region.EU, "Namco")
        add("SLPS-00300", "Tekken 2", Ps2Region.JP, "Namco")

        // Gran Turismo Series (PS1)
        add("SCUS-94194", "Gran Turismo", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-00984", "Gran Turismo", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCPS-10045", "Gran Turismo", Ps2Region.JP, "Sony Computer Entertainment")

        add("SCUS-94455", "Gran Turismo 2", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-02380", "Gran Turismo 2", Ps2Region.EU, "Sony Computer Entertainment")
        add("SCPS-10116", "Gran Turismo 2", Ps2Region.JP, "Sony Computer Entertainment")

        // RPGs & Classics (PS1)
        add("SLUS-00664", "Xenogears", Ps2Region.US, "Square")
        add("SLPS-01160", "Xenogears", Ps2Region.JP, "Square")

        add("SLUS-01041", "Chrono Cross", Ps2Region.US, "Square")
        add("SLPS-02364", "Chrono Cross", Ps2Region.JP, "Square")

        add("SLUS-01363", "Chrono Trigger (Final Fantasy Chronicles)", Ps2Region.US, "Square")

        add("SLUS-01040", "Vagrant Story", Ps2Region.US, "Square")
        add("SLES-02755", "Vagrant Story", Ps2Region.EU, "Square")
        add("SLPS-02500", "Vagrant Story", Ps2Region.JP, "Square")

        add("SLUS-00292", "Suikoden", Ps2Region.US, "Konami")
        add("SLES-00527", "Suikoden", Ps2Region.EU, "Konami")

        add("SLUS-00958", "Suikoden II", Ps2Region.US, "Konami")
        add("SLES-02442", "Suikoden II", Ps2Region.EU, "Konami")

        add("SLUS-01066", "Tony Hawk's Pro Skater 2", Ps2Region.US, "Activision")
        add("SLES-02908", "Tony Hawk's Pro Skater 2", Ps2Region.EU, "Activision")

        add("SLUS-00152", "Tomb Raider", Ps2Region.US, "Eidos Interactive")
        add("SLES-00024", "Tomb Raider", Ps2Region.EU, "Eidos Interactive")

        add("SLUS-00437", "Tomb Raider II", Ps2Region.US, "Eidos Interactive")
        add("SLES-00718", "Tomb Raider II", Ps2Region.EU, "Eidos Interactive")

        add("SCUS-94304", "Twisted Metal 2", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-00511", "Twisted Metal 2", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94240", "Syphon Filter", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-01910", "Syphon Filter", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94457", "Syphon Filter 2", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-02688", "Syphon Filter 2", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94241", "Ape Escape", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-01564", "Ape Escape", Ps2Region.EU, "Sony Computer Entertainment")

        add("SCUS-94227", "MediEvil", Ps2Region.US, "Sony Computer Entertainment")
        add("SCES-00311", "MediEvil", Ps2Region.EU, "Sony Computer Entertainment")

        add("SLUS-00889", "Legacy of Kain: Soul Reaver", Ps2Region.US, "Eidos Interactive")
        add("SLES-01301", "Legacy of Kain: Soul Reaver", Ps2Region.EU, "Eidos Interactive")

        add("SCUS-94204", "Klonoa: Door to Phantomile", Ps2Region.US, "Namco")
        add("SCES-01063", "Klonoa: Door to Phantomile", Ps2Region.EU, "Namco")

        add("SLUS-00005", "Rayman", Ps2Region.US, "Ubisoft")
        add("SLES-00049", "Rayman", Ps2Region.EU, "Ubisoft")

        add("SLUS-00422", "Breath of Fire III", Ps2Region.US, "Capcom")
        add("SLES-01304", "Breath of Fire III", Ps2Region.EU, "Capcom")

        add("SLUS-01324", "Breath of Fire IV", Ps2Region.US, "Capcom")
        add("SLES-03552", "Breath of Fire IV", Ps2Region.EU, "Capcom")

        add("SLUS-00840", "Star Ocean: The Second Story", Ps2Region.US, "Sony Computer Entertainment")
        add("SLES-02585", "Star Ocean: The Second Story", Ps2Region.EU, "Sony Computer Entertainment")

        add("SLUS-00628", "Lunar: Silver Star Story Complete", Ps2Region.US, "Working Designs")
        add("SLUS-01156", "Valkyrie Profile", Ps2Region.US, "Enix")
        add("SLUS-00561", "Mega Man X4", Ps2Region.US, "Capcom")
        add("SCUS-94183", "PaRappa the Rapper", Ps2Region.US, "Sony Computer Entertainment")
    }
}
