package com.speedpubread.oceo.speedread

import android.text.Html
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class Reader(
        val story: ArrayList<String>,
        var WPM: Long = 0,
        var sentenceDelay: Long = 0,
        var currSentenceStart: Int = 0,
        var currSentenceIdx: Int = 0,
        var currentWordIdx: Int = 0,
        var maxWordIdx: Int = 0,
        var currentChapter: Int = 0) {

    private val TAG = "Reader"
    var disposableReader: Disposable? = null

    init {
    }

    // TODO move this into its own class
    fun disposeListener() {
//        Log.d("disposing Listener", "START")
//        Log.d("disposing Listener", "reader.currentWordIdx: " + currentWordIdx.toString())
//        Log.d("disposing Listener", "currentSentenceStart: " + reader.currSentenceStart.toString())
//        Log.d("disposing Listener", "cur: " + reader.currSentenceStart.toString())
        if (disposableReader != null && !disposableReader!!.isDisposed) {
            disposableReader!!.dispose()
        }
    }

    fun iterateWords(currentChunkView: TextView, currentWordView: TextView,
                     chptProgressView: TextView,
                     chapterSeekBar: SeekBar
    ) {
        val sentencesEndIdx = getNextSentencesStartIdx(story, 1, currentWordIdx)
        val displayStrs = buildBoldSentences(story, currSentenceStart, sentencesEndIdx)
        val tempWordIdx = currSentenceStart

        Log.d("OBSERVABLE", "--------------------OBS setup---------------------")
        Log.d("tempWordIdx START", tempWordIdx.toString())
        Log.d("sentencesEndIdx", sentencesEndIdx.toString())
        Log.d("WPM_MS", SpeedReadUtilities.WPMtoMS(WPM).toString())
        Log.d("WPM", WPM.toString())
        Log.d("sentenceDelay", sentenceDelay.toString())
        Log.d("OBSERVABLE", "--------------------OBS-setup---------------------\n\n")

        var rangeObs: Observable<*> = Observable.range(tempWordIdx, sentencesEndIdx - currentWordIdx)
                .concatMap { i: Any ->
                    Observable.just(i)
                            .delay(SpeedReadUtilities.WPMtoMS(WPM), TimeUnit.MILLISECONDS)
                }

        rangeObs = rangeObs.delay(sentenceDelay, TimeUnit.MILLISECONDS) // delay at the end of the sentence
        rangeObs = rangeObs.observeOn(AndroidSchedulers.mainThread())

        disposableReader = rangeObs.subscribe({ wordIdx: Any? ->
            Log.d("The OBS", wordIdx.toString() + " / " + sentencesEndIdx.toString());
            Log.d("currSentenceIdx < displayStrs.sizse", "${currSentenceIdx} / ${displayStrs.size}")
            if (currSentenceIdx < displayStrs.size) {
                Log.d("The OBS", "Is IN of Bounds")
                Log.d(TAG, currentWordIdx.toString() + " / " + displayStrs.size.toString())
                currentChunkView.text = Html.fromHtml(displayStrs[currSentenceIdx].toString())
                currentWordView.text = story[currentWordIdx]
                currSentenceIdx++
                currentWordIdx++
                setSeekBarData(chptProgressView, chapterSeekBar)
            } else {
                Log.d("The OBS", "Is Out of Bounds")
                Log.d(TAG, currentWordIdx.toString() + " / " + displayStrs.size.toString())
            }
        },
                { e: Any? -> }
        ) {
            // move to next sentence
            if (currentWordIdx < maxWordIdx) {
                currSentenceIdx = 0
                currSentenceStart = currentWordIdx
                iterateWords(currentChunkView, currentWordView, chptProgressView, chapterSeekBar)
            }
        }
    }

    fun setSeekBarData(chptProgressView: TextView, chapterSeekBar: SeekBar) {
        val chapterCompleted = getChapterPercentageComplete()
        if (chapterCompleted.toString().length > 3) {
            chptProgressView.text = chapterCompleted.toString().substring(0, 4) + "%"
        } else {
            chptProgressView.text = "$chapterCompleted%"
        }
        chapterSeekBar.progress = currentWordIdx
    }

    fun getSentenceStartIdx(idx: Int): Int {
        var idx = idx
        while (!story!![idx].contains(".") && idx > 0) {
            idx -= 1
        }
        return idx + 1
    }

    fun getChapterPercentageComplete(): Float {
        return currentWordIdx.toFloat() / maxWordIdx.toFloat() * 100
    }

    fun getNextSentencesStartIdx(tokens: java.util.ArrayList<String>?, numSentences: Int, startIdx: Int): Int {
        var start = startIdx
        var foundSentences = 0
        while (foundSentences < numSentences) {
            while (start < maxWordIdx && (!tokens!![start].contains(".")
                            || tokens[start].contains("?")
                            || tokens[start].contains("!"))) {
                start++
            }
            start += 1
            foundSentences += 1
        }
        if (tokens != null && startIdx < tokens.size && tokens[startIdx].contains("”")) {
            start += 1
        }
        return start
    }

    fun buildBoldSentences(tokenList: java.util.ArrayList<String>?, startIdx: Int, endIdx: Int): java.util.ArrayList<StringBuilder> {
        var end = endIdx
        if (end > maxWordIdx) {
            end = maxWordIdx
        }
        val displayStrs = java.util.ArrayList<StringBuilder>()
        for (targetWord in startIdx until end) {
            val formattedDisplayStr = StringBuilder()
            for (i in startIdx until end) {
                if (targetWord == i) {
                    formattedDisplayStr.append("<font color=\"gray\">" + tokenList!![i] + "</font> ")
                } else {
                    formattedDisplayStr.append(tokenList!![i] + " ")
                }
            }
            displayStrs.add(formattedDisplayStr)
        }
        return displayStrs
    }
}