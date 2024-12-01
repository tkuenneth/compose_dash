package com.thomaskuenneth.composedash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

const val COLUMNS = 40
const val ROWS = 14

val level = """
    ########################################
    #...............................X......#
    #.......OO.......OOOOOO................#
    #.......OO........OOOOOO...............#
    #.......XXXX!.......!X.................#
    #......................................#
    #.........................##############
    #.........OO...........................#
    #.........XXX..........................#
    ##################.....................#
    #......................XXXXXX..........#
    #       OOOOOOO........................#
    #      !.X......................@......#
    ########################################
    """.trimIndent()

const val POWER = 0x21BB

const val PLAYER = 0x1F3C3
const val CHAR_PLAYER = '@'

const val GEM = 0x1F48E
const val CHAR_GEM = 'X'

const val BRICK = 0x1F9F1
const val CHAR_BRICK = '#'

const val ROCK = 0x1FAA8
const val CHAR_ROCK = 'O'

const val SAND = 0x2591
const val CHAR_SAND = '.'

const val SPIDER = 0x1F577
const val CHAR_SPIDER = '!'

const val BLANK = 0x20
const val CHAR_BLANK = ' '

const val NUMBER_OF_LIVES = 3

lateinit var enemies: SnapshotStateList<Enemy>
lateinit var levelData: SnapshotStateList<Char>
lateinit var lives: MutableState<Int>
lateinit var key: MutableState<Long>

data class Enemy(var index: Int) {
    var dirX = 0
    var dirY = 0
}

var playerIsMoving = false

suspend fun moveEnemies() {
    delay(200)
    var playerHit = false
    if (::enemies.isInitialized) {
        val indexPlayer = levelData.indexOf(CHAR_PLAYER)
        val colPlayer = indexPlayer % COLUMNS
        val rowPlayer = indexPlayer / COLUMNS
        enemies.forEach {
            delay(400)
            if (!playerHit) {
                val current = it.index
                val row = current / COLUMNS
                val col = current % COLUMNS
                var newPos = current
                if (col != colPlayer) {
                    if (it.dirX == 0)
                        it.dirX = if (col >= colPlayer) -1 else 1
                    newPos += it.dirX
                    val newCol = newPos % COLUMNS
                    if (newCol < 0 || levelData[newPos] != CHAR_BLANK) {
                        if (isPlayer(levelData, newPos)) {
                            playerHit = true
                        }
                        newPos = current
                        it.dirX = -it.dirX
                    }
                }
                if (row != rowPlayer) {
                    val temp = newPos
                    if (it.dirY == 0)
                        it.dirY = if (row >= rowPlayer) -COLUMNS else COLUMNS
                    newPos += it.dirY
                    val newRow = newPos / COLUMNS
                    if (newRow < 0 || newRow >= ROWS || levelData[newPos] != CHAR_BLANK) {
                        if (isPlayer(levelData, newPos)) {
                            playerHit = true
                        }
                        newPos = temp
                        it.dirY = 0
                    }
                }
                if (newPos != it.index) {
                    levelData[newPos] = CHAR_SPIDER
                    levelData[it.index] = CHAR_BLANK
                    it.index = newPos
                }
            }
        }
    }
    if (playerHit) {
        lives.value -= 1
        key.value += 1
    }
}

fun isPlayer(levelData: SnapshotStateList<Char>, index: Int) = levelData[index] == CHAR_PLAYER

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDashTheme {
                Surface(color = Color.Black) {
                    ComposeDash()
                }
            }
        }
        lifecycleScope.launch {
            while (isActive) moveEnemies()
        }
    }

    @Composable
    fun ComposeDash() {
        key = remember { mutableLongStateOf(0L) }
        levelData = remember(key.value) {
            createLevelData()
        }
        enemies = remember(key.value) { createEnemies(levelData) }
        val gemsTotal = remember(key.value) { Collections.frequency(levelData, CHAR_GEM) }
        val gemsCollected = remember(key.value) { mutableIntStateOf(0) }
        // Must be reset explicitly
        val lastLives = remember { mutableIntStateOf(NUMBER_OF_LIVES) }
        lives = remember { mutableIntStateOf(NUMBER_OF_LIVES) }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS)
                ) {
                    itemsIndexed(levelData, itemContent = { index, item ->
                        var background = Color.Transparent
                        val symbol = when (item) {
                            CHAR_BRICK -> BRICK
                            CHAR_GEM -> GEM
                            CHAR_ROCK -> ROCK
                            CHAR_SAND -> {
                                background = Color(0xffc2b280)
                                SAND
                            }

                            CHAR_PLAYER -> PLAYER
                            CHAR_SPIDER -> SPIDER
                            else -> BLANK
                        }
                        Text(
                            modifier = Modifier
                                .background(background)
                                .clickable {
                                    movePlayerTo(levelData, index, gemsCollected, lives)
                                }, text = symbol.unicodeToString()
                        )
                    })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RestartButton(key, lives, lastLives)
                    Text(
                        text = "${PLAYER.unicodeToString()}${lives.value} ${GEM.unicodeToString()}${gemsCollected.intValue}",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 16.sp),
                        modifier = Modifier.weight(1.0F)
                    )
                }
            }
            if (gemsTotal == gemsCollected.intValue) LevelCompleted(key)
            if (lives.value != lastLives.intValue) {
                NextTry(key, lives, lastLives)
            }
        }
    }

    @Composable
    fun LevelCompleted(key: MutableState<Long>) {
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

    @Composable
    fun NextTry(
        key: MutableState<Long>,
        lives: MutableState<Int>,
        lastLives: MutableState<Int>
    ) {
        val canTryAgain = lives.value > 0
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xa0000000))
                .clickable {
                    if (canTryAgain) lastLives.value = lives.value
                    else {
                        lives.value = NUMBER_OF_LIVES
                        lastLives.value = NUMBER_OF_LIVES
                    }
                    key.value += 1
                }
        ) {
            Text(
                "I am sorry!\n${if (canTryAgain) "Try again" else "You lost"}",
                style = TextStyle(fontSize = 48.sp, textAlign = TextAlign.Center),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    fun RestartButton(
        key: MutableState<Long>, lives: MutableState<Int>, lastLives: MutableState<Int>
    ) {
        Text(POWER.unicodeToString(),
            style = TextStyle(fontSize = 32.sp),
            color = Color.White,
            modifier = Modifier.clickable {
                lives.value = NUMBER_OF_LIVES
                lastLives.value = NUMBER_OF_LIVES
                key.value += 1
            })
    }

    private fun createLevelData(): SnapshotStateList<Char> {
        val data = mutableStateListOf<Char>()
        var rows = 0
        level.split("\n").forEach {
            if (it.length != COLUMNS)
                throw RuntimeException("length of row $rows is not $COLUMNS")
            data.addAll(it.toList())
            rows += 1
        }
        if (rows != ROWS)
            throw RuntimeException("number of rows is not $ROWS")
        return data
    }

    private fun createEnemies(levelDate: SnapshotStateList<Char>): SnapshotStateList<Enemy> {
        val enemies = mutableStateListOf<Enemy>()
        levelDate.forEachIndexed { index, char ->
            if (char == CHAR_SPIDER) {
                enemies.add(Enemy(index))
            }
        }
        return enemies
    }

    private fun movePlayerTo(
        levelData: SnapshotStateList<Char>,
        desti: Int,
        gemsCollected: MutableState<Int>,
        lives: MutableState<Int>
    ) {
        if (playerIsMoving) return
        val start = levelData.indexOf(CHAR_PLAYER)
        if (start == desti) return
        val startX = start % COLUMNS
        val startY = start / COLUMNS
        val destiX = desti % COLUMNS
        val destiY = desti / COLUMNS
        val dirX = if (destiX > startX) 1 else -1
        val dirY = if (destiY > startY) 1 else -1
        var current = start
        lifecycleScope.launch {
            playerIsMoving = true
            var x = startX
            var y = startY
            while (current != -1 && y != destiY) {
                current = walk(levelData, current, x, y, gemsCollected, lives)
                y += dirY
            }
            while (current != -1 && current != desti) {
                current = walk(levelData, current, x, y, gemsCollected, lives)
                x += dirX
            }
            playerIsMoving = false
        }
    }

    private suspend fun walk(
        levelData: SnapshotStateList<Char>,
        current: Int,
        x: Int,
        y: Int,
        gemsCollected: MutableState<Int>,
        lives: MutableState<Int>
    ): Int {
        val newPos = (y * COLUMNS) + x
        var result = newPos
        when (levelData[newPos]) {
            CHAR_GEM -> {
                gemsCollected.value += 1
            }
            CHAR_ROCK, CHAR_BRICK -> {
                result = -1
            }
        }
        if (result != -1) {
            levelData[current] = CHAR_BLANK
            levelData[newPos] = CHAR_PLAYER
        }
        delay(200)
        freeFall(levelData, newPos - COLUMNS, CHAR_ROCK, lives)
        freeFall(levelData, newPos - COLUMNS, CHAR_GEM, lives)
        return result
    }

    private fun freeFall(
        levelData: SnapshotStateList<Char>,
        current: Int,
        what: Char,
        lives: MutableState<Int>
    ) {
        lifecycleScope.launch {
            delay(800)
            if (levelData[current] == what) {
                freeFall(levelData, current - COLUMNS, CHAR_ROCK, lives)
                freeFall(levelData, current - COLUMNS, CHAR_GEM, lives)
                val x = current % COLUMNS
                var y = current / COLUMNS + 1
                var pos = current
                var playerHit = false
                while (y < ROWS) {
                    val newPos = y * COLUMNS + x
                    when (levelData[newPos]) {
                        CHAR_BRICK, CHAR_ROCK, CHAR_GEM -> {
                            break
                        }
                        CHAR_PLAYER -> {
                            if (!playerHit) {
                                playerHit = true
                                lives.value -= 1
                            }
                        }
                    }
                    levelData[pos] = CHAR_BLANK
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
