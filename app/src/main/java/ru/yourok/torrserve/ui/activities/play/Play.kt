package ru.yourok.torrserve.ui.activities.play

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.yourok.torrserve.R
import ru.yourok.torrserve.server.api.Api
import ru.yourok.torrserve.server.api.Viewed
import ru.yourok.torrserve.server.models.torrent.Torrent
import ru.yourok.torrserve.ui.fragments.play.InfoFragment
import ru.yourok.torrserve.ui.fragments.play.TorrentFilesFragment
import ru.yourok.torrserve.utils.TorrentHelper

object Play {

    fun PlayActivity.play(save: Boolean) {
        lifecycleScope.launch {
            torrentSave = save
            withContext(Dispatchers.IO) {
                val result = load(this@play)
                result?.first?.let { torr ->
                    //TODO logic for one or more pl files
                    TorrentFilesFragment().apply {
                        showTorrent(this@play, torr, result.second)
                    }
                }
            }
        }
    }

    suspend fun load(activity: PlayActivity): Pair<Torrent, List<Viewed>?>? {
        var torr: Torrent? = null
        var list: List<Viewed>? = null
        activity.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                // show info
                val info = InfoFragment()
                info.show(this@apply, R.id.top_container)
                info.showProgress()
                // get torrent
                torr = loadTorrent(torrentLink, torrentHash, torrentTitle, torrentPoster, torrentSave)
                torr?.let {
                    torrentHash = it.hash
                    // start update info
                    info.startInfo(it.hash)
                    //get viewed
                    list = Api.listViewed(it.hash)
                    // wait torr info and change fragment
                    lifecycleScope.launch(Dispatchers.IO) {
                        TorrentHelper.waitFiles(torrentHash)
                    }.join()
                    info.hideProgress()
                }
            }.join()
        }

        torr?.let {
            return it to list
        }
        return null
    }

    private fun loadTorrent(link: String, hash: String, title: String, poster: String, save: Boolean): Torrent? {
        val magnet =
            if (hash.isNotEmpty())
                hash
            else if (link.isNotEmpty())
                link
            else
                return null

        val torr = Api.addTorrent(magnet, title, poster, save)
        return torr
    }
}