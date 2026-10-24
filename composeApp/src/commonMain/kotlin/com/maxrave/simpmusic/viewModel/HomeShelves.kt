package com.maxrave.simpmusic.viewModel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Which of Home's known sections a shelf is, whatever language it came back in. Ported from
// Limusic desktop (crates/innertube/src/models/home_shelves.rs, captured from live signed-in feeds
// on 2026-10-15). YouTube sends no id for a home shelf that survives a change of language, so three
// are told apart by where their "More" button goes and the rest by title, compared ignoring case.
// A title missing here costs that shelf on Home in that language and nothing else; when YouTube
// rewords one, add the new wording next to the old.

/** Kinds named by their "More" button's browseId, the same in every language. */
private val ByMore =
    mapOf(
        "FEmusic_library_landing" to "from_library",
        "FEmusic_new_releases_albums" to "new_releases",
        "FEmusic_mixed_for_you" to "mixed_for_you",
    )

/** Kinds named by title, every language in one list. */
private val ByTitle: Map<String, List<String>> =
    mapOf(
        "listen_again" to
            listOf(
                "Listen again", // en
                "Noch mal anhören", // de
                "Vuelve a escucharlo", // es
                "Réécouter", // fr
                "Dengarkan lagi", // id
                "Ascolta di nuovo", // it
                "もう一度聴く", // ja
                "다시 듣기", // ko
                "Posłuchaj jeszcze raz", // pl
                "Ouvir de novo", // pt-BR
                "Ascultă din nou", // ro
                "Послушать ещё раз", // ru
                "மீண்டும் கேளுங்கள்", // ta
                "Yeniden dinleyin", // tr
                "Послухати ще раз", // uk
                "Nghe lại", // vi
                "再听一遍", // zh-Hans
                "再聽一次", // zh-Hant
            ),
        "quick_picks" to
            listOf(
                "Quick picks", // en
                "Schnellauswahl", // de
                "Selecciones rápidas", // es
                "Sélection rapide", // fr
                "Pilihan cepat", // id
                "Scelte rapide", // it
                "おすすめ", // ja
                "빠른 선곡", // ko
                "Szybki wybór", // pl
                "Escolha a dedo", // pt-BR
                "Selecții rapide", // ro
                "Рекомендуем", // ru
                "விரைவுத் தேர்வுகள்", // ta
                "Hızlı seçimler", // tr
                "Швидкий вибір", // uk
                "Chọn nhanh đài phát", // vi
                "猜你喜欢", // zh-Hans
                "歌曲快選", // zh-Hant
            ),
        "from_library" to
            listOf(
                "From your library", // en
                "Aus deiner Mediathek", // de
                "De tu biblioteca", // es
                "De votre bibliothèque", // fr
                "Dari koleksi Anda", // id
                "Dalla tua raccolta", // it
                "ライブラリから", // ja
                "내 보관함에 있는 음악", // ko
                "Z Twojej biblioteki", // pl
                "Da sua biblioteca", // pt-BR
                "Din biblioteca ta", // ro
                "Из вашей библиотеки", // ru
                "உங்கள் லைப்ரரியிலிருந்து", // ta
                "Kitaplığınızdan", // tr
                "З вашої бібліотеки", // uk
                "Dựa trên thư viện của bạn", // vi
                "来自你的媒体库", // zh-Hans
                "你的媒體庫", // zh-Hant
            ),
        "community" to
            listOf(
                "From the community", // en
                "Aus der Community", // de
                "De la comunidad", // es
                "De la communauté", // fr
                "Dari komunitas", // id
                "Dalla community", // it
                "コミュニティから", // ja
                "커뮤니티 제공", // ko
                "Od społeczności", // pl
                "Da comunidade", // pt-BR
                "De la comunitate", // ro
                "Плейлисты других пользователей", // ru
                "பிறர் உருவாக்கியவை", // ta
                "Topluluktan", // tr
                "Від спільноти", // uk
                "Danh sách phát mọi người tạo", // vi
                "来自社区", // zh-Hans
                "社群播放清單", // zh-Hant
            ),
        "forgotten" to
            listOf(
                "Forgotten favorites", // en
                "Vergessene Favoriten", // de
                "Favoritos olvidados", // es
                "Favoris à redécouvrir", // fr
                "Favorit yang terlupakan", // id
                "Preferiti che non ascolti da un po'", // it
                "最近聞いていないお気に入り", // ja
                "잊고 있던 좋은 음악", // ko
                "Ulubione utwory z przeszłości", // pl
                "Favoritos antigos", // pt-BR
                "Preferințe uitate", // ro
                "Вы давно не слушали", // ru
                "நீண்டநாள் கேட்காத, பிடித்தமான பாடல்கள்", // ta
                "Unutulan favoriler", // tr
                "Забуте вибране", // uk
                "Những bản nhạc yêu thích dần trôi vào quên lãng", // vi
                "老歌重温", // zh-Hans
                "重溫舊愛", // zh-Hant
            ),
        "fresh_finds" to
            listOf(
                "Fresh finds, old favorites", // en
                "Neue Empfehlungen, alte Favoriten", // de
                "Nuevos descubrimientos y los favoritos de siempre", // es
                "Nouveautés et classiques", // fr
                "Temuan baru, favorit lama", // id
                "Nuove scoperte, vecchi successi", // it
                "新しい曲や過去のお気に入り", // ja
                "새로 발견한 곡과 기존 인기곡", // ko
                "Nowości i ulubione stare przeboje", // pl
                "Novidades e clássicos", // pt-BR
                "Descoperiri noi, preferate vechi", // ro
                "Новинки и любимые треки", // ru
                "புதிதாகக் கண்டறிந்தவையும் விருப்பமான பழையவையும்", // ta
                "Yeni keşifler ve eski favoriler", // tr
                "Нові й старі улюблені пісні", // uk
                "Nhạc mới và những ca khúc cũ bạn thích", // vi
                "新鲜好歌，挚爱经典", // zh-Hans
                "新歌舊愛，一次滿足", // zh-Hant
            ),
        "albums_for_you" to
            listOf(
                "Albums for you", // en
                "Álbumes para ti", // es
                "맞춤 추천 앨범", // ko
                "Albumy dla Ciebie", // pl
                "Albume pentru tine", // ro
                "உங்களுக்கான ஆல்பங்கள்", // ta
                "Sizin için önerilen albümler", // tr
                "Альбоми для вас", // uk
                "Đĩa nhạc cho bạn", // vi
                "專屬推薦專輯", // zh-Hant
            ),
        "trending" to
            listOf(
                "Trending songs for you", // en
                "Músicas em alta para você", // pt-BR
                "Melodii în tendințe pentru tine", // ro
            ),
        "featured_playlists" to
            listOf(
                "Featured playlists for you", // en
                "나를 위한 추천 재생목록", // ko
                "Playlists em destaque para você", // pt-BR
                "你的精選播放清單", // zh-Hant
            ),
        "new_releases" to
            listOf(
                "New releases", // en
                "Neuerscheinungen", // de
                "Últimos lanzamientos", // es
                "Nouveautés", // fr
                "Rilis baru", // id
                "Nuove uscite", // it
                "新作", // ja
                "최신 음악", // ko
                "Nowości", // pl
                "Lançamentos", // pt-BR
                "Lansări noi", // ro
                "Новые релизы", // ru
                "புதிய வெளியீடுகள்", // ta
                "Yeni çıkanlar", // tr
                "Новинки", // uk
                "Đĩa nhạc và đĩa đơn mới phát hành", // vi
                "新曲上线", // zh-Hans
                "最新發行", // zh-Hant
            ),
        "mixed_for_you" to
            listOf(
                "Mixed for you", // en
                "Empfohlene Mixe", // de
                "Mixes personalizados", // es
                "Mixés pour vous", // fr
                "Kompilasi untuk Anda", // id
                "Mixtape per te", // it
                "おすすめのミックス", // ja
                "맞춤 믹스", // ko
                "Składanki dla Ciebie", // pl
                "Mixtapes criadas para você", // pt-BR
                "Mixate pentru tine", // ro
                "Миксы для вас", // ru
                "உங்களுக்கான இசைக் கலவைகள்", // ta
                "Sizin için derlenenler", // tr
                "Мікси для вас", // uk
                "Tuyển tập nhạc cho bạn", // vi
                "个性化合辑", // zh-Hans
                "為你推薦的合輯", // zh-Hant
            ),
        "daily_discover" to
            listOf(
                "Your daily discover", // en
                "Deine Empfehlungen des Tages", // de
                "Tu descubrimiento diario", // es
                "Votre découverte du jour", // fr
                "Rekomendasi harian Anda", // id
                "La tua scoperta quotidiana", // it
                "毎日のおすすめ", // ja
                "오늘의 발견", // ko
                "Nowości na dziś", // pl
                "Suas descobertas diárias", // pt-BR
                "Descoperirea ta zilnică", // ro
                "Находки дня", // ru
                "தினசரிப் பரிந்துரைகள்", // ta
                "Günlük keşif", // tr
                "Щоденні рекомендації", // uk
                "Nhạc mới mỗi ngày", // vi
                "每日发现", // zh-Hans
                "每日探索", // zh-Hant
            ),
        "music_videos" to
            listOf(
                "Music videos for you", // en
                "Musikvideos für dich", // de
                "Clips musicaux pour vous", // fr
                "Video musicali per te", // it
                "おすすめのミュージック ビデオ", // ja
                "Vídeos de música recomendados", // pt-BR
                "为你推荐的音乐视频", // zh-Hans
            ),
    )

private val TitleToKind: Map<String, String> =
    ByTitle.flatMap { (kind, titles) -> titles.map { it.lowercase() to kind } }.toMap()

/** The kind of a Home shelf, or null for one Home doesn't show (an artist's shelf, Recaps...). */
fun homeShelfKind(
    title: String,
    moreBrowseId: String?,
): String? = moreBrowseId?.let(ByMore::get) ?: TitleToKind[title.trim().lowercase()]

/** Home's own section, keyed so a YouTube shelf title can never collide with it. */
const val SHORTCUTS_SECTION = "@shortcuts"

/** Every kind Home shows; Edit Home lists the ones the feed hasn't sent yet in this order. */
val HomeKinds =
    listOf(
        "quick_picks",
        "listen_again",
        "mixed_for_you",
        "forgotten",
        "from_library",
        "community",
        "daily_discover",
        "fresh_finds",
        "albums_for_you",
        "trending",
        "featured_playlists",
        "new_releases",
        "music_videos",
    )

/** Everything Edit Home sets. Sections are keyed by kind ([homeShelfKind]) or [SHORTCUTS_SECTION]. */
@Serializable
data class HomeLayout(
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    /** The mood chip row. */
    val chips: Boolean = true,
    /** The playing cover behind the greeting; off, the header keeps the accent wash. */
    val backdrop: Boolean = true,
    /** Width of the cards on Home's shelves. Rows of songs don't change. */
    val cards: CardSize = CardSize.MEDIUM,
) {
    enum class CardSize { SMALL, MEDIUM, LARGE }

    /**
     * [items] in the user's order, hidden ones included. One with no rank sorts after the ranked
     * ones and keeps the place it arrived in among its peers (a stable sort).
     */
    fun <T> arrange(
        items: List<T>,
        key: (T) -> String,
    ): List<T> {
        val rank = order.withIndex().associate { it.value to it.index }
        return items.sortedBy { rank[key(it)] ?: Int.MAX_VALUE }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(raw: String?): HomeLayout = raw?.let { runCatching { json.decodeFromString<HomeLayout>(it) }.getOrNull() } ?: HomeLayout()

        fun encode(layout: HomeLayout): String = json.encodeToString(layout)
    }
}
