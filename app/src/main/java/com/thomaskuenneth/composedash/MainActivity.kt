package com.thomaskuenneth.composedash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.thomaskuenneth.composedash.ui.theme.ComposeDashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

const val COLUMNS = 40
const val ROWS = 14

val level = """
    ########################################
    #...............................X......#
    #.......OO.......OOOOOO................#
    #.......OO........OOOOOO...............#
    #.......XXXX.........X.................#
    #......................................#
    #.........................##############
    #.........OO...........................#
    #.........XXX..........................#
    ##################.....................#
    #......................XXXXXX..........#
    #.......OOOOOOO........................#
    #........X......................@......#
    ########################################
    """.trimIndent()

const val PLAYER = 0x1F3C3

const val GEM = 0x1F48E
const val CHAR_GEM = 'X'

const val BRICK = 0x1F9F1

const val ROCK = 0x1FAA8

const val SAND = 0x2591

const val NUMBER_OF_LIVES = 3

class MainActivity : ComponentActivity() {

    @ExperimentalStdlibApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDashTheme {
                Surface(color = Color.Black) {
                    ComposeDash()
                }
            }
        }
    }

    @ExperimentalStdlibApi
    @ExperimentalFoundationApi
    @Composable
    fun ComposeDash() {
        val key = remember { mutableStateOf(0L) }
        val levelData = remember(key.value) {
            createLevelDate()
        }
        val gemsTotal = remember(key.value) { Collections.frequency(levelData, CHAR_GEM) }
        val lives = remember(key.value) { mutableStateOf(NUMBER_OF_LIVES) }
        val gemsCollected = remember(key.value) { mutableStateOf(0) }
        Box {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                cells = GridCells.Fixed(COLUMNS)
            ) {
                itemsIndexed(levelData, itemContent = { index, item ->
                    var background = Color.Transparent
                    val symbol = when (item) {
                        '#' -> BRICK
                        CHAR_GEM -> GEM
                        'O' -> ROCK
                        '.' -> {
                            background = Color(0xffc2b280)
                            SAND
                        }
                        '@' -> PLAYER
                        else -> 32
                    }
                    Text(
                        modifier = Modifier
                            .background(background)
                            .clickable {
                                movePlayerTo(levelData, index, gemsCollected)
                            },
                        text = symbol.unicodeToString()
                    )
                })
            }
            Text(
                text = "${PLAYER.unicodeToString()}${lives.value} ${GEM.unicodeToString()}${gemsCollected.value}",
                color = Color.White,
                style = TextStyle(fontSize = 16.sp),
                modifier = Modifier.align(alignment = Alignment.BottomCenter)
            )
            if (gemsTotal == gemsCollected.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(0xa0000000))
                        .clickable {
                            key.value += 1
                        }
                ) {
                    Text(
                        "Well done!\nTap to restart",
                        style = TextStyle(fontSize = 48.sp, textAlign = TextAlign.Center),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }

    private fun createLevelDate(): SnapshotStateList<Char> {
        val data = mutableStateListOf<Char>()
        var rows = 0
        level.split("\n").forEach {
            if (it.length != COLUMNS) throw RuntimeException("length of row $rows is not $COLUMNS")
            data.addAll(it.toList())
            rows += 1
        }
        if (rows != ROWS) throw RuntimeException("number of rows is not $ROWS")
        return data
    }

    private fun movePlayerTo(
        levelData: SnapshotStateList<Char>,
        desti: Int,
        gemsCollected: MutableState<Int>
    ) {
        val start = levelData.indexOf('@')
        if (start == desti) return
        val startX = start % COLUMNS
        val startY = start / COLUMNS
        val destiX = desti % COLUMNS
        val destiY = desti / COLUMNS
        val dirX = if (destiX > startX) 1 else -1
        val dirY = if (destiY > startY) 1 else -1
        var current = start
        lifecycleScope.launch {
            var x = startX
            var y = startY
            while (current != -1 && y != destiY) {
                current = walk(levelData, current, x, y, gemsCollected)
                y += dirY
            }
            while (current != -1 && current != desti) {
                current = walk(levelData, current, x, y, gemsCollected)
                x += dirX
            }
        }
    }

    private suspend fun walk(
        levelData: SnapshotStateList<Char>,
        current: Int,
        x: Int,
        y: Int,
        gemsCollected: MutableState<Int>
    ): Int {
        val newPos = (y * COLUMNS) + x
        when (levelData[newPos]) {
            'X' -> {
                gemsCollected.value += 1
            }
            'O', '#' -> {
                return -1
            }
        }
        levelData[current] = ' '
        levelData[newPos] = '@'
        delay(200)
        if (current != -1) {
            freeFall(levelData, current - COLUMNS, 'O')
            freeFall(levelData, current - COLUMNS, 'X')
        }
        return newPos
    }

    private suspend fun freeFall(
        levelData: SnapshotStateList<Char>,
        current: Int,
        what: Char
    ) {
        if (levelData[current] == what) {
            lifecycleScope.launch {
                delay(200)
                freeFall(levelData, current - COLUMNS, what)
                val x = current % COLUMNS
                var y = current / COLUMNS + 1
                var pos = current
                while (y < ROWS) {
                    val newPos = y * COLUMNS + x
                    when (levelData[newPos]) {
                        '#', 'O', 'X' -> {
                            break
                        }
                    }
                    levelData[pos] = ' '
                    levelData[newPos] = what
                    y += 1
                    pos = newPos
                    delay(200)
                }
            }
        }
    }
}

fun Int.unicodeToString(): String = String(Character.toChars(this))
