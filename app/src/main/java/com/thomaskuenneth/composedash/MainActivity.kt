package com.thomaskuenneth.composedash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.thomaskuenneth.composedash.ui.theme.ComposeDashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

class MainActivity : ComponentActivity() {

    @ExperimentalStdlibApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val levelData = remember {
                createLevelDate()
            }
            ComposeDashTheme {
                Surface(color = Color.Black) {
                    ComposeDash(levelData)
                }
            }
        }
    }

    @ExperimentalStdlibApi
    @ExperimentalFoundationApi
    @Composable
    fun ComposeDash(levelData: SnapshotStateList<Char>) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            cells = GridCells.Fixed(COLUMNS)
        ) {
            itemsIndexed(levelData, itemContent = { index, item ->
                var background = Color.Transparent
                val symbol = when (item) {
                    '#' -> 0x1F9F1
                    'X' -> 0x1F48E
                    'O' -> 0x1FAA8
                    '.' -> {
                        background = Color(0xffc2b280)
                        0x2591
                    }
                    '@' -> 0x1F3C3
                    else -> 32
                }
                Text(
                    modifier = Modifier
                        .background(background)
                        .clickable {
                            movePlayerTo(levelData, index)
                        },
                    text = String(Character.toChars(symbol))
                )
            })
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

    private fun movePlayerTo(levelData: SnapshotStateList<Char>, desti: Int) {
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
            while (y != destiY) {
                current = walk(levelData, current, x, y)
                if (current == -1) break
                y += dirY
            }
            while (current != -1 && current != desti) {
                current = walk(levelData, current, x, y)
                if (current == -1) break
                x += dirX
            }
        }
    }

    private suspend fun walk(
        levelData: SnapshotStateList<Char>,
        current: Int,
        x: Int,
        y: Int
    ): Int {
        val newPos = (y * COLUMNS) + x
        when(levelData[newPos]) {
            'X' -> {
                // TODO: update gem count
            }
            'O', '#' -> {
                return -1
            }
        }
        levelData[current] = ' '
        levelData[newPos] = '@'
        delay(200)
        return newPos
    }
}