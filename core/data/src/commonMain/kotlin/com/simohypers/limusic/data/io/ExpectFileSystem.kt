package com.simohypers.limusic.data.io

import okio.FileSystem

expect fun fileSystem(): FileSystem

expect fun fileDir(): String