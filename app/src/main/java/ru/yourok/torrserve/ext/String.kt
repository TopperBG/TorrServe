package ru.yourok.torrserve.ext

fun String.removeNonPrintable(): String // All Control Char
{
    return this.replace("[\\p{C}]".toRegex(), "")
}

fun String.removeSomeControlChar(): String // Some Control Char
{
    return this.replace("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]".toRegex(), "")
}

fun String.removeFullControlChar(): String {
    return removeNonPrintable().replace("[\\r\\n\\t]".toRegex(), "")
}

fun String.removeOtherSymbolChar(): String // Some Control Char
{
    return this.replace("[\\p{So}]".toRegex(), "")
}

fun String.clearName(): String {
    return this.removeOtherSymbolChar()
        .replace(",", " ")
        .replace(".", " ")
        .replace("_", " ")
        .replace("\\s+".toRegex(), " ") // clear &nbsp and wide spaces
        .trim()
}

fun String.clearPath(): String {
    return this.removeOtherSymbolChar()
        .replace(",", " ")
        .replace(".", " ")
        .replace("\\s+".toRegex(), Typography.nbsp.toString()) // use &nbsp (don't break)
        .trim()
}

// https://github.com/YouROK/NUMParser/blob/be9eb56f1b4b53ff251d84f75186f162019ddac4/db/models/torrentDetails.go#L9
fun String.normalize(): String {
    return when {
        this.contains("movie", true) -> "movie"
        this.contains("series", true) -> "tv"
        this.equals("tvshow", true) -> "tv"
        else -> this.lowercase()
    }
}